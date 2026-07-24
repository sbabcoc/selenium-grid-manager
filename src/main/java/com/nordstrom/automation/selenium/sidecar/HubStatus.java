package com.nordstrom.automation.selenium.sidecar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.nordstrom.automation.selenium.sidecar.servlet.SidecarPathName;

import com.nordstrom.common.base.UncheckedThrow;

/**
 * Immutable data class representing the status of a Selenium Grid hub instance.
 * <p>
 * Instances are returned by the sidecar's {@value SidecarPathName#STATUS_PATH}
 * endpoint as part of a {@link GridScanResult}. Three categories of hub are
 * represented: managed (launched and registered with this sidecar), discovered
 * (found by background scan), and monitored (future enhancement — explicitly
 * registered remote instances).
 *
 * @since 36.0.0
 */
public class HubStatus {

    private final URL hubUrl;
    private final boolean active;
    private final int apiVersion;
    private final Integer pubPort;
    private final Integer subPort;
    private final boolean managed;
    private final boolean monitored;

    private HubStatus(URL hubUrl, boolean active, int apiVersion,
            Integer pubPort, Integer subPort, boolean managed, boolean monitored) {
        this.hubUrl = hubUrl;
        this.active = active;
        this.apiVersion = apiVersion;
        this.pubPort = pubPort;
        this.subPort = subPort;
        this.managed = managed;
        this.monitored = monitored;
    }

    /**
     * Create a {@link HubStatus} for a managed Grid hub of the specified API version.
     * <p>
     * Returns {@code null} if the specified API version is not recognized.
     *
     * @param hubUrl {@link URL} of the managed hub
     * @param apiVersion Selenium API version (3 or 4)
     * @param active {@code true} if the hub is currently active
     * @param pubPort event bus publisher port; {@code null} if not applicable
     * @param subPort event bus subscriber port; {@code null} if not applicable
     * @return {@link HubStatus} for the managed hub, or {@code null} if the API version
     *         is not recognized
     * @since 36.0.0
     */
    public static HubStatus managed(URL hubUrl, int apiVersion, boolean active,
            Integer pubPort, Integer subPort) {
        if (apiVersion == 3) return new HubStatus(hubUrl, active, 3, null, null, true, false);
        if (apiVersion == 4) return new HubStatus(hubUrl, active, 4, pubPort, subPort, true, false);
        return null;
    }

    /**
     * Create a {@link HubStatus} for a discovered unmanaged Grid hub of the specified API version.
     * <p>
     * Returns {@code null} if the specified API version is not recognized, allowing callers
     * to use a simple null check to filter out non-Grid servers.
     *
     * @param hubUrl {@link URL} of the discovered hub
     * @param apiVersion Selenium API version (3 or 4)
     * @return {@link HubStatus} for the discovered hub, or {@code null} if the API version
     *         is not recognized
     * @since 36.0.0
     */
    public static HubStatus discovered(URL hubUrl, int apiVersion) {
        if (apiVersion == 3 || apiVersion == 4) {
            return new HubStatus(hubUrl, true, apiVersion, null, null, false, false);
        }
        return null;
    }

    /**
     * Serialize this status to a {@link Map} for JSON output.
     *
     * @return map representation of this status
     */
    public Map<String, Object> toJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("hubUrl", hubUrl.toString());
        map.put("active", active);
        map.put("apiVersion", apiVersion);
        if (pubPort != null) map.put("pubPort", pubPort);
        if (subPort != null) map.put("subPort", subPort);
        map.put("managed", managed);
        map.put("monitored", monitored);
        return map;
    }

    /**
     * Deserialize a {@link HubStatus} from the specified map.
     *
     * @param map map representation of a hub status
     * @return deserialized {@link HubStatus}
     */
    public static HubStatus fromJson(Map<String, Object> map) {
        try {
            URL hubUrl = new URL((String) map.get("hubUrl"));
            boolean active = (Boolean) map.get("active");
            int apiVersion = ((Long) map.get("apiVersion")).intValue();
            Integer pubPort = map.containsKey("pubPort") ? ((Long) map.get("pubPort")).intValue() : null;
            Integer subPort = map.containsKey("subPort") ? ((Long) map.get("subPort")).intValue() : null;
            boolean managed = (Boolean) map.get("managed");
            boolean monitored = (Boolean) map.get("monitored");
            return new HubStatus(hubUrl, active, apiVersion, pubPort, subPort, managed, monitored);
        } catch (MalformedURLException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }

    /**
     * Get the URL of this hub.
     * 
     * @return {@link URL} of this hub
     */
    public URL getHubUrl() { return hubUrl; }

    /**
     * Determine if this hub is active.
     * 
     * @return {@code true} if this hub is active
     */
    public boolean isActive() { return active; }

    /**
     * Get the Selenium API version of this server.
     * 
     * @return Selenium API version (3 or 4)
     */
    public int getApiVersion() { return apiVersion; }

    /**
     * Get the event bus publish port for this hub.
     * 
     * @return event bus publisher port; {@code null} if not applicable
     */
    public Integer getPubPort() { return pubPort; }

    /**
     * Get the event bus subscribe port for this hub.
     * 
     * @return event bus subscribe port; {@code null} if not applicable
     */
    public Integer getSubPort() { return subPort; }

    /**
     * Determine if this hub is managed by the sidecar.
     * 
     * @return {@code true} if this hub is managed by the sidecar
     */
    public boolean isManaged() { return managed; }

    /**
     * Determine if this hub is being monitored by the sidecar.
     * 
     * @return {@code true} if this hub is being monitored by the sidecar
     */
    public boolean isMonitored() { return monitored; }
}
