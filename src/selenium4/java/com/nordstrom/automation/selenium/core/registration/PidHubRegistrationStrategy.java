package com.nordstrom.automation.selenium.core.registration;

import com.nordstrom.automation.selenium.core.LocalGridServer;

/**
 * {@link RegistrationStrategy} for Selenium 4 hub servers.
 * Captures the hub PID and event bus ports at launch time.
 *
 * @since [next-major]
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
        // TODO: Phase 2 - construct GridServerRegistration and pass to SidecarClient
        // SidecarClient.register(GridServerRegistration.forPid(
        //     server.getHubPort(), server.getUrl(), true,
        //     process.pid(), pubPort, subPort));
    }
}
