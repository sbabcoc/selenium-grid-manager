package com.nordstrom.automation.selenium.core;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.sidecar.SidecarClient;
import com.nordstrom.automation.selenium.sidecar.SidecarManager;

/**
 * This class registers the unmanaged hub monitor with {@link SeleniumGrid} when
 * {@code selenium-grid-manager} is on the classpath.
 *
 * @since [next-major]
 */
public class UnmanagedGridMonitorPlugin implements GridMonitorPlugin {
    static {
        SeleniumGrid.registerUnmanagedHubRegistrar((config, hubUrl) -> {
            if (!config.getBoolean(SeleniumSettings.MONITOR_UNMANAGED_HUBS.key())) {
                return;
            }
            int apiVersion = GridUtility.probeApiVersion(hubUrl);
            if (apiVersion > 0) {
                SidecarManager.ensureRunning();
                SidecarClient.monitor(hubUrl, apiVersion);
            }
        });
    }
}
