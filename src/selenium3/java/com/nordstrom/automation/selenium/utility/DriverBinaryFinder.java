package com.nordstrom.automation.selenium.utility;

import java.io.File;

import org.openqa.selenium.Capabilities;

import com.nordstrom.automation.selenium.SeleniumConfig;

import io.github.bonigarcia.wdm.WebDriverManager;

/**
 * Selenium 3 driver binary discovery, wrapping {@link WebDriverManager}.
 *
 * @since 36.0.0
 */
public class DriverBinaryFinder {

    /**
     * Private constructor to prevent instantiation.
     */
    private DriverBinaryFinder() {
        throw new AssertionError("DriverBinaryFinder is a static utility class that cannot be instantiated");
    }

    /**
     * Find/install driver indicated by the specified capabilities.
     * 
     * @param capabilities For driver binaries, the required capabilities for the specified driver
     * @return path to driver supporting specified capabilities as a {@link File} object
     */
    public static File findDriver(String capabilities) {
        Capabilities caps = SeleniumConfig.getConfig().getCapabilitiesForJson(capabilities)[0];
        WebDriverManager manager = WebDriverManager.getInstance(caps.getBrowserName()).capabilities(caps);
        manager.setup();
        return new File(manager.getDownloadedDriverPath());
    }
}
