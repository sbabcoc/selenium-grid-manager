package com.nordstrom.automation.selenium.core.registration;

import java.net.MalformedURLException;
import java.net.URL;

import com.nordstrom.automation.selenium.core.LocalGridServer;
import com.nordstrom.automation.selenium.sidecar.GridServerRegistration;
import com.nordstrom.automation.selenium.sidecar.SidecarClient;
import com.nordstrom.common.uri.UriUtils;

import com.nordstrom.common.base.UncheckedThrow;

/**
 * {@link RegistrationStrategy} for Selenium 3 hub and node servers,
 * using the {@code LifecycleServlet} for shutdown.
 *
 * @since 36.0.0
 */
public class LifecycleRegistrationStrategy implements RegistrationStrategy {

    /**
     * {@inheritDoc}
     */
    @Override
    public void register(LocalGridServer server, Process process) {
        try {
            String path = server.isHub() ? "/lifecycle-manager/LifecycleServlet" : "/extra/LifecycleServlet";
            URL lifecycleUrl = UriUtils.uriForPath(server.getUrl(), path, "action=shutdown").toURL();
            SidecarClient.register(GridServerRegistration.forLifecycle(
                    server.getHubPort(), server.getUrl(), server.isHub(), lifecycleUrl));
        } catch (MalformedURLException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }
}
