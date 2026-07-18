package com.nordstrom.automation.selenium.core.registration;

import com.nordstrom.automation.selenium.core.LocalGridServer;
import com.nordstrom.automation.selenium.sidecar.GridServerRegistration;
import com.nordstrom.automation.selenium.sidecar.SidecarClient;

/**
 * {@link RegistrationStrategy} for Selenium 4 hub servers.
 * Captures the hub PID and event bus ports at launch time.
 *
 * @since 36.0.0
 */
public class PidHubRegistrationStrategy implements RegistrationStrategy {

    private final int pubPort;
    private final int subPort;

    /**
     * Constructor for PID hub registration strategy.
     *
     * @param pubPort event bus publisher port
     * @param subPort event bus subscriber port
     */
    public PidHubRegistrationStrategy(int pubPort, int subPort) {
        this.pubPort = pubPort;
        this.subPort = subPort;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(LocalGridServer server, Process process) {
        SidecarClient.register(GridServerRegistration.forPidHub(
                server.getHubPort(), server.getUrl(), process.pid(), pubPort, subPort));
    }
}
