package com.nordstrom.automation.selenium.core;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.SystemUtils;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.core.registration.RegistrationStrategy;
import com.nordstrom.automation.selenium.utility.NodeBinaryFinder;
import com.nordstrom.common.file.PathUtils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This class represents a single Appium node server belonging to a local Grid collection.
 *
 * @since 36.0.0
 */
public class AppiumGridServer extends LocalGridServer {

    private static final Logger LOGGER = LoggerFactory.getLogger(AppiumGridServer.class);

    /**
     * Constructor for local Grid Appium node server object.
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
    public AppiumGridServer(String host, Integer port, boolean isHub, int hubPort,
            ProcessBuilder builder, Path workingPath, Path outputPath,
            RegistrationStrategy registrationStrategy) {
        super(host, port, isHub, hubPort, builder, workingPath, outputPath, registrationStrategy);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Uses the {@code /status} endpoint since Appium does not support
     * the standard {@code /wd/hub/status} endpoint.
     */
    @Override
    public boolean isActive() {
        return GridUtility.isHostActive(getUrl(), "/status");
    }
    
    /**
     * Get stored relay node configuration path.
     * <br>
     * <b>NOTE</b>: A relay node is needed to connect the Appium server to a Selenium 4+ Grid hub.
     *
     * @return path to relay node configuration; may be {@code null}
     */
    public Path getNodeConfigPath() {
        String nodeConfigPath = getEnvironment().get("nodeConfigPath");
        return (nodeConfigPath != null) ? Paths.get(nodeConfigPath) : null;
    }

    /**
     * If the specified URL is a local Appium node running with PM2, delete the process.
     *
     * @param nodeUrl {@link URL} object for target node server
     * @return {@code true} if node was shut down; otherwise {@code false}
     */
    public static boolean shutdownAppiumWithPM2(URL nodeUrl) {
        if (!GridUtility.isLocalHost(nodeUrl)) return false;

        int exitCode = 0;
        String executable;
        ProcessBuilder builder;
        List<String> argsList = new ArrayList<>();
        File pm2Binary = NodeBinaryFinder.findBinary("pm2", SeleniumSettings.PM2_BINARY_PATH.key()).getAbsoluteFile();

        argsList.add("delete");
        argsList.add("appium-" + nodeUrl.getPort());

        if (SystemUtils.IS_OS_WINDOWS) {
            executable = "cmd.exe";
            argsList.add(0, "\"" + pm2Binary.getAbsolutePath() + "\"");
            argsList.add(0, "/c");
        } else {
            executable = pm2Binary.getAbsolutePath();
        }

        argsList.add(0, executable);
        builder = new ProcessBuilder(argsList);
        builder.environment().put("PATH", PathUtils.getSystemPath());

        try {
            exitCode = builder.start().waitFor();
            LOGGER.debug("Deleted PM2 process: appium-{}", nodeUrl.getPort());
        } catch (IOException e) {
            LOGGER.debug("I/O exception while shutting down PM2-managed Appium node", e);
            exitCode = -1;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            LOGGER.debug("Interrupted while shutting down PM2-managed Appium node", e);
            exitCode = -1;
        }

        return exitCode == 0;
    }
}
