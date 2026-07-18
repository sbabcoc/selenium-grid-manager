package com.nordstrom.automation.selenium.sidecar;

/**
 * Enumeration of shutdown mechanisms used by the sidecar to terminate registered grid servers.
 * <p>
 * The shutdown mode for each server is determined at registration time and stored in
 * {@link GridServerRegistration}. The sidecar selects the appropriate shutdown strategy
 * based on this value — no user configuration of shutdown behavior is provided or needed.
 *
 * @since 36.0.0
 */
public enum ShutdownMode {

    /**
     * Selenium 4 non-hub servers — standard nodes and relay nodes.
     * The server process is terminated via {@code ProcessHandle#destroy()}.
     */
    PID,

    /**
     * PM2-managed Appium servers (both Selenium 3 and 4).
     * PM2 is unconditionally required for all Appium.
     * The process is terminated via {@code pm2 delete appium-<port>}.
     */
    PM2,

    /**
     * Selenium 3 hub and node servers.
     * The server is shut down via an HTTP POST to its {@code LifecycleServlet} endpoint.
     */
    LIFECYCLE
}
