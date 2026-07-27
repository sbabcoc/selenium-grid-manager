package com.nordstrom.automation.selenium.sidecar;

import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import org.openqa.selenium.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;

/**
 * Registry for Selenium Grid hubs the sidecar monitors but does not manage.
 * <p>
 * Monitored hubs are tracked for console visibility and easy access only —
 * the sidecar never attempts to shut them down. Entries persist across
 * sidecar restarts via the file specified by {@link SeleniumSettings#SIDECAR_MONITOR_FILE}.
 * <p>
 * This class is a static singleton accessed by sidecar servlets via {@link #getInstance()}.
 *
 * @since 36.2.0
 */
public class MonitoredGridRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(MonitoredGridRegistry.class);
    private static final MonitoredGridRegistry INSTANCE = new MonitoredGridRegistry();

    private final List<MonitoredGrid> monitored = new CopyOnWriteArrayList<>();
    private final Path storagePath;

    private MonitoredGridRegistry() {
        String fileName = SeleniumConfig.getConfig().getString(SeleniumSettings.SIDECAR_MONITOR_FILE.key());
        storagePath = Paths.get(fileName);
        load();
    }

    /**
     * Get the singleton {@link MonitoredGridRegistry} instance.
     *
     * @return singleton {@link MonitoredGridRegistry} instance
     */
    public static MonitoredGridRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Add the specified hub to the monitored list. No-op if already present.
     *
     * @param hubUrl {@link URL} of the hub to monitor
     * @param apiVersion Selenium API version (3 or 4)
     */
    public void add(URL hubUrl, int apiVersion) {
        if (isMonitored(hubUrl)) {
            return;
        }
        monitored.add(MonitoredGrid.of(hubUrl, apiVersion));
        LOGGER.debug("Added monitored grid at {}", hubUrl);
        save();
    }

    /**
     * Remove the specified hub from the monitored list.
     *
     * @param hubUrl {@link URL} of the hub to stop monitoring
     */
    public void remove(URL hubUrl) {
        boolean removed = monitored.removeIf(g -> g.getHubUrl().equals(hubUrl));
        if (removed) {
            LOGGER.debug("Removed monitored grid at {}", hubUrl);
            save();
        }
    }

    /**
     * Determine if the specified hub is already monitored.
     *
     * @param hubUrl {@link URL} to check
     * @return {@code true} if the specified hub is monitored; otherwise {@code false}
     */
    public boolean isMonitored(URL hubUrl) {
        return monitored.stream().anyMatch(g -> g.getHubUrl().equals(hubUrl));
    }

    /**
     * Get the list of all monitored grid entries.
     *
     * @return unmodifiable list of {@link MonitoredGrid} entries
     */
    public List<MonitoredGrid> getAll() {
        return Collections.unmodifiableList(new ArrayList<>(monitored));
    }

    @SuppressWarnings("unchecked")
    private void load() {
        if (!Files.exists(storagePath)) {
            return;
        }
        try {
            String json = new String(Files.readAllBytes(storagePath), StandardCharsets.UTF_8);
            Map<String, Object> wrapper = SeleniumConfig.getConfig().fromJson(json, Json.MAP_TYPE);
            List<Object> entries = (List<Object>) wrapper.getOrDefault("grids", Collections.emptyList());
            for (Object entry : entries) {
                monitored.add(MonitoredGrid.fromJson((Map<String, Object>) entry));
            }
            LOGGER.debug("Loaded {} monitored grid(s) from {}", monitored.size(), storagePath);
        } catch (IOException e) {
            LOGGER.warn("Failed loading monitored grids from {}: {}", storagePath, e.getMessage());
        }
    }

    private void save() {
        try {
            List<Map<String, Object>> entries = new ArrayList<>();
            for (MonitoredGrid grid : monitored) {
                entries.add(grid.toJson());
            }
            Map<String, Object> wrapper = new HashMap<>();
            wrapper.put("grids", entries);
            String json = SeleniumConfig.getConfig().toJson(wrapper);
            Files.write(storagePath, json.getBytes(StandardCharsets.UTF_8));
        } catch (IOException e) {
            LOGGER.warn("Failed saving monitored grids to {}: {}", storagePath, e.getMessage());
        }
    }
}
