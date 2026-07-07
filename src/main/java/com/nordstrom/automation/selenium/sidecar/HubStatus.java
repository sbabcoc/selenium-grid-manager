package com.nordstrom.automation.selenium.sidecar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.nordstrom.common.base.UncheckedThrow;

/**
 * Immutable data class representing the status of a Selenium Grid hub instance.
 * <p>
 * Instances are returned by the sidecar's {@code /grid/control/status} endpoint
 * as part of a {@link GridScanResult}. Three categories of hub are represented:
 * managed (launched and registered with this sidecar), discovered (found by background
 * scan), and monitored (future enhancement — explicitly registered remote instances).
 *
 * @since [next-major]
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
     * Get the status for a managed Selenium 3 hub.
     * 
     * @param hubUrl {@link URL} of the hub
     * @param active {@code true} if the hub is active
     * @return managed Selenium 3 hub status
     * @since [next-major]
     */
    public static HubStatus forSelenium3(URL hubUrl, boolean active) {
        return new HubStatus(hubUrl, active, 3, null, null, true, false);
    }

    /**
     * Get the status for a managed Selenium 4 hub.
     * 
     * @param hubUrl {@link URL} of the hub
     * @param active {@code true} if the hub is active
     * @param pubPort event bus publisher port
     * @param subPort event bus subscriber port
     * @return managed Selenium 4 hub status
     * @since [next-major]
     */
    public static HubStatus forSelenium4(URL hubUrl, boolean active, Integer pubPort, Integer subPort) {
        return new HubStatus(hubUrl, active, 4, pubPort, subPort, true, false);
    }

    /**
     * Get the status for a discovered Selenium 3 hub.
     * 
     * @param hubUrl {@link URL} of the hub
     * @return discovered Selenium 3 hub status
     * @since [next-major]
     */
    public static HubStatus discoveredSelenium3(URL hubUrl) {
        return new HubStatus(hubUrl, true, 3, null, null, false, false);
    }

    /**
     * Get the status for a discovered Selenium 4 hub.
     * 
     * @param hubUrl {@link URL} of the hub
     * @return discovered Selenium 4 hub status
     * @since [next-major]
     */
    public static HubStatus discoveredSelenium4(URL hubUrl) {
        return new HubStatus(hubUrl, true, 4, null, null, false, false);
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
