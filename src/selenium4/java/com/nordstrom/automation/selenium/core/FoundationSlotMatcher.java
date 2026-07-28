//Licensed to the Software Freedom Conservancy (SFC) under one
//or more contributor license agreements.  See the NOTICE file
//distributed with this work for additional information
//regarding copyright ownership.  The SFC licenses this file
//to you under the Apache License, Version 2.0 (the
//"License"); you may not use this file except in compliance
//with the License.  You may obtain a copy of the License at
//
//http://www.apache.org/licenses/LICENSE-2.0
//
//Unless required by applicable law or agreed to in writing,
//software distributed under the License is distributed on an
//"AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
//KIND, either express or implied.  See the License for the
//specific language governing permissions and limitations
//under the License.

package com.nordstrom.automation.selenium.core;

import java.io.Serializable;
import java.util.Objects;
import org.openqa.selenium.Capabilities;
import org.openqa.selenium.grid.data.SlotMatcher;

/**
* Default matching implementation for slots, loosely based on the requirements for capability
* matching from the WebDriver spec, and kept in step with upstream {@code DefaultSlotMatcher}.
* A match is made if the following are all true:
*
* <ul>
*   <li>All non-extension capabilities from the {@code stereotype} match those in the {@link
*       Capabilities} being considered.
*   <li>If the {@link Capabilities} being considered contain browserName or platformName, the
*       {@code stereotype} must contain the same values. If they contain browserVersion, the
*       stereotype's value must match under a dot-separated, prefix-based comparison (e.g. a
*       request for {@code "131"} matches a stereotype of {@code "131.0.6778.85"}), mirroring
*       upstream's semantic-version matching.
*   <li>A namespaced {@code platformVersion} capability (e.g. {@code appium:platformVersion}) is
*       matched directly between stereotype and requested capabilities, ahead of general
*       extension-capability handling — same as upstream.
*   <li>Other extension capabilities are skipped for matching per the RFC's stated rule for
*       configuration options: a name with the {@code se:} prefix, or ending in {@code options},
*       {@code Options}, {@code loggingPrefs}, or {@code debuggerAddress}. Everything else is
*       compared for equality — deliberately without upstream {@code DefaultSlotMatcher}'s
*       hardcoded vendor-prefix exclusion list ({@code goog:}, {@code moz:}, {@code ms:}, {@code
*       safari:} — wholesale-ignored regardless of suffix). That list treats identical
*       capability shapes differently depending on which vendor happens to own the prefix, which
*       is the behavior this class exists to avoid; the four exclusions above are each excluded
*       on their own, RFC-stated merits, not as leftover parts of that list.
* </ul>
*
* <p>A second deliberate difference from upstream: the extension-capability check above is
* driven by the <b>requested</b> capabilities rather than the stereotype's declared capabilities.
* Upstream iterates the stereotype's capability names, so a capability the client requests but
* the stereotype never declares at all is silently skipped rather than treated as a mismatch —
* e.g. a request differentiated only by {@code appium:automationName} can match a plain browser
* stereotype that declares no extension capabilities whatsoever. Driving the check from the
* request side closes that gap.
*/
@SuppressWarnings("serial")
public class FoundationSlotMatcher implements SlotMatcher, Serializable {

    @Override
    public boolean matches(Capabilities stereotype, Capabilities capabilities) {

        if (capabilities.asMap().isEmpty()) {
            return false;
        }

        if (!initialMatch(stereotype, capabilities)) {
            return false;
        }

        if (!managedDownloadsEnabled(stereotype, capabilities)) {
            return false;
        }

        if (!platformVersionMatch(stereotype, capabilities)) {
            return false;
        }

        if (!extensionCapabilitiesMatch(stereotype, capabilities)) {
            return false;
        }

        // At the end, a simple browser, browserVersion and platformName match
        boolean browserNameMatch = capabilities.getBrowserName() == null 
                || capabilities.getBrowserName().isEmpty()
                || Objects.equals(stereotype.getBrowserName(), capabilities.getBrowserName());
        boolean browserVersionMatch = capabilities.getBrowserVersion() == null
                || capabilities.getBrowserVersion().isEmpty()
                || Objects.equals(capabilities.getBrowserVersion(), "stable")
                || semanticVersionMatch(stereotype.getBrowserVersion(), capabilities.getBrowserVersion());
        boolean platformNameMatch = capabilities.getPlatformName() == null
                || Objects.equals(stereotype.getPlatformName(), capabilities.getPlatformName())
                || (stereotype.getPlatformName() != null
                        && stereotype.getPlatformName().is(capabilities.getPlatformName()));
        return browserNameMatch && browserVersionMatch && platformNameMatch;
    }

    private Boolean initialMatch(Capabilities stereotype, Capabilities capabilities) {
        return stereotype.getCapabilityNames().stream()
                // Matching of extension capabilities is handled separately. Skip them
                .filter(name -> !name.contains(":"))
                // Platform matching is special, we do it later
                .filter(name -> !"platformName".equalsIgnoreCase(name))
                .filter(name -> capabilities.getCapability(name) != null).map(name -> {
                    if (stereotype.getCapability(name) instanceof String
                            && capabilities.getCapability(name) instanceof String) {
                        return ((String) stereotype.getCapability(name))
                                .equalsIgnoreCase((String) capabilities.getCapability(name));
                    }
                    return Objects.equals(stereotype.getCapability(name), capabilities.getCapability(name));
                }).reduce(Boolean::logicalAnd).orElse(true);
    }

    private Boolean managedDownloadsEnabled(Capabilities stereotype, Capabilities capabilities) {
        // First lets check if user wanted a Node with managed downloads enabled
        Object raw = capabilities.getCapability("se:downloadsEnabled");
        if (raw == null || !Boolean.parseBoolean(raw.toString())) {
            // User didn't ask. So lets move on to the next matching criteria
            return true;
        }
        // User wants managed downloads enabled to be done on this Node, let's check the
        // stereotype
        raw = stereotype.getCapability("se:downloadsEnabled");
        // Try to match what the user requested
        return raw != null && Boolean.parseBoolean(raw.toString());
    }

    private Boolean platformVersionMatch(Capabilities stereotype, Capabilities capabilities) {
        /*
         * This platform version match is not W3C compliant but users can add Appium
         * servers as Nodes, so we avoid delaying the match until the Slot, which makes
         * the whole matching process faster.
         */
        return capabilities.getCapabilityNames().stream().filter(name -> name.contains("platformVersion"))
                .map(platformVersionCapName -> Objects.equals(stereotype.getCapability(platformVersionCapName),
                        capabilities.getCapability(platformVersionCapName)))
                .reduce(Boolean::logicalAnd).orElse(true);
    }

    /**
    * Compare two browser version strings with dot-separated, prefix-based matching, mirroring
    * the documented/tested behavior of upstream {@code DefaultSlotMatcher}'s semantic-version
    * comparison: a less-specific version (e.g. {@code "131"}) matches a more-specific one (e.g.
    * {@code "131.0.6778.85"}) as long as every segment they share in common is identical — but
    * {@code "131.0.6778.95"} does not match {@code "131.0.6778.85"}, since the fourth segment
    * actually differs.
    * <p>
    * This is an independent reimplementation based on upstream's documented test cases, not a
    * verbatim port of {@code NodeStatus.semVerComparator} (source wasn't available to copy from
    * directly) — worth a spot-check against upstream's actual implementation if exact parity
    * matters for an edge case not covered by the documented examples.
    */
    private static boolean semanticVersionMatch(String stereotypeVersion, String requestedVersion) {
        if (stereotypeVersion == null || requestedVersion == null) {
            return Objects.equals(stereotypeVersion, requestedVersion);
        }
        String[] stereotypeParts = stereotypeVersion.split("\\.");
        String[] requestedParts = requestedVersion.split("\\.");
        int length = Math.min(stereotypeParts.length, requestedParts.length);
        for (int i = 0; i < length; i++) {
            if (!stereotypeParts[i].equals(requestedParts[i])) {
                return false;
            }
        }
        return true;
    }

    /**
    * Match extension capabilities, skipping those the RFC defines as configuration options: a
    * name with the {@code se:} prefix, or ending in {@code options}, {@code Options}, {@code
    * loggingPrefs}, or {@code debuggerAddress} — deliberately without upstream {@code
    * DefaultSlotMatcher}'s broader vendor-prefix exclusion list; see the class javadoc for why.
    * Everything else is compared for equality.
    * <p>
    * Unlike upstream, this iterates the <b>requested</b> capability names rather than the
    * stereotype's declared names, so a requested identity-relevant capability the stereotype
    * doesn't declare at all is correctly treated as a mismatch instead of silently skipped. This
    * requires guarding the stereotype's value against {@code null} before comparing (upstream's
    * String branch calls {@code .toString()} on the stereotype's value unconditionally, which is
    * safe there only because the stereotype is guaranteed to declare the name being iterated —
    * that guarantee no longer holds once the iteration is request-driven).
    */
    private Boolean extensionCapabilitiesMatch(Capabilities stereotype, Capabilities capabilities) {
        return capabilities.getCapabilityNames().stream()
                .filter(name -> name.contains(":"))
                .filter(name -> !name.endsWith("options"))
                .filter(name -> !name.endsWith("Options"))
                .filter(name -> !name.endsWith("loggingPrefs"))
                .filter(name -> !name.endsWith("debuggerAddress"))
                .filter(name -> !name.startsWith("se:"))
                .filter(name -> capabilities.getCapability(name) != null)
                .map(name -> {
                    Object stereotypeValue = stereotype.getCapability(name);
                    Object capabilityValue = capabilities.getCapability(name);
                    if (capabilityValue instanceof String) {
                        return stereotypeValue != null
                                && ((String) stereotypeValue).equalsIgnoreCase((String) capabilityValue);
                    }
                    return Objects.equals(stereotypeValue, capabilityValue);
                }).reduce(Boolean::logicalAnd).orElse(true);
    }
}
