package com.nordstrom.automation.selenium.sidecar;

import java.net.MalformedURLException;
import java.net.URL;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.utility.HostUtils;
import com.nordstrom.common.uri.UriUtils;

/**
 * Client for communicating with the sidecar servlet container.
 * <p>
 * This is a stub implementation. Full implementation is provided in Phase 2.
 *
 * @since [next-major]
 */
public class SidecarClient {

    private SidecarClient() {
        throw new AssertionError("SidecarClient is a static utility class that cannot be instantiated");
    }

    /**
     * Register a grid server with the sidecar registry.
     * <p>
     * <b>NOTE</b>: This is a stub implementation. Full implementation is provided in Phase 2.
     *
     * @param registration the registration object describing the server
     */
    public static void register(Object registration) {
        // TODO: Phase 2 - post registration to sidecar
    }

    /**
     * Request the sidecar to shut down the grid collection on the specified hub port.
     * <p>
     * <b>NOTE</b>: This is a stub implementation. Full implementation is provided in Phase 2.
     *
     * @param hubPort port of the hub whose collection should be shut down
     */
    public static void shutdown(int hubPort) {
        // TODO: Phase 2 - post shutdown request to sidecar
    }
    
    private static URL sidecarUrl() {
        int port = SeleniumConfig.getConfig().getInt(SeleniumSettings.SIDECAR_PORT.key());
        try {
            return UriUtils.makeBasicURI("http", HostUtils.getLocalHost(), port).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to create sidecar URL", e);
        }
    }
}
