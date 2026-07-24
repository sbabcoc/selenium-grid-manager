package com.nordstrom.automation.selenium.sidecar.servlet;

import org.slf4j.Logger;

/**
 * Shared helper for terminating the sidecar JVM.
 *
 * @since 36.0.0
 */
final class SidecarLifecycle {

    private SidecarLifecycle() {
        throw new AssertionError("SidecarLifecycle is a static utility class that cannot be instantiated");
    }

    /**
     * Stop the sidecar JVM after a brief delay, allowing the triggering
     * HTTP response to flush first.
     *
     * @param logger logger to record the stop event
     * @param reason brief description of why the sidecar is stopping
     */
    static void stopAfterDelay(Logger logger, String reason) {
        logger.info("Sidecar stopping — {}", reason);
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            System.exit(0);
        }, "sidecar-stop").start();
    }
}
