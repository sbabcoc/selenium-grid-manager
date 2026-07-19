package com.nordstrom.automation.selenium.core;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.registration.RegistrationStrategy;
import com.nordstrom.automation.selenium.sidecar.SidecarClient;
import com.nordstrom.common.base.UncheckedThrow;
import com.nordstrom.common.uri.UriUtils;

/**
 * This class represents a single Selenium server (hub or node) belonging
 * to a local Grid collection.
 *
 * @since 36.0.0
 */
public class LocalGridServer extends GridServer {

    private final ProcessBuilder builder;
    private final RegistrationStrategy registrationStrategy;
    private Process process = null;
    private boolean hasStarted = false;
    private boolean isActive = false;
    private final int hubPort;
    private final Map<String, String> personalities = new HashMap<>();

    /**
     * Constructor for local Grid server object.
     *
     * @param host IP address of local Grid server
     * @param port port of local Grid server
     * @param isHub role of Grid server being started ({@code true} = hub; {@code false} = node)
     * @param hubPort port of the hub for the Grid collection this server belongs to
     * @param builder {@link ProcessBuilder} for local Grid server process
     * @param workingPath {@link Path} of working directory for server process; {@code null} for default
     * @param outputPath {@link Path} to output log file; {@code null} to decline log-to-file
     * @param registrationStrategy {@link RegistrationStrategy} for registering this server with the sidecar
     */
    public LocalGridServer(String host, Integer port, boolean isHub, int hubPort,
            ProcessBuilder builder, Path workingPath, Path outputPath,
            RegistrationStrategy registrationStrategy) {
        super(getServerUrl(host, port), isHub);
        this.hubPort = hubPort;
        this.registrationStrategy = registrationStrategy;

        if (workingPath != null) {
            builder.directory(workingPath.toFile());
        }

        if (outputPath != null) {
            builder.redirectOutput(outputPath.toFile());
            builder.redirectErrorStream(true);
        }

        this.builder = builder;
    }

    /**
     * Get the hub port for the Grid collection this server belongs to.
     *
     * @return hub port
     */
    public int getHubPort() {
        return hubPort;
    }

    /**
     * Get process environment of this local Grid server.
     *
     * @return map of process environment variables
     */
    public Map<String, String> getEnvironment() {
        return builder.environment();
    }

    /**
     * Get the driver 'personalities' for this local Grid server.
     *
     * @return map: "personality" &rarr; desired capabilities (JSON)
     */
    @Override
    public Map<String, String> getPersonalities() {
        return personalities;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void start() throws IOException, InterruptedException, TimeoutException {
        if (!hasStarted && builder != null) {
            process = builder.start();
            hasStarted = true;
            registrationStrategy.register(this, process);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        if (!isActive) {
            isActive = super.isActive();
        }
        return isActive;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean shutdown() throws InterruptedException {
        if (!isActive()) return true;
        SidecarClient.shutdown(hubPort);
        hasStarted = false;
        isActive = false;
        return true;
    }

    /**
     * Get {@code localhost} URL for Selenium Grid server at the specified port.
     * <p>
     * <b>NOTE</b>: The assembled URL includes the Grid web service base path.
     *
     * @param host IP address of local Grid server
     * @param port port of local Grid server
     * @return {@link URL} for local Grid server at the specified port
     */
    public static URL getServerUrl(String host, Integer port) {
        try {
            String[] pathAndParams = SeleniumConfig.getConfig().isW3C() ?
                    new String[] {} : new String[] {"/wd/hub"};
            return UriUtils.makeBasicURI("http", host, port, pathAndParams).toURL();
        } catch (MalformedURLException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }
}
