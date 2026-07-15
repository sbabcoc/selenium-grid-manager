package com.nordstrom.automation.selenium.sidecar;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.ServiceLoader;

import org.apache.http.util.EntityUtils;
import org.openqa.selenium.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.GridUtility;
import com.nordstrom.automation.selenium.utility.HostUtils;
import com.nordstrom.common.base.UncheckedThrow;
import com.nordstrom.common.uri.UriUtils;

/**
 * Manager for the sidecar servlet container.
 * <p>
 * The sidecar runs either embedded within the grid launcher JVM (started automatically
 * when a local grid collection is created) or as a standalone dashboard (started via
 * the {@code runSidecar} Gradle task). In both cases the same servlet infrastructure
 * is used.
 * <p>
 * Multiple JVMs safely share a single sidecar — if the sidecar is already active when
 * {@link #ensureRunning()} is called, it returns without starting a new instance.
 *
 * @since [next-major]
 */
public class SidecarManager {

    private static final Logger LOGGER = LoggerFactory.getLogger(SidecarManager.class);

    private static final List<String> SIDECAR_SERVLETS = Arrays.asList(
            "com.nordstrom.automation.selenium.sidecar.servlet.RegistrationServlet",
            "com.nordstrom.automation.selenium.sidecar.servlet.DeregistrationServlet",
            "com.nordstrom.automation.selenium.sidecar.servlet.ShutdownServlet",
            "com.nordstrom.automation.selenium.sidecar.servlet.StopServlet",
            "com.nordstrom.automation.selenium.sidecar.servlet.StatusServlet",
            "com.nordstrom.automation.selenium.sidecar.servlet.ConsoleServlet");

    private SidecarManager() {
        throw new AssertionError("SidecarManager is a static utility class that cannot be instantiated");
    }

    /**
     * Ensure the sidecar servlet container is running.
     * <p>
     * If the sidecar is already active (either in this JVM or another), this method
     * returns without starting a new instance. Otherwise, starts the sidecar on the
     * port specified by {@link SeleniumSettings#SIDECAR_PORT}.
     */
    public static synchronized void ensureRunning() {
        if (isActive()) {
            LOGGER.debug("Sidecar already active — skipping launch");
            try {
                int port = SeleniumConfig.getConfig().getInt(SeleniumSettings.SIDECAR_PORT.key());
                URL sidecarUrl = UriUtils.makeBasicURI("http", HostUtils.getLocalHost(), port).toURL();
                String json = EntityUtils.toString(
                        GridUtility.getHttpResponse(sidecarUrl, "/grid/control/status").getEntity(),
                        StandardCharsets.UTF_8);
                Map<String, Object> status = SeleniumConfig.getConfig().fromJson(json, Json.MAP_TYPE);
                boolean exampleSiteActive = status.get("exampleSiteUrl") != null;
                boolean serveExampleSite = SeleniumConfig.getConfig()
                        .getBoolean(SeleniumSettings.SERVE_EXAMPLE_SITE.key());
                if (exampleSiteActive != serveExampleSite) {
                    LOGGER.warn("SERVE_EXAMPLE_SITE mismatch — setting is {} but active sidecar {} " +
                            "the example site; restart the sidecar to apply this setting",
                            serveExampleSite, exampleSiteActive ? "is serving" : "is not serving");
                }
            } catch (Exception e) {
                LOGGER.debug("Unable to verify example site status from active sidecar", e);
            }
            return;
        }

        int port = SeleniumConfig.getConfig().getInt(SeleniumSettings.SIDECAR_PORT.key());
        LOGGER.debug("Starting sidecar servlet container on port {}", port);

        List<String> servlets = new ArrayList<>(SIDECAR_SERVLETS);
        if (SeleniumConfig.getConfig().getBoolean(SeleniumSettings.SERVE_EXAMPLE_SITE.key())) {
            servlets.add("com.nordstrom.automation.selenium.servlet.ExamplePageServlet");
            servlets.add("com.nordstrom.automation.selenium.servlet.ExamplePageServlet$FrameA_Servlet");
            servlets.add("com.nordstrom.automation.selenium.servlet.ExamplePageServlet$FrameB_Servlet");
            servlets.add("com.nordstrom.automation.selenium.servlet.ExamplePageServlet$FrameC_Servlet");
            servlets.add("com.nordstrom.automation.selenium.servlet.ExamplePageServlet$FrameD_Servlet");
            LOGGER.debug("Example page site added to sidecar at /grid/admin/ExamplePageServlet");
        }

        SidecarSupport support = getSupport();
        try {
            support.start(servlets, port);
            Runtime.getRuntime().addShutdownHook(new Thread(() -> {
                LOGGER.info("JVM shutting down — shutting down all managed grid collections");
                GridRegistry.getInstance().shutdownAll();
            }, "sidecar-shutdown-hook"));
            LOGGER.info("Sidecar started on port {} — console at " +
                    "http://{}:{}/grid/control/console", port, HostUtils.getLocalHost(), port);
        } catch (IOException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }

    /**
     * Await termination of the sidecar servlet container.
     * <p>
     * Blocks the calling thread until the sidecar is stopped via
     * {@code /grid/control/stop} or the JVM exits.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    public static void awaitTermination() throws InterruptedException {
        getSupport().await();
    }

    /**
     * Determine if the sidecar is currently active.
     * <p>
     * Checks both the in-JVM {@link SidecarSupport} state and the HTTP health
     * endpoint, so this correctly detects sidecars started by other JVMs.
     *
     * @return {@code true} if the sidecar is active; otherwise {@code false}
     */
    public static boolean isActive() {
        // check in-JVM state first
        if (getSupport().isActive()) return true;
        // check HTTP health endpoint for cross-JVM detection
        try {
            int port = SeleniumConfig.getConfig().getInt(SeleniumSettings.SIDECAR_PORT.key());
            return GridUtility.isHostActive(
                    UriUtils.makeBasicURI("http", HostUtils.getLocalHost(), port,
                            "/grid/control/status").toURL());
        } catch (MalformedURLException e) {
            return false;
        }
    }

    /**
     * Get the {@link SidecarSupport} implementation via {@link ServiceLoader}.
     *
     * @return {@link SidecarSupport} implementation
     * @throws IllegalStateException if no implementation is found
     */
    private static SidecarSupport getSupport() {
        for (SidecarSupport support : ServiceLoader.load(SidecarSupport.class)) {
            return support;
        }
        throw new IllegalStateException(
                "No SidecarSupport implementation found — ensure selenium-grid-manager is on the classpath");
    }
}
