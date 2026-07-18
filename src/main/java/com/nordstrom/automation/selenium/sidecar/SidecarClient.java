package com.nordstrom.automation.selenium.sidecar;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.exceptions.SidecarUnavailableException;
import com.nordstrom.automation.selenium.utility.HostUtils;
import com.nordstrom.common.uri.UriUtils;

/**
 * Client for communicating with the sidecar servlet container.
 * <p>
 * Provides static methods for registering grid servers, requesting shutdowns,
 * and stopping the sidecar itself. All methods use Apache HttpClient for HTTP
 * communication, consistent with
 * {@link com.nordstrom.automation.selenium.core.GridUtility}.
 *
 * @since 36.0.0
 */
public class SidecarClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(SidecarClient.class);

    private SidecarClient() {
        throw new AssertionError("SidecarClient is a static utility class that cannot be instantiated");
    }

    /**
     * Register a grid server with the sidecar registry.
     *
     * @param registration {@link GridServerRegistration} describing the server to register
     * @throws SidecarUnavailableException if the registration request fails
     */
    public static void register(GridServerRegistration registration) {
        URL url = endpointUrl("/grid/control/register");
        String json = SeleniumConfig.getConfig().toJson(registration.toJson());
        postJson(url, json);
        LOGGER.debug("Registered {} server at {} with sidecar",
                registration.isHub() ? "hub" : "node/relay/appium",
                registration.getServerUrl());
    }

    /**
     * Request the sidecar to shut down the grid collection on the specified hub port.
     *
     * @param hubPort port of the hub whose collection should be shut down
     * @throws SidecarUnavailableException if the shutdown request fails
     */
    public static void shutdown(int hubPort) {
        URL url = endpointUrl("/grid/control/shutdown");
        postForm(url, "hubPort=" + hubPort);
        LOGGER.debug("Requested sidecar shutdown of grid collection on port {}", hubPort);
    }

    /**
     * Request the sidecar to shut down all managed grid collections and stop.
     *
     * @throws SidecarUnavailableException if the stop request fails
     */
    public static void stop() {
        URL url = endpointUrl("/grid/control/stop");
        postForm(url, "");
        LOGGER.debug("Requested sidecar stop");
    }

    /**
     * Build the URL for the specified sidecar endpoint path.
     *
     * @param path endpoint path
     * @return {@link URL} for the specified endpoint
     */
    private static URL endpointUrl(String path) {
        try {
            int port = SeleniumConfig.getConfig().getInt(SeleniumSettings.SIDECAR_PORT.key());
            return UriUtils.makeBasicURI("http", HostUtils.getLocalHost(), port, path).toURL();
        } catch (MalformedURLException e) {
            throw new RuntimeException("Unable to create sidecar endpoint URL for path: " + path, e);
        }
    }

    /**
     * POST JSON content to the specified URL.
     *
     * @param url target {@link URL}
     * @param json JSON content to post
     * @throws SidecarUnavailableException if the request fails
     */
    private static void postJson(URL url, String json) {
        post(url, new StringEntity(json, ContentType.APPLICATION_JSON));
    }

    /**
     * POST form content to the specified URL.
     *
     * @param url target {@link URL}
     * @param body form body content
     * @throws SidecarUnavailableException if the request fails
     */
    private static void postForm(URL url, String body) {
        post(url, new StringEntity(body, ContentType.APPLICATION_FORM_URLENCODED));
    }

    /**
     * POST the specified entity to the specified URL.
     *
     * @param url target {@link URL}
     * @param entity request entity
     * @throws SidecarUnavailableException if the request fails
     */
    private static void post(URL url, StringEntity entity) {
        try (CloseableHttpClient client = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(url.toURI());
            request.setHeader("X-OS-User", System.getProperty("user.name"));
            request.setEntity(entity);
            try (CloseableHttpResponse response = client.execute(request)) {
                int statusCode = response.getStatusLine().getStatusCode();
                EntityUtils.consume(response.getEntity());
                if (statusCode == 401) {
                    throw new SidecarUnavailableException(url,
                            "Unauthorized — configure " + SeleniumSettings.SIDECAR_STOP_TOKEN.key());
                }
                if (statusCode >= 400) {
                    throw new SidecarUnavailableException(url,
                            "Request failed with status: " + statusCode);
                }
            }
        } catch (SidecarUnavailableException e) {
            throw e;
        } catch (Exception e) {
            throw new SidecarUnavailableException(url, e);
        }
    }
}
