package com.nordstrom.automation.selenium.sidecar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.common.file.PathUtils;

/**
 * Shutdown strategy for servers registered with a PID in Java 8 environments.
 * <p>
 * Since {@link ProcessHandle} requires Java 9 or later, this implementation falls back
 * to command-line process termination via {@code kill} (Unix) or {@code taskkill} (Windows).
 * <p>
 * This approach works on standard platforms but is not supported in Termux/PRoot environments,
 * where process termination via command line is unavailable. A warning is logged when
 * termination fails or is unsupported.
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
     * Shut down the server identified by the specified registration by terminating its process
     * via the command line.
     * <p>
     * <b>NOTE</b>: This implementation uses {@code kill} (Unix) or {@code taskkill} (Windows)
     * since {@link ProcessHandle} is not available in Java 8. Process termination is not
     * supported in Termux/PRoot environments — a warning is logged if termination fails.
     *
     * @param registration {@link GridServerRegistration} of the server to shut down
     */
    public void shutdown(GridServerRegistration registration) {
        long pid = registration.getPid();
        try {
            List<String> args = new ArrayList<>();
            if (SystemUtils.IS_OS_WINDOWS) {
                args.add("taskkill");
                args.add("/F");
                args.add("/PID");
                args.add(String.valueOf(pid));
            } else {
                args.add("kill");
                args.add(String.valueOf(pid));
            }
            ProcessBuilder pb = new ProcessBuilder(args);
            pb.environment().put("PATH", PathUtils.getSystemPath());
            int exitCode = pb.start().waitFor();
            if (exitCode == 0) {
                LOGGER.debug("Terminated process {} for server at {}",
                        pid, registration.getServerUrl());
            } else {
                LOGGER.warn("kill exited with code {} for process {} — " +
                        "process termination may not be supported in this environment",
                        exitCode, pid);
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            LOGGER.warn("Failed terminating process {} — " +
                    "process termination may not be supported in this environment", pid, e);
        }
    }
}
