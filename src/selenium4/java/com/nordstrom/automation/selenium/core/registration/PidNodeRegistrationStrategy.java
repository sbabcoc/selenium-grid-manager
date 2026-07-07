package com.nordstrom.automation.selenium.core.registration;

import com.nordstrom.automation.selenium.core.LocalGridServer;
import com.nordstrom.automation.selenium.sidecar.GridServerRegistration;
import com.nordstrom.automation.selenium.sidecar.SidecarClient;

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
        SidecarClient.register(GridServerRegistration.forPidNode(
                server.getHubPort(), server.getUrl(), process.pid()));
    }
}
