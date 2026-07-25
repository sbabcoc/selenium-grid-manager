package com.nordstrom.automation.selenium.sidecar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.nordstrom.common.base.UncheckedThrow;

/**
 * Immutable data class representing a Selenium Grid hub that has been added to
 * the sidecar's monitored list. Monitored hubs are tracked for console visibility
 * and easy access only — the sidecar never attempts to shut them down.
 *
 * @since [next-major]
 */
public class MonitoredGrid {

    private final URL hubUrl;
    private final int apiVersion;
    private final long addedTime;

    private MonitoredGrid(URL hubUrl, int apiVersion, long addedTime) {
        this.hubUrl = hubUrl;
        this.apiVersion = apiVersion;
        this.addedTime = addedTime;
    }

    /**
     * Create a monitored grid entry, timestamped at the current time.
     *
     * @param hubUrl {@link URL} of the hub
     * @param apiVersion Selenium API version (3 or 4)
     * @return {@link MonitoredGrid} instance
     */
    public static MonitoredGrid of(URL hubUrl, int apiVersion) {
        return new MonitoredGrid(hubUrl, apiVersion, System.currentTimeMillis());
    }

    /**
     * Serialize this entry to a {@link Map} for JSON output.
     *
     * @return map representation of this entry
     */
    public Map<String, Object> toJson() {
        Map<String, Object> map = new HashMap<>();
        map.put("hubUrl", hubUrl.toString());
        map.put("apiVersion", apiVersion);
        map.put("addedTime", addedTime);
        return map;
    }

    /**
     * Deserialize a {@link MonitoredGrid} from the specified map.
     *
     * @param map map representation of an entry
     * @return deserialized {@link MonitoredGrid}
     */
    public static MonitoredGrid fromJson(Map<String, Object> map) {
        try {
            URL hubUrl = new URL((String) map.get("hubUrl"));
            int apiVersion = ((Long) map.get("apiVersion")).intValue();
            long addedTime = (Long) map.get("addedTime");
            return new MonitoredGrid(hubUrl, apiVersion, addedTime);
        } catch (MalformedURLException e) {
            throw UncheckedThrow.throwUnchecked(e);
        }
    }

    /** @return {@link URL} of the monitored hub */
    public URL getHubUrl() { return hubUrl; }

    /** @return Selenium API version (3 or 4) */
    public int getApiVersion() { return apiVersion; }

    /** @return time this entry was added, in epoch milliseconds */
    public long getAddedTime() { return addedTime; }
}
