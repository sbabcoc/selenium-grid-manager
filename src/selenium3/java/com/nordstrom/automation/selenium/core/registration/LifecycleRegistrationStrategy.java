package com.nordstrom.automation.selenium.core.registration;

import com.nordstrom.automation.selenium.core.LocalSeleniumGrid.LocalGridServer;

/**
 * {@link RegistrationStrategy} for Selenium 3 hub and node servers,
 * using the {@code LifecycleServlet} for shutdown.
 *
 * @since [next-major]
 */
public class LifecycleRegistrationStrategy implements RegistrationStrategy {

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(LocalGridServer server, Process process) {
        // TODO: Phase 2 - construct GridServerRegistration and pass to SidecarClient
        // try {
        //     URL lifecycleUrl = new URL(server.getUrl(), "/extra/LifecycleServlet");
        //     SidecarClient.register(GridServerRegistration.forLifecycle(
        //         server.getHubPort(), server.getUrl(), server.isHub(), lifecycleUrl));
        // } catch (MalformedURLException e) {
        //     throw UncheckedThrow.throwUnchecked(e);
        // }
    }
}
