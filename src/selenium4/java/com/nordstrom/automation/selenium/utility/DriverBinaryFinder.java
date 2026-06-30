package com.nordstrom.automation.selenium.utility;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

import org.openqa.selenium.Capabilities;
import org.openqa.selenium.manager.SeleniumManager;
import org.openqa.selenium.manager.SeleniumManagerOutput.Result;

import com.nordstrom.automation.selenium.SeleniumConfig;

/**
 * Selenium 4 driver binary discovery, wrapping {@link SeleniumManager}.
 *
 * @since [next-major]
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
        SeleniumManager manager = SeleniumManager.getInstance();
        Result result = manager.getBinaryPaths(
            new ArrayList<String>(Arrays.asList("--browser", caps.getBrowserName())));
        return new File(result.getDriverPath());
    }
}
