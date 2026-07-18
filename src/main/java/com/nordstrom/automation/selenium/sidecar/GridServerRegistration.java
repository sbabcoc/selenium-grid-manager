package com.nordstrom.automation.selenium.sidecar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.nordstrom.common.base.UncheckedThrow;

/**
 * Immutable data class representing the registration of a grid server with the sidecar registry.
 * <p>
 * Instances are created via static factory methods and posted as JSON to the sidecar's
 * {@code /grid/control/register} endpoint by {@link SidecarClient}. The sidecar uses the
 * {@link ShutdownMode} to select the appropriate shutdown strategy when a grid collection
 * is shut down.
 * <p>
 * Serialization uses the {@code toJson()} / {@code fromJson(Map)} pattern supported by the
 * Selenium {@code Json} class, allowing instances to remain fully immutable.
 *
 * @since 36.0.0
 */
public class GridServerRegistration {

    private final int hubPort;
    private final URL serverUrl;
    private final boolean isHub;
    private final int apiVersion;
    private final ShutdownMode shutdownMode;
    private final Long pid;
    private final Integer pubPort;
    private final Integer subPort;
    private final URL lifecycleUrl;

    private GridServerRegistration(int hubPort, URL serverUrl, boolean isHub, int apiVersion,
            ShutdownMode shutdownMode, Long pid, Integer pubPort, Integer subPort, URL lifecycleUrl) {
        this.hubPort = hubPort;
        this.serverUrl = serverUrl;
        this.isHub = isHub;
        this.apiVersion = apiVersion;
        this.shutdownMode = shutdownMode;
        this.pid = pid;
        this.pubPort = pubPort;
        this.subPort = subPort;
        this.lifecycleUrl = lifecycleUrl;
    }

    /**
     * Create a registration for a Selenium 3 hub or node server managed via {@code LifecycleServlet}.
     *
     * @param hubPort port of the hub for the Grid collection this server belongs to
     * @param serverUrl {@link URL} of the server
     * @param isHub {@code true} if this server is a hub; otherwise {@code false}
     * @param lifecycleUrl {@link URL} of the server's {@code LifecycleServlet} endpoint
     * @return {@link GridServerRegistration} for the specified server
     */
    public static GridServerRegistration forLifecycle(int hubPort, URL serverUrl, boolean isHub, URL lifecycleUrl) {
        return new GridServerRegistration(hubPort, serverUrl, isHub, 3,
                ShutdownMode.LIFECYCLE, null, null, null, lifecycleUrl);
    }

    /**
     * Create a registration for a Selenium 4 hub server managed via PID.
     *
     * @param hubPort port of the hub for the Grid collection this server belongs to
     * @param serverUrl {@link URL} of the server
     * @param pid process ID of the server
     * @param pubPort event bus publisher port
     * @param subPort event bus subscriber port
     * @return {@link GridServerRegistration} for the specified server
     */
    public static GridServerRegistration forPidHub(int hubPort, URL serverUrl, long pid,
            int pubPort, int subPort) {
        return new GridServerRegistration(hubPort, serverUrl, true, 4,
                ShutdownMode.PID, pid, pubPort, subPort, null);
    }

    /**
     * Create a registration for a Selenium 4 non-hub server managed via PID.
     * Covers standard nodes and relay nodes.
     *
     * @param hubPort port of the hub for the Grid collection this server belongs to
     * @param serverUrl {@link URL} of the server
     * @param pid process ID of the server
     * @return {@link GridServerRegistration} for the specified server
     */
    public static GridServerRegistration forPidNode(int hubPort, URL serverUrl, long pid) {
        return new GridServerRegistration(hubPort, serverUrl, false, 4,
                ShutdownMode.PID, pid, null, null, null);
    }

    /**
     * Create a registration for a PM2-managed Appium server.
     * PM2 is unconditionally required for all Appium. API version is supplied
     * explicitly — it is always known in the launching JVM and should never be probed.
     *
     * @param hubPort port of the hub for the Grid collection this server belongs to
     * @param serverUrl {@link URL} of the server
     * @param apiVersion Selenium API version (3 or 4)
     * @return {@link GridServerRegistration} for the specified server
     */
    public static GridServerRegistration forAppiumPM2(int hubPort, URL serverUrl, int apiVersion) {
        return new GridServerRegistration(hubPort, serverUrl, false, apiVersion,
                ShutdownMode.PM2, null, null, null, null);
    }

    /**
     * Serialize this registration to a {@link Map} for JSON output.
     *
     * @return map representation of this registration
     */
    public Map<String, Object> toJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("hubPort", hubPort);
        map.put("serverUrl", serverUrl.toString());
        map.put("isHub", isHub);
        map.put("apiVersion", apiVersion);
        map.put("shutdownMode", shutdownMode.name());
        if (pid != null) map.put("pid", pid);
        if (pubPort != null) map.put("pubPort", pubPort);
        if (subPort != null) map.put("subPort", subPort);
        if (lifecycleUrl != null) map.put("lifecycleUrl", lifecycleUrl.toString());
        return map;
    }

    /**
     * Deserialize a {@link GridServerRegistration} from the specified map.
     *
     * @param map map representation of a registration
     * @return deserialized {@link GridServerRegistration}
     */
    public static GridServerRegistration fromJson(Map<String, Object> map) {
        try {
            int hubPort = ((Long) map.get("hubPort")).intValue();
            URL serverUrl = new URL((String) map.get("serverUrl"));
            boolean isHub = (Boolean) map.get("isHub");
            int apiVersion = ((Long) map.get("apiVersion")).intValue();
            ShutdownMode shutdownMode = ShutdownMode.valueOf((String) map.get("shutdownMode"));
            Long pid = (Long) map.get("pid");
            Integer pubPort = map.containsKey("pubPort") ? ((Long) map.get("pubPort")).intValue() : null;
            Integer subPort = map.containsKey("subPort") ? ((Long) map.get("subPort")).intValue() : null;
            URL lifecycleUrl = map.containsKey("lifecycleUrl") ? new URL((String) map.get("lifecycleUrl")) : null;
            return new GridServerRegistration(hubPort, serverUrl, isHub, apiVersion,
                    shutdownMode, pid, pubPort, subPort, lifecycleUrl);
        } catch (MalformedURLException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }

    /**
     * Get the port of the hub this server belongs to.
     * 
     * @return port of the hub for the Grid collection this server belongs to
     */
    public int getHubPort() {
        return hubPort;
    }

    /**
     * Get the URL of this server.
     * 
     * @return {@link URL} of this server
     */
    public URL getServerUrl() {
        return serverUrl;
    }

    /**
     * Determine if this server is a hub.
     * 
     * @return {@code true} if this server is a hub; otherwise {@code false}
     */
    public boolean isHub() {
        return isHub;
    }

    /**
     * Get the Selenium API version of this server.
     * 
     * @return Selenium API version (3 or 4)
     */
    public int getApiVersion() {
        return apiVersion;
    }

    /**
     * Get the shutdown mode for this server.
     * 
     * @return shutdown mechanism for this server
     */
    public ShutdownMode getShutdownMode() {
        return shutdownMode;
    }

    /**
     * Get the process ID for this server.
     * 
     * @return process ID of this server; {@code null} if not applicable
     */
    public Long getPid() {
        return pid;
    }

    /**
     * Get the event bus publish port for the hub this server belongs to.
     * 
     * @return event bus publisher port; {@code null} if not applicable
     */
    public Integer getPubPort() {
        return pubPort;
    }

    /**
     * Get the event bus subscribe port for the hub this server belongs to.
     * 
     * @return event bus subscribe port; {@code null} if not applicable
     */
    public Integer getSubPort() {
        return subPort;
    }

    /**
     * Get the URL of the lifecycle servlet endpoint for this server.
     * 
     * @return {@link URL} of the {@code LifecycleServlet} endpoint;
     *         {@code null} if not applicable
     */
    public URL getLifecycleUrl() {
        return lifecycleUrl;
    }
}
