package com.nordstrom.automation.selenium.core;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.openqa.selenium.net.PortProber;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.exceptions.GridServerLaunchFailedException;
import com.nordstrom.automation.selenium.grid.GridApiProvider;
import com.nordstrom.automation.selenium.grid.GridApiProviderRegistry;
import com.nordstrom.common.file.PathUtils;

/**
 * This class provides utility methods for managing local Selenium Grid instances.
 * <p>
 * <b>NOTE</b>: This class complements {@link GridUtility}, which provides general Grid
 * interaction utilities. Methods in this class are specific to local Grid management
 * and depend on the local Grid implementation classes.
 */
public final class LocalGridUtility {

    /**
     * Private constructor to prevent instantiation.
     */
    private LocalGridUtility() {
        throw new AssertionError("LocalGridUtility is a static utility class that cannot be instantiated");
    }

    /**
     * Get next configured output path for Grid server of specified role.
     *
     * @param config {@link SeleniumConfig} object
     * @param isHub role of Grid server being started: <ul>
     *     <li>{@code true} = hub</li>
     *     <li>{@code false} = node</li>
     *     <li>{@code null} = relay</li>
     * </ul>
     * @return Grid server output path (may be {@code null})
     */
    public static Path getOutputPath(SeleniumConfig config, Boolean isHub) {
        Path outputPath = null;

        if (!config.getBoolean(SeleniumSettings.GRID_NO_REDIRECT.key())) {
            String gridRole = (isHub == null) ? "relay" : (isHub) ? "hub" : "node";
            String logsFolder = config.getString(SeleniumSettings.GRID_LOGS_FOLDER.key());
            Path logsPath = Paths.get(logsFolder);
            if (!logsPath.isAbsolute()) {
                String workingDir = config.getString(SeleniumSettings.GRID_WORKING_DIR.key());
                if (workingDir == null || workingDir.isEmpty()) {
                    workingDir = System.getProperty("user.dir");
                }
                logsPath = Paths.get(workingDir, logsFolder);
            }

            try {
                if (!logsPath.toFile().exists()) {
                    Files.createDirectories(logsPath);
                }
                outputPath = PathUtils.getNextPath(logsPath, "grid-" + gridRole, "log");
            } catch (IOException e) {
                throw new GridServerLaunchFailedException(gridRole, e);
            }
        }

        return outputPath;
    }

    /**
     * Get instances of all configured local driver plugins.
     * <p>
     * <b>NOTE</b>: This method filters the full list of driver plugins returned by
     * {@link GridUtility#getDriverPlugins(SeleniumConfig)}, returning only those that
     * implement {@link ManagedDriverPlugin}.
     *
     * @param config {@link SeleniumConfig} object
     * @return list of local driver plug-in instances
     */
    public static List<ManagedDriverPlugin> getLocalDriverPlugins(SeleniumConfig config) {
        return GridUtility.getDriverPlugins(config).stream()
                .filter(p -> p instanceof ManagedDriverPlugin)
                .map(p -> (ManagedDriverPlugin) p)
                .collect(Collectors.toList());
    }
    
    /**
     * Get port number associated with the specified setting.
     * 
     * @param config {@link SeleniumConfig} object
     * @param setting associated configuration setting
     * @return resolved port number
     */
    public static Integer getLocalGridPort(SeleniumConfig config, SeleniumSettings setting) {
        return Optional.ofNullable(config.getInteger(setting.key(), null)).orElse(PortProber.findFreePort());
    }
    
    /**
     * Verify that the active hub at the specified URL matches the current runtime API version.
     *
     * @param hubUrl {@link URL} of the active hub
     * @throws IllegalStateException if the hub version doesn't match or isn't recognized
     */
    public static void verifyHubVersion(URL hubUrl) {
        GridApiProvider provider = GridApiProviderRegistry.forHub(hubUrl);
        if (provider == null) {
            throw new IllegalStateException("Active server at " + hubUrl + " is not a recognized "
                    + "Selenium Grid hub — shut down the existing server and retry");
        }
        int currentVersion = SeleniumConfig.getConfig().getVersion();
        if (provider.getApiVersion() != currentVersion) {
            throw new IllegalStateException("Active hub at " + hubUrl + " is Selenium "
                    + provider.getApiVersion() + " but current runtime is Selenium "
                    + currentVersion + " — shut down the existing grid and retry");
        }
    }
    
}
