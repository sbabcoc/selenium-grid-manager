package com.nordstrom.automation.selenium.core;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig;
import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;

/**
 * Factory interface for creating version-specific Selenium Grid configuration files.
 * <p>
 * Version-specific implementations ({@code GridConfigFactoryImpl}) are provided in
 * the appropriate source sets. The interface ensures completeness when adding support
 * for new Selenium API versions.
 * <p>
 * Default implementations are provided for path resolution methods since these
 * are version-agnostic.
 *
 * @since [next-major]
 */
public interface GridConfigFactory {

    /** suffix for node configuration modifier files */
    String NODE_MODS_SUFFIX = ".node.mods";
    
    Logger LOGGER = LoggerFactory.getLogger(GridConfigFactory.class);

    /**
     * Create a hub configuration file from the configured template.
     *
     * @param config {@link SeleniumConfig} object
     * @return {@link Path} of the generated hub configuration file
     * @throws IOException if an I/O error occurs
     */
    Path createHubConfig(SeleniumConfig config) throws IOException;

    /**
     * Create a node configuration file from the configured template.
     *
     * @param config {@link SeleniumConfig} object
     * @param capabilities node capabilities as a JSON string
     * @param hubUrl {@link URL} of the hub with which the node should register
     * @return {@link Path} of the generated node configuration file
     * @throws IOException if an I/O error occurs
     */
    Path createNodeConfig(SeleniumConfig config, String capabilities, URL hubUrl) throws IOException;

    /**
     * Get the path to the hub configuration template file.
     *
     * @param config {@link SeleniumConfig} object
     * @return {@link Path} of hub configuration template; {@code null} if not configured
     */
    default Path getHubConfigPath(SeleniumConfig config) {
        String path = AbstractSeleniumConfig.getConfigPath(config.getString(SeleniumSettings.HUB_CONFIG.key()));
        if (path == null) return null;
        LOGGER.debug("hubConfig = {}", path);
        return Paths.get(path);
    }

    /**
     * Get the path to the node configuration template file.
     *
     * @param config {@link SeleniumConfig} object
     * @return {@link Path} of node configuration template; {@code null} if not configured
     */
    default Path getNodeConfigPath(SeleniumConfig config) {
        String path = AbstractSeleniumConfig.getConfigPath(config.getString(SeleniumSettings.NODE_CONFIG.key()));
        if (path == null) return null;
        LOGGER.debug("nodeConfig = {}", path);
        return Paths.get(path);
    }

    /**
     * Get the path to the Appium configuration file.
     *
     * @param config {@link SeleniumConfig} object
     * @return {@link Path} of Appium configuration file; {@code null} if not configured
     */
    default Path getAppiumConfigPath(SeleniumConfig config) {
        String path = AbstractSeleniumConfig.getConfigPath(config.getString(SeleniumSettings.APPIUM_CONFIG_PATH.key()));
        if (path == null) return null;
        LOGGER.debug("appiumConfig = {}", path);
        return Paths.get(path);
    }
}
