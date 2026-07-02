package com.nordstrom.automation.selenium.core;

/**
 * Client for communicating with the sidecar servlet container.
 * <p>
 * This is a stub implementation. Full implementation is provided in Phase 2.
 *
 * @since [next-major]
 */
public class SidecarClient {

    private SidecarClient() {
        throw new AssertionError("SidecarClient is a static utility class that cannot be instantiated");
    }

    /**
     * Register a grid server with the sidecar registry.
     * <p>
     * <b>NOTE</b>: This is a stub implementation. Full implementation is provided in Phase 2.
     *
     * @param registration the registration object describing the server
     */
    public static void register(Object registration) {
        // TODO: Phase 2 - post registration to sidecar
    }

    /**
     * Request the sidecar to shut down the grid collection on the specified hub port.
     * <p>
     * <b>NOTE</b>: This is a stub implementation. Full implementation is provided in Phase 2.
     *
     * @param hubPort port of the hub whose collection should be shut down
     */
    public static void shutdown(int hubPort) {
        // TODO: Phase 2 - post shutdown request to sidecar
    }
}
