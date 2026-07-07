package com.nordstrom.automation.selenium.sidecar;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.utility.NodeBinaryFinder;
import com.nordstrom.common.file.PathUtils;

/**
 * Shutdown strategy for PM2-managed Appium servers (both Selenium 3 and 4).
 * Terminates the Appium process via {@code pm2 delete appium-<port>}.
 * <p>
 * This is an internal implementation detail of the sidecar — it is not a user extension point.
 * The appropriate strategy is selected automatically based on the {@link ShutdownMode} recorded
 * in the {@link GridServerRegistration} at launch time.
 *
 * @since [next-major]
 */
class PM2ShutdownStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(PM2ShutdownStrategy.class);

    /**
     * Shut down the Appium server identified by the specified registration by deleting
     * its PM2 process.
     *
     * @param registration {@link GridServerRegistration} of the server to shut down
     * @throws InterruptedException if interrupted while waiting for PM2 to complete
     */
    void shutdown(GridServerRegistration registration) throws InterruptedException {
        int port = registration.getServerUrl().getPort();
        String pm2Name = "appium-" + port;

        if (!pm2ProcessExists(pm2Name)) {
            LOGGER.debug("PM2 process '{}' not found — already stopped", pm2Name);
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder(buildArgs("delete", pm2Name));
            pb.environment().put("PATH", PathUtils.getSystemPath());
            int exitCode = pb.start().waitFor();
            if (exitCode == 0) {
                LOGGER.debug("Deleted PM2 process: {}", pm2Name);
            } else {
                LOGGER.warn("PM2 delete exited with code {} for process: {}", exitCode, pm2Name);
            }
        } catch (IOException e) {
            LOGGER.warn("I/O exception deleting PM2 process: {}", pm2Name, e);
        }
    }

    /**
     * Determine if the specified PM2 process exists.
     *
     * @param pm2Name PM2 process name
     * @return {@code true} if the process exists; otherwise {@code false}
     */
    private boolean pm2ProcessExists(String pm2Name) {
        try {
            ProcessBuilder pb = new ProcessBuilder(buildArgs("describe", pm2Name));
            pb.environment().put("PATH", PathUtils.getSystemPath());
            return pb.start().waitFor() == 0;
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) Thread.currentThread().interrupt();
            return false;
        }
    }

    /**
     * Build the argument list for the specified PM2 command and process name.
     *
     * @param command PM2 command (e.g. {@code delete}, {@code describe})
     * @param pm2Name PM2 process name
     * @return argument list for the PM2 command
     */
    private List<String> buildArgs(String command, String pm2Name) {
        List<String> args = new ArrayList<>();
        String pm2Path = NodeBinaryFinder.findPM2Binary().getAbsolutePath();

        if (SystemUtils.IS_OS_WINDOWS) {
            args.add("cmd.exe");
            args.add("/c");
            args.add("\"" + pm2Path + "\"");
        } else {
            args.add(pm2Path);
        }

        args.add(command);
        args.add(pm2Name);
        return args;
    }
}
