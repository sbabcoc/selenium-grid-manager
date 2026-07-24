package com.nordstrom.automation.selenium.sidecar;

import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.sidecar.servlet.SidecarPathName;

/**
 * Entry point for launching the sidecar as a standalone dashboard.
 * <p>
 * Launched via the {@code runSidecar} Gradle task:
 * <pre>
 *   ./gradlew runSidecar
 * </pre>
 * The console is available at {@code http://localhost:9001}{@value SidecarPathName#CONSOLE_PATH}
 * (or the configured {@link SeleniumSettings#SIDECAR_PORT}).
 *
 * @since 36.0.0
 */
public class SidecarDashboard {

    private static final Logger LOGGER = LoggerFactory.getLogger(SidecarDashboard.class);

    private SidecarDashboard() {
        throw new AssertionError("SidecarDashboard is a static utility class that cannot be instantiated");
    }

    /**
     * Launch the sidecar as a standalone dashboard.
     *
     * @param args command line arguments (unused)
     */
    public static void main(String[] args) {
        LOGGER.info("Starting Selenium Grid Manager sidecar dashboard");
        try {
            URL sidecarUrl = SidecarManager.ensureRunning();
            LOGGER.info("Sidecar dashboard running — console at {}{}", sidecarUrl, SidecarPathName.CONSOLE_PATH);
            SidecarManager.awaitTermination();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.info("Sidecar dashboard interrupted — shutting down");
        } catch (Exception e) {
            LOGGER.error("Sidecar dashboard failed to start: {}", e.getMessage(), e);
            System.exit(1);
        }
    }
}
