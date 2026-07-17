package com.nordstrom.automation.selenium;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.core.GridServer;
import com.nordstrom.automation.selenium.core.LocalGridUtility;

/**
 * This interface extends {@link DriverPlugin} with the ability to create and manage
 * local Selenium Grid node servers.
 * <p>
 * Implementations provide both the client-side driver support defined by {@link DriverPlugin}
 * and the server-side node management required for local Grid instances.
 */
public interface ManagedDriverPlugin extends DriverPlugin {

    /**
     * Get dependency contexts for this driver.
     * <p>
     * Each entry is a fully-qualified class name whose containing JAR is required
     * on the classpath of the local Grid node process.
     *
     * @return driver dependency contexts
     */
    String[] getDependencyContexts();

    /**
     * Get driver capabilities as JSON string.
     *
     * @param config {@link SeleniumConfig} object
     * @return JSON driver capabilities
     */
    String getCapabilities(SeleniumConfig config);

    /**
     * Get names of supported System properties.
     *
     * @param capabilities required capabilities for target driver
     * @return System property names
     */
    String[] getPropertyNames(String capabilities);

    /**
     * Start local Selenium Grid node for this driver.
     *
     * @param config {@link SeleniumConfig} object
     * @param hubUrl Grid hub {@link URL} with which node should register
     * @return {@link GridServer} object for specified node
     * @throws IOException if an I/O error occurs
     */
    default GridServer create(SeleniumConfig config, URL hubUrl) throws IOException {
        String launcherClassName = config.getString(SeleniumSettings.GRID_LAUNCHER.key());
        String[] dependencyContexts = config.getDependencyContexts();
        String workingDir = config.getString(SeleniumSettings.GRID_WORKING_DIR.key());
        Path workingPath = (workingDir == null || workingDir.isEmpty()) ? null : Paths.get(workingDir);
        return create(config, hubUrl.getPort(), launcherClassName, dependencyContexts, hubUrl, workingPath);
    }

    /**
     * Start local Selenium Grid node for this driver.
     *
     * @param config {@link SeleniumConfig} object
     * @param launcherClassName fully-qualified name of {@code GridLauncher} class
     * @param dependencyContexts fully-qualified names of context classes for Selenium Grid dependencies
     * @param hubUrl Grid hub {@link URL} with which node should register
     * @param workingPath {@link Path} of working directory for server process; {@code null} for default
     * @return {@link GridServer} object for specified node
     * @throws IOException if an I/O error occurs
     */
    default GridServer create(SeleniumConfig config, String launcherClassName,
            String[] dependencyContexts, URL hubUrl, Path workingPath) throws IOException {
        return create(config, hubUrl.getPort(), launcherClassName, dependencyContexts, hubUrl, workingPath);
    }

    /**
     * Start local Selenium Grid node for this driver.
     *
     * @param config {@link SeleniumConfig} object
     * @param hubPort port of the Grid hub with which node should register
     * @param launcherClassName fully-qualified name of {@code GridLauncher} class
     * @param dependencyContexts fully-qualified names of context classes for Selenium Grid dependencies
     * @param hubUrl Grid hub {@link URL} with which node should register
     * @param workingPath {@link Path} of working directory for server process; {@code null} for default
     * @return {@link GridServer} object for specified node
     * @throws IOException if an I/O error occurs
     */
    default GridServer create(SeleniumConfig config, int hubPort, String launcherClassName,
            String[] dependencyContexts, URL hubUrl, Path workingPath) throws IOException {
        Path outputPath = LocalGridUtility.getOutputPath(config, false);
        GridServer nodeServer =
                create(config, hubPort, launcherClassName, dependencyContexts, hubUrl, workingPath, outputPath);
        nodeServer.getPersonalities().putAll(getPersonalities());
        return nodeServer;
    }

    /**
     * Start local Selenium Grid node for this driver.
     *
     * @param config {@link SeleniumConfig} object
     * @param hubPort port of the Grid hub with which node should register
     * @param launcherClassName fully-qualified class name for Grid launcher
     * @param dependencyContexts common dependency contexts for all Grid nodes
     * @param hubUrl Grid hub {@link URL} with which node should register
     * @param workingPath {@link Path} of working directory for server process; {@code null} for default
     * @param outputPath {@link Path} to output log file; {@code null} to decline log-to-file
     * @return {@link GridServer} object for specified node
     * @throws IOException if an I/O error occurs
     */
    GridServer create(SeleniumConfig config, int hubPort, String launcherClassName,
            String[] dependencyContexts, URL hubUrl, Path workingPath,
            Path outputPath) throws IOException;
}
