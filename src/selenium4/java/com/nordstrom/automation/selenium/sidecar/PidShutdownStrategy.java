package com.nordstrom.automation.selenium.sidecar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shutdown strategy for servers registered with a PID in Java 9+ environments.
 * <p>
 * Uses {@link ProcessHandle#destroy()} to terminate the server process. This implementation
 * is capable of managing both Selenium 3 and Selenium 4 servers registered with a PID,
 * since {@link ProcessHandle} is available in Java 9 and later.
 * <p>
 * This is an internal implementation detail of the sidecar — it is not a user extension point.
 * The appropriate strategy is selected automatically based on the {@link ShutdownMode} recorded
 * in the {@link GridServerRegistration} at launch time.
 *
 * @since [next-major]
 */
class PidShutdownStrategy implements ShutdownStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(PidShutdownStrategy.class);

    /**
     * Shut down the server identified by the specified registration by destroying its process.
     *
     * @param registration {@link GridServerRegistration} of the server to shut down
     */
    public void shutdown(GridServerRegistration registration) {
        long pid = registration.getPid();
        ProcessHandle.of(pid).ifPresentOrElse(
                handle -> {
                    handle.destroy();
                    LOGGER.debug("Destroyed process {} for server at {}",
                            pid, registration.getServerUrl());
                },
                () -> LOGGER.debug("Process {} not found — already stopped", pid));
    }
}
