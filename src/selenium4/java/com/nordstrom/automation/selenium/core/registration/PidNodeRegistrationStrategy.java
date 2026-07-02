package com.nordstrom.automation.selenium.core.registration;

import com.nordstrom.automation.selenium.core.LocalSeleniumGrid.LocalGridServer;

/**
 * {@link RegistrationStrategy} for Selenium 4 standard nodes and relay nodes.
 * All are structurally identical non-hub PID registrations.
 *
 * @since [next-major]
 */
public class PidNodeRegistrationStrategy implements RegistrationStrategy {

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(LocalGridServer server, Process process) {
        // TODO: Phase 2 - construct GridServerRegistration and pass to SidecarClient
        // SidecarClient.register(GridServerRegistration.forPid(
        //     server.getHubPort(), server.getUrl(), false,
        //     process.pid()));
    }
}
