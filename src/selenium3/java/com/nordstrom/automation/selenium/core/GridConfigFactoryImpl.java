package com.nordstrom.automation.selenium.core;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.StringReader;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.grid.internal.utils.configuration.GridHubConfiguration;
import org.openqa.grid.internal.utils.configuration.GridNodeConfiguration;
import org.openqa.selenium.MutableCapabilities;
import org.openqa.selenium.grid.config.ConfigException;
import org.openqa.selenium.json.Json;
import org.openqa.selenium.json.JsonInput;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;

/**
 * Selenium 3 implementation of {@link GridConfigFactory}.
 * <p>
 * Creates hub and node configuration files using the Selenium 3
 * {@link GridHubConfiguration} and {@link GridNodeConfiguration} APIs.
 *
 * @since 36.0.0
 */
public class GridConfigFactoryImpl implements GridConfigFactory {

    /** Singleton instance. */
    public static final GridConfigFactory INSTANCE = new GridConfigFactoryImpl();

    private static final String JSON_HEAD = "{ \"capabilities\": [";
    private static final String JSON_TAIL = "] }";

    /**
     * {@inheritDoc}
     */
    @Override
    public Path createHubConfig(SeleniumConfig config) throws IOException {
        // get path to hub configuration template
        String hubConfigPath = getHubConfigPath(config).toString();
        // create hub configuration from template
        GridHubConfiguration hubConfig = GridHubConfiguration.loadFromJSON(hubConfigPath);

        String slotMatcher = config.getString(SeleniumSettings.SLOT_MATCHER.key());

        // get configured hub servlet collection
        Set<String> servlets = new HashSet<>(hubConfig.servlets);
        // always add ProxyListServlet for programmatic node enumeration
        servlets.add("com.nordstrom.automation.selenium.servlet.ProxyListServlet");
        // add servlets from HUB_SERVLETS setting
        String hubServlets = config.getString(SeleniumSettings.HUB_SERVLETS.key());
        if (hubServlets != null && !hubServlets.isEmpty()) {
            for (String servlet : hubServlets.split(",")) {
                servlets.add(servlet.trim());
            }
        }

        // strip extension to get template base path
        String configPathBase = hubConfigPath.substring(0, hubConfigPath.length() - 5);
        // get hash code of slot matcher and servlets as 8-digit hexadecimal string
        String hashCode = String.format("%08X", Objects.hash(slotMatcher, servlets));
        // assemble hub configuration file path with servlets hash code
        Path filePath = Paths.get(configPathBase + "-" + hashCode + ".json");

        // if assembled path does not exist
        if (filePath.toFile().createNewFile()) {
            try {
                hubConfig.capabilityMatcher =
                        (CapabilityMatcher) Class.forName(slotMatcher).newInstance();
            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
                throw new ConfigException("Failed instantiating capability matcher: " + slotMatcher, e);
            }
            hubConfig.servlets = Arrays.asList(servlets.toArray(new String[0]));
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
    public Path createNodeConfig(SeleniumConfig config, String capabilities, URL hubUrl)
            throws IOException {
        // get path to node configuration template
        String nodeConfigPath = getNodeConfigPath(config).toString();
        // create node configuration from template
        GridNodeConfiguration nodeConfig = GridNodeConfiguration.loadFromJSON(nodeConfigPath);

        // convert capabilities string to [JsonInput] object
        JsonInput input = new Json().newInput(new StringReader(JSON_HEAD + capabilities + JSON_TAIL));
        // convert [JsonInput] object to list of [MutableCapabilities] objects
        List<MutableCapabilities> capabilitiesList =
                GridNodeConfiguration.loadFromJSON(input).capabilities;
        // for each [MutableCapabilities] object
        for (MutableCapabilities theseCaps : capabilitiesList) {
            // apply specified node modifications (if any)
            theseCaps.merge(config.getModifications(theseCaps, NODE_MODS_SUFFIX));
        }

        // convert list of [MutableCapabilities] objects to set of maps
        Set<Map<String, Object>> capabilitiesSet = capabilitiesList.stream()
                .map(caps -> caps.toJson())
                .collect(Collectors.toSet());

        // get configured node servlet collection
        Set<String> servlets = new HashSet<>(nodeConfig.servlets);
        // always add LifecycleServlet for sidecar-based shutdown
        servlets.add("org.openqa.grid.web.servlet.LifecycleServlet");
        // add servlets from NODE_SERVLETS setting
        String nodeServlets = config.getString(SeleniumSettings.NODE_SERVLETS.key());
        if (nodeServlets != null && !nodeServlets.isEmpty()) {
            for (String servlet : nodeServlets.split(",")) {
                servlets.add(servlet.trim());
            }
        }

        // strip extension to get template base path
        String configPathBase = nodeConfigPath.substring(0, nodeConfigPath.length() - 5);
        // get hash code of capabilities set, hub URL, and servlets as 8-digit hexadecimal string
        String hashCode = String.format("%08X", Objects.hash(capabilitiesSet, hubUrl, servlets));
        // assemble node configuration file path with aggregated hash code
        Path filePath = Paths.get(configPathBase + "-" + hashCode + ".json");

        // if assembled path does not exist
        if (filePath.toFile().createNewFile()) {
            nodeConfig.hub = null;
            nodeConfig.capabilities = capabilitiesList;
            nodeConfig.hubHost = hubUrl.getHost();
            nodeConfig.hubPort = hubUrl.getPort();
            nodeConfig.servlets = Arrays.asList(servlets.toArray(new String[0]));
            try (OutputStream fos = new FileOutputStream(filePath.toFile());
                 OutputStream out = new BufferedOutputStream(fos)) {
                out.write(new Json().toJson(nodeConfig).getBytes(UTF_8));
            }
        }
        return filePath;
    }
}
