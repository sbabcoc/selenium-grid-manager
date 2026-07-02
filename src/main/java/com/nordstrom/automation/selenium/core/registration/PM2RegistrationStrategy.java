package com.nordstrom.automation.selenium.core.registration;

import com.nordstrom.automation.selenium.core.LocalSeleniumGrid.LocalGridServer;

/**
 * {@link RegistrationStrategy} for all Appium servers across Selenium 3 and 4.
 * PM2 is unconditionally required for all Appium. The API version is supplied
 * at construction time — it is always known in the launching JVM and is never
 * probed.
 *
 * @since [next-major]
 */
public class PM2RegistrationStrategy implements RegistrationStrategy {

    private final int apiVersion;

    /**
     * Constructor for PM2 registration strategy.
     *
     * @param apiVersion Selenium API version (3 or 4)
     */
    public PM2RegistrationStrategy(int apiVersion) {
        this.apiVersion = apiVersion;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(LocalGridServer server, Process process) {
        // TODO: Phase 2 - construct GridServerRegistration and pass to SidecarClient
        // SidecarClient.register(GridServerRegistration.forAppiumPM2(
        //     server.getHubPort(), server.getUrl(), apiVersion));
    }
}
