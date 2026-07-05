package com.nordstrom.automation.selenium.core;

/**
 * Manager for the sidecar servlet container.
 * <p>
 * This is a stub implementation. Full implementation is provided in Phase 2.
 *
 * @since [next-major]
 */
public class SidecarManager {

    private SidecarManager() {
        throw new AssertionError("SidecarManager is a static utility class that cannot be instantiated");
    }

    /**
     * Ensure the sidecar servlet container is running.
     * <p>
     * <b>NOTE</b>: This is a stub implementation. Full implementation is provided in Phase 2.
     */
    public static void ensureRunning() {
        // TODO: Phase 2 - launch sidecar servlet container if not already running
    }

    /**
     * Await termination of the sidecar servlet container.
     * <p>
     * <b>NOTE</b>: This is a stub implementation. Full implementation is provided in Phase 2.
     *
     * @throws InterruptedException if this thread was interrupted
     */
    public static void awaitTermination() throws InterruptedException {
        // TODO: Phase 2 - await sidecar servlet container termination
    }
}
