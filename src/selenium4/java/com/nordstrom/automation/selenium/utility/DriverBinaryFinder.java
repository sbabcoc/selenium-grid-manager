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
     * <p>
     * <b>NOTE</b>: If {@code driverPathProperty} is set (as a system property or, failing that, via
     * {@code driverPathEnvVar}) and resolves to an executable file, this path is returned directly and
     * {@link SeleniumManager} is never invoked.
     * 
     * @param capabilities For driver binaries, the required capabilities for the specified driver
     * @param driverPathProperty name of the system property that specifies the path to the driver executable
     * @param driverPathEnvVar name of the environment variable that specifies the path to the driver
     *        executable; consulted only if {@code driverPathProperty} is unset
     * @return path to driver supporting specified capabilities as a {@link File} object
     */
    public static File findDriver(String capabilities, String driverPathProperty, String driverPathEnvVar) {
        String driverPath = System.getProperty(driverPathProperty, System.getenv(driverPathEnvVar));
        if (driverPath != null) {
            File driverFile = new File(driverPath);
            if (driverFile.canExecute()) {
                return driverFile;
            }
        }
        Capabilities caps = SeleniumConfig.getConfig().getCapabilitiesForJson(capabilities)[0];
        SeleniumManager manager = SeleniumManager.getInstance();
        Result result = manager.getBinaryPaths(
            new ArrayList<String>(Arrays.asList("--browser", caps.getBrowserName())));
        return new File(result.getDriverPath());
    }
}
