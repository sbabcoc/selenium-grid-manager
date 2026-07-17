package com.nordstrom.automation.selenium.core;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.openqa.selenium.json.Json.LIST_OF_MAPS_TYPE;
import static org.openqa.selenium.json.Json.MAP_TYPE;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.grid.config.ConfigException;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.net.PortProber;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.utility.HostUtils;

/**
 * Selenium 4 implementation of {@link GridConfigFactory}.
 * <p>
 * Creates hub and node configuration files using JSON map manipulation
 * appropriate for the Selenium 4 TOML-compatible JSON configuration format.
 *
 * @since [next-major]
 */
public class GridConfigFactoryImpl implements GridConfigFactory {

    /** Singleton instance. */
    public static final GridConfigFactory INSTANCE = new GridConfigFactoryImpl();

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Path createHubConfig(SeleniumConfig config) throws IOException {
        Map<String, Object> hubConfig;
        // get path to hub configuration template
        String hubConfigPath = getHubConfigPath(config).toString();
        // create hub configuration from template
        try (Reader reader = Files.newBufferedReader(getHubConfigPath(config))) {
            hubConfig = new Json().toType(reader, MAP_TYPE);
        } catch (IOException e) {
            throw new ConfigException("Failed reading hub configuration template.", e);
        }

        String slotMatcher = config.getString(SeleniumSettings.SLOT_MATCHER.key());

        // strip extension to get template base path
        String configPathBase = hubConfigPath.substring(0, hubConfigPath.length() - 5);
        // get hash code of slot matcher as 8-digit hexadecimal string
        String hashCode = String.format("%08X", Objects.hash(slotMatcher));
        // assemble hub configuration file path with aggregated hash code
        Path filePath = Paths.get(configPathBase + "-" + hashCode + ".json");

        // if assembled path does not exist
        if (filePath.toFile().createNewFile()) {
            // add slot matcher configuration
            Map<String, Object> distributorOptions =
                    (Map<String, Object>) hubConfig.get("distributor");
            distributorOptions.put("slot-matcher", slotMatcher);
            try (OutputStream fos = new FileOutputStream(filePath.toFile());
                 OutputStream out = new BufferedOutputStream(fos)) {
                out.write(new Json().toJson(hubConfig).getBytes(UTF_8));
            }
        }
        return filePath;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @SuppressWarnings("unchecked")
    public Path createNodeConfig(SeleniumConfig config, String capabilities, URL hubUrl)
            throws IOException {
        Map<String, Object> nodeConfig;
        boolean isAppium = capabilities.contains("appium");
        // get path to node configuration template
        String nodeConfigPath = getNodeConfigPath(config).toString();
        // create node configuration from template
        try (Reader reader = Files.newBufferedReader(getNodeConfigPath(config))) {
            nodeConfig = new Json().toType(reader, MAP_TYPE);
            Map<String, Object> nodeOptions = (Map<String, Object>) nodeConfig.computeIfAbsent(
                    "node", k -> new HashMap<>());
            nodeOptions.put("hub",
                    hubUrl.getProtocol() + "://" + hubUrl.getAuthority() + "/grid/register");
            nodeOptions.computeIfAbsent("detect-drivers", k -> false);
            // if Appium
            if (isAppium) {
                // create relay configuration template if absent
                Map<String, Object> relayOptions = (Map<String, Object>) nodeConfig.computeIfAbsent(
                        "relay", k -> new HashMap<>());
                relayOptions.computeIfAbsent("host", k -> HostUtils.getLocalHost());
                relayOptions.computeIfAbsent("port", k -> PortProber.findFreePort());
                relayOptions.computeIfAbsent("configs", k -> new ArrayList<>());
            // otherwise (not Appium)
            } else {
                // add driver configuration template if absent
                nodeOptions.computeIfAbsent("driver-configuration", k -> new ArrayList<>());
            }
        } catch (IOException e) {
            throw new ConfigException("Failed reading node configuration template.", e);
        } catch (ClassCastException e) {
            throw new ConfigException("Failed unwrapping [node.driver-configuration] option", e);
        }

        // convert capabilities string to List<Map<String, Object>>
        String capsList = (capabilities.startsWith("[")) ? capabilities : "[" + capabilities + "]";
        List<Map<String, Object>> capsMapList = new Json().toType(capsList, LIST_OF_MAPS_TYPE);
        // convert list of maps into [MutableCapabilities]
        // => apply specified node modifications (if any)
        List<MutableCapabilities> capabilitiesList = capsMapList.stream()
                .map(MutableCapabilities::new)
                .map(theseCaps -> (MutableCapabilities) theseCaps.merge(
                        config.getModifications(theseCaps, NODE_MODS_SUFFIX)))
                .collect(Collectors.toList());

        // convert list of [MutableCapabilities] objects to set of maps
        Set<Map<String, Object>> capabilitiesSet = capabilitiesList.stream()
                .map(caps -> caps.toJson())
                .collect(Collectors.toSet());

        // strip extension to get template base path
        String configPathBase = nodeConfigPath.substring(0, nodeConfigPath.length() - 5);
        // get hash code of capabilities set and hub URL as 8-digit hexadecimal string
        String hashCode = String.format("%08X", Objects.hash(capabilitiesSet, hubUrl));
        // assemble node configuration file path with aggregated hash code
        Path filePath = Paths.get(configPathBase + "-" + hashCode + ".json");

        // if assembled path does not exist
        if (filePath.toFile().createNewFile()) {
            // if Appium
            if (isAppium) {
                // add relay slot specification
                Map<String, Object> relayOptions = (Map<String, Object>) nodeConfig.get("relay");
                List<Object> configs = (List<Object>) relayOptions.get("configs");
                capabilitiesList.forEach(theseCaps -> {
                    configs.add("1");
                    configs.add(config.toJson(theseCaps));
                });
            // otherwise (not Appium)
            } else {
                // add driver configuration
                Map<String, Object> nodeOptions = (Map<String, Object>) nodeConfig.get("node");
                List<Object> driverConfiguration =
                        (List<Object>) nodeOptions.get("driver-configuration");
                capabilitiesList.forEach(theseCaps -> {
                    Map<String, Object> thisConfig = new HashMap<>();
                    thisConfig.put("stereotype", theseCaps);
                    thisConfig.put("display-name", GridUtility.getPersonality(theseCaps));
                    Optional.ofNullable(GridUtility.getDriverPath(theseCaps))
                            .ifPresent(value -> thisConfig.put("webdriver-executable", value));
                    driverConfiguration.add(thisConfig);
                });
            }
            try (OutputStream fos = new FileOutputStream(filePath.toFile());
                 OutputStream out = new BufferedOutputStream(fos)) {
                out.write(new Json().toJson(nodeConfig).getBytes(UTF_8));
            }
        }
        return filePath;
    }
}
