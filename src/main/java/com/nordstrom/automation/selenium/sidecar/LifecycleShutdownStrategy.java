package com.nordstrom.automation.selenium.sidecar;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Shutdown strategy for Selenium 3 hub and node servers.
 * Sends an HTTP POST to the server's {@code LifecycleServlet} endpoint.
 * <p>
 * This is an internal implementation detail of the sidecar — it is not a user extension point.
 * The appropriate strategy is selected automatically based on the {@link ShutdownMode} recorded
 * in the {@link GridServerRegistration} at launch time.
 *
 * @since [next-major]
 */
class LifecycleShutdownStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(LifecycleShutdownStrategy.class);

    /**
     * Shut down the server identified by the specified registration by posting to its
     * {@code LifecycleServlet} endpoint.
     *
     * @param registration {@link GridServerRegistration} of the server to shut down
     * @throws IOException if the HTTP request fails
     */
    void shutdown(GridServerRegistration registration) throws IOException {
        URL lifecycleUrl = registration.getLifecycleUrl();
        HttpURLConnection conn = (HttpURLConnection) lifecycleUrl.openConnection();
        try {
            conn.setRequestMethod("POST");
            conn.setConnectTimeout(5000);
            conn.setReadTimeout(5000);
            conn.setDoOutput(true);
            int responseCode = conn.getResponseCode();
            LOGGER.debug("LifecycleServlet responded {} for server at {}",
                    responseCode, registration.getServerUrl());
        } finally {
            conn.disconnect();
        }
    }
}
