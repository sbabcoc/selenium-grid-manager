// Licensed to the Software Freedom Conservancy (SFC) under one
// or more contributor license agreements.  See the NOTICE file
// distributed with this work for additional information
// regarding copyright ownership.  The SFC licenses this file
// to you under the Apache License, Version 2.0 (the
// "License"); you may not use this file except in compliance
// with the License.  You may obtain a copy of the License at
//
//   http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing,
// software distributed under the License is distributed on an
// "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
// KIND, either express or implied.  See the License for the
// specific language governing permissions and limitations
// under the License.

package com.nordstrom.automation.selenium.core;

import static org.openqa.selenium.remote.BrowserType.SAFARI;
import static org.openqa.selenium.remote.CapabilityType.BROWSER_NAME;

import com.google.common.collect.ImmutableSet;

import org.openqa.grid.internal.utils.CapabilityMatcher;
import org.openqa.selenium.Platform;
import org.openqa.selenium.WebDriverException;
import org.openqa.selenium.remote.CapabilityType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;
import java.util.stream.Stream;

/**
 * Default (naive) implementation of the capability matcher.
 * <p>
 * The default capability matcher will look at all the key from the request do not start with _ and
 * will try to find a node that has at least those capabilities.
 */
public class FoundationSlotMatcher implements CapabilityMatcher {

    private static final String GRID_TOKEN = "_";

    interface Validator extends BiFunction<Map<String, Object>, Map<String, Object>, Boolean> {
    }

    private boolean anything(Object requested) {
        return requested == null || ImmutableSet.of("any", "", "*").contains(requested.toString().toLowerCase());
    }

    class PlatformValidator implements Validator {
        @Override
        @SuppressWarnings("deprecation")
        public Boolean apply(Map<String, Object> providedCapabilities, Map<String, Object> requestedCapabilities) {
            Object requested = Optional.ofNullable(requestedCapabilities.get(CapabilityType.PLATFORM))
                    .orElse(requestedCapabilities.get(CapabilityType.PLATFORM_NAME));
            if (anything(requested)) {
                return true;
            }
            Object provided = Optional.ofNullable(providedCapabilities.get(CapabilityType.PLATFORM))
                    .orElse(providedCapabilities.get(CapabilityType.PLATFORM_NAME));
            Platform requestedPlatform = extractPlatform(requested);
            if (requestedPlatform != null) {
                Platform providedPlatform = extractPlatform(provided);
                return providedPlatform != null && providedPlatform.is(requestedPlatform);
            }

            return provided != null && Objects.equals(requested.toString(), provided.toString());
        }
    }

    class AliasedPropertyValidator implements Validator {
        private String[] propertyAliases;

        AliasedPropertyValidator(String... propertyAliases) {
            this.propertyAliases = propertyAliases;
        }

        @Override
        public Boolean apply(Map<String, Object> providedCapabilities, Map<String, Object> requestedCapabilities) {
            Object requested = Stream.of(propertyAliases).map(requestedCapabilities::get).filter(Objects::nonNull)
                    .findFirst().orElse(null);

            if (anything(requested)) {
                return true;
            }

            Object provided = Stream.of(propertyAliases).map(providedCapabilities::get).filter(Objects::nonNull)
                    .findFirst().orElse(null);
            return Objects.equals(requested, provided);
        }
    }

    class BrowserVersionValidator implements Validator {
        @Override
        public Boolean apply(Map<String, Object> providedCapabilities, Map<String, Object> requestedCapabilities) {
            if ("htmlunit".equals(requestedCapabilities.get(BROWSER_NAME))) {
                return true;
            }

            Object requested = Optional.ofNullable(requestedCapabilities.get(CapabilityType.BROWSER_VERSION))
                    .orElse(requestedCapabilities.get(CapabilityType.VERSION));
            if (anything(requested)) {
                return true;
            }
            Object provided = Optional.ofNullable(providedCapabilities.get(CapabilityType.BROWSER_VERSION))
                    .orElse(providedCapabilities.get(CapabilityType.VERSION));
            Platform requestedPlatform = extractPlatform(requested);
            if (requestedPlatform != null) {
                Platform providedPlatform = extractPlatform(provided);
                return providedPlatform != null && providedPlatform.is(requestedPlatform);
            }

            return provided != null && semanticVersionMatch(provided.toString(), requested.toString());
        }
    }

    class SimplePropertyValidator implements Validator {
        private List<String> toConsider;

        SimplePropertyValidator(String... toConsider) {
            this.toConsider = Arrays.asList(toConsider);
        }

        @Override
        public Boolean apply(Map<String, Object> providedCapabilities, Map<String, Object> requestedCapabilities) {
            return requestedCapabilities.entrySet().stream().filter(entry -> !entry.getKey().startsWith(GRID_TOKEN))
                    .filter(entry -> toConsider.contains(entry.getKey())).filter(entry -> !anything(entry.getValue()))
                    .allMatch(entry -> entry.getValue().equals(providedCapabilities.get(entry.getKey())));
        }
    }

    /**
     * Validates {@code appium:automationName} unconditionally (no {@link #addToConsider(String)}
     * registration required), the same narrow fix applied to the Selenium 4 variant of this
     * class. Without this, nothing in the default validator chain inspects {@code
     * appium:automationName} or any other {@code :}-namespaced capability at all — so on a grid
     * with multiple Appium personalities that happen to share a platform family (e.g. an
     * HtmlUnit node and a Mac2 node both reporting {@code platform=MAC}), a request that never
     * specifies {@code browserName} can match either one, since every other default validator
     * short-circuits to {@code true} when the request doesn't mention what it checks. This is
     * request-driven the same way {@link SimplePropertyValidator} already is: if the provided
     * capabilities don't declare {@code appium:automationName} at all, {@code Map.get()} returns
     * {@code null}, and a non-null requested value can never {@code .equals()} that, so the
     * mismatch is caught correctly without any extra null-guarding.
     */
    class AutomationNameValidator implements Validator {
        private static final String AUTOMATION_NAME = "appium:automationName";

        @Override
        public Boolean apply(Map<String, Object> providedCapabilities, Map<String, Object> requestedCapabilities) {
            Object requested = requestedCapabilities.get(AUTOMATION_NAME);
            if (anything(requested)) {
                return true;
            }
            return requested.equals(providedCapabilities.get(AUTOMATION_NAME));
        }
    }

    class FirefoxSpecificValidator implements Validator {
        @Override
        public Boolean apply(Map<String, Object> providedCapabilities, Map<String, Object> requestedCapabilities) {
            if (!"firefox".equals(requestedCapabilities.get(BROWSER_NAME))) {
                return true;
            }

            if (requestedCapabilities.get("marionette") != null
                    && !Boolean.valueOf(requestedCapabilities.get("marionette").toString())) {
                return providedCapabilities.get("marionette") != null
                        && !Boolean.valueOf(providedCapabilities.get("marionette").toString());
            } else {
                return providedCapabilities.get("marionette") == null
                        || Boolean.valueOf(providedCapabilities.get("marionette").toString());
            }
        }
    }

    class SafariSpecificValidator implements Validator {
        static final String SAFARI_TECH_PREVIEW = "Safari Technology Preview";
        static final String AUTOMATIC_INSPECTION = "safari:automaticInspection";
        static final String AUTOMATIC_PROFILING = "safari:automaticProfiling";
        static final String TECHNOLOGY_PREVIEW = "technologyPreview";

        @Override
        public Boolean apply(Map<String, Object> providedCapabilities, Map<String, Object> requestedCapabilities) {
            if (!SAFARI.equals(getBrowserName(requestedCapabilities))
                    && !SAFARI_TECH_PREVIEW.equals(getBrowserName(requestedCapabilities))) {
                return true;
            }

            return getAutomaticInspection(requestedCapabilities) == getAutomaticInspection(providedCapabilities)
                    && getAutomaticProfiling(requestedCapabilities) == getAutomaticProfiling(providedCapabilities)
                    && getUseTechnologyPreview(requestedCapabilities) == getUseTechnologyPreview(providedCapabilities);
        }

        private String getBrowserName(Map<String, Object> capabilities) {
            return (String) capabilities.get(BROWSER_NAME);
        }

        private boolean getAutomaticInspection(Map<String, Object> capabilities) {
            return Boolean.TRUE.equals(capabilities.get(AUTOMATIC_INSPECTION));
        }

        private boolean getAutomaticProfiling(Map<String, Object> capabilities) {
            return Boolean.TRUE.equals(capabilities.get(AUTOMATIC_PROFILING));
        }

        private boolean getUseTechnologyPreview(Map<String, Object> capabilities) {
            return SAFARI_TECH_PREVIEW.equals(getBrowserName(capabilities))
                    || Boolean.TRUE.equals(capabilities.get(TECHNOLOGY_PREVIEW));
        }
    }

    private final List<Validator> validators = new ArrayList<>();
    {
        validators.addAll(Arrays.asList(new PlatformValidator(), new AliasedPropertyValidator(BROWSER_NAME, "browser"),
                new BrowserVersionValidator(), new SimplePropertyValidator(CapabilityType.APPLICATION_NAME),
                new AutomationNameValidator(), new FirefoxSpecificValidator(), new SafariSpecificValidator()));
    }

    public void addToConsider(String capabilityName) {
        validators.add(new SimplePropertyValidator(capabilityName));
    }

    public boolean matches(Map<String, Object> providedCapabilities, Map<String, Object> requestedCapabilities) {
        return providedCapabilities != null && requestedCapabilities != null
                && validators.stream().allMatch(v -> v.apply(providedCapabilities, requestedCapabilities));
    }

    private Platform extractPlatform(Object o) {
        if (o == null) {
            return null;
        }
        if (o instanceof Platform) {
            return (Platform) o;
        }
        try {
            return Platform.fromString(o.toString());
        } catch (WebDriverException ex) {
            return null;
        }
    }

    /**
     * Compare two browser version strings with dot-separated, prefix-based matching, the same
     * semantics used for browserVersion matching in the Selenium 4 variant of this class: a
     * less-specific version (e.g. "131") matches a more-specific one (e.g. "131.0.6778.85") as
     * long as every segment they share in common is identical, but "131.0.6778.95" does not
     * match "131.0.6778.85", since the fourth segment actually differs. Segments are compared as
     * strings, not numerically.
     * <p>
     * Plain String.split/equals — no external version-parsing library — so this compiles and
     * runs under Java 8.
     */
    private boolean semanticVersionMatch(String providedVersion, String requestedVersion) {
        if (providedVersion == null || requestedVersion == null) {
            return Objects.equals(providedVersion, requestedVersion);
        }
        String[] providedParts = providedVersion.split("\\.");
        String[] requestedParts = requestedVersion.split("\\.");
        int length = Math.min(providedParts.length, requestedParts.length);
        for (int i = 0; i < length; i++) {
            if (!providedParts[i].equals(requestedParts[i])) {
                return false;
            }
        }
        return true;
    }
}
