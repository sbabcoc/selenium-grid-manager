package com.nordstrom.automation.selenium.sidecar.servlet;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openqa.selenium.json.Json;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.sidecar.DefaultSidecarAuthStrategy;
import com.nordstrom.automation.selenium.sidecar.GridRegistry;
import com.nordstrom.automation.selenium.sidecar.GridServerRegistration;
import com.nordstrom.automation.selenium.sidecar.ShutdownMode;
import com.nordstrom.automation.selenium.sidecar.SidecarAuthStrategy;

/**
 * Servlet that handles device provisioning requests for Appium/UiAutomator2 test execution.
 * <p>
 * Accepts POST requests with a {@code hubPort} parameter and a {@code driverName} parameter
 * (e.g. {@code "UiAutomator2"}). Resolves the active Android device (matching Appium's own
 * first-in-list selection when more than one is connected via {@code adb}), verifies that the
 * UiAutomator2 server, server-test, and settings packages are installed on the device, and —
 * if not — installs them by acquiring and immediately releasing a throwaway Appium session
 * with {@code skipServerInstallation}/{@code skipDeviceInitialization} forced to {@code false}.
 * <p>
 * Intended to be called once per test suite, before any real sessions are acquired, so that
 * the {@code APPIUM_DEFAULT_CAPS} setting's skip capabilities can be safely applied as
 * launch-time defaults without incurring per-session install overhead.
 * <p>
 * This operation can install/update APKs on the target device, so it requires authorization
 * via {@link SidecarAuthStrategy}, consistent with other state-mutating sidecar servlets
 * (e.g. {@code StopServlet}).
 *
 * @since TBD
 */
@WebServlet(urlPatterns = { SidecarPathName.PROVISION_PATH })
public class ProvisionServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(ProvisionServlet.class);
    private static final SidecarAuthStrategy AUTH = new DefaultSidecarAuthStrategy();

    private static final Pattern DEVICE_LINE = Pattern.compile("^(\\S+)\\s+device$", Pattern.MULTILINE);

    private static final List<String> REQUIRED_PACKAGES = Arrays.asList(
            "io.appium.uiautomator2.server",
            "io.appium.uiautomator2.server.test",
            "io.appium.settings");

    private static final int SYSTEM_PORT = 8200;

    // guards concurrent suite-startup calls against redundant/racing installs
    private static final Object PROVISION_LOCK = new Object();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!AUTH.isAuthorized(req, resp)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String hubPortParam = req.getParameter("hubPort");
        String driverName = req.getParameter("driverName");
        if (hubPortParam == null || driverName == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        int hubPort = Integer.parseInt(hubPortParam);

        synchronized (PROVISION_LOCK) {
            String serial;
            String appiumSessionUrl;
            try {
                serial = resolveTargetDevice();
                appiumSessionUrl = resolveAppiumSessionUrl(hubPort, driverName);
            } catch (ProvisioningException e) {
                resp.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
                resp.setContentType("text/plain");
                resp.getWriter().println(e.getMessage());
                return;
            }

            boolean ready = verifyAndProvision(serial, appiumSessionUrl);
            if (ready) {
                resp.setStatus(HttpServletResponse.SC_OK);
            } else {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("text/plain");
                resp.getWriter().println("Provisioning failed for device: " + serial);
            }
        }
    }

    /**
     * Resolve the ADB target device, matching Appium's own first-in-list selection
     * when more than one device is connected.
     *
     * @return serial of the target device
     * @throws ProvisioningException if no devices are connected
     */
    private String resolveTargetDevice() throws IOException, ProvisioningException {
        List<String> devices = getConnectedDevices();
        if (devices.isEmpty()) {
            throw new ProvisioningException("No devices found");
        }
        String target = devices.get(0);
        if (devices.size() > 1) {
            LOGGER.warn("Multiple devices detected; provisioning '{}' (first in list, matching Appium selection)",
                    target);
        }
        return target;
    }

    private List<String> getConnectedDevices() throws IOException {
        List<String> result = new ArrayList<>();
        Process proc = new ProcessBuilder("adb", "devices").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                Matcher m = DEVICE_LINE.matcher(line);
                if (m.matches()) {
                    result.add(m.group(1));
                }
            }
        }
        return result;
    }

    /**
     * Resolve the Appium session endpoint for the specified hub port and driver name, via
     * the grid registry's PM2-registered servers.
     *
     * @param hubPort hub port of the grid collection to query
     * @param driverName driver name to filter by (e.g. "UiAutomator2")
     * @return Appium {@code /session} endpoint URL for the resolved server
     * @throws ProvisioningException if no matching PM2 server is registered
     */
    private String resolveAppiumSessionUrl(int hubPort, String driverName) throws ProvisioningException {
        List<GridServerRegistration> matches = GridRegistry.getInstance()
                .getServersByModeAndDriver(hubPort, ShutdownMode.PM2, driverName);

        if (matches.isEmpty()) {
            throw new ProvisioningException(
                    "No pm2-registered Appium server for driver '" + driverName + "' on hub port " + hubPort);
        }
        if (matches.size() > 1) {
            LOGGER.warn("Multiple pm2 servers for driver '{}' on hub port {}; using first", driverName, hubPort);
        }
        return matches.get(0).getServerUrl().toString() + "/session";
    }

    /**
     * Check whether the device already has the required UiAutomator2 packages installed;
     * install them via a throwaway Appium session if not.
     *
     * @param serial ADB serial of the target device
     * @param appiumSessionUrl Appium {@code /session} endpoint to use for the install fallback
     * @return {@code true} once the device is confirmed ready
     */
    private boolean verifyAndProvision(String serial, String appiumSessionUrl) {
        if (isProvisioned(serial)) {
            LOGGER.info("Device '{}' already provisioned", serial);
            return true;
        }
        LOGGER.info("Device '{}' not fully provisioned — forcing install via Appium session", serial);
        return installUiAutomator2(serial, appiumSessionUrl);
    }

    private boolean isProvisioned(String serial) {
        try {
            Set<String> installed = getInstalledPackages(serial);
            return installed.containsAll(REQUIRED_PACKAGES);
        } catch (IOException e) {
            LOGGER.warn("Failed querying installed packages for device '{}'", serial, e);
            return false;
        }
    }

    private Set<String> getInstalledPackages(String serial) throws IOException {
        Set<String> result = new HashSet<>();
        Process proc = new ProcessBuilder("adb", "-s", serial, "shell", "pm", "list", "packages").start();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(proc.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // lines are formatted "package:some.package.name"
                if (line.startsWith("package:")) {
                    result.add(line.substring("package:".length()).trim());
                }
            }
        }
        return result;
    }

    /**
     * Install the UiAutomator2 server/settings packages by acquiring and immediately releasing
     * a throwaway Appium session with install/init skip flags forced off.
     *
     * @param serial ADB serial of the target device
     * @param appiumSessionUrl Appium {@code /session} endpoint to use
     * @return {@code true} if the session was created successfully
     */
    @SuppressWarnings("unchecked")
    private boolean installUiAutomator2(String serial, String appiumSessionUrl) {
        Map<String, Object> alwaysMatch = new LinkedHashMap<>();
        alwaysMatch.put("platformName", "Android");
        alwaysMatch.put("appium:automationName", "UiAutomator2");
        alwaysMatch.put("appium:udid", serial);
        alwaysMatch.put("appium:systemPort", SYSTEM_PORT);
        alwaysMatch.put("appium:appPackage", "com.android.settings");
        alwaysMatch.put("appium:appActivity", ".Settings");
        alwaysMatch.put("appium:skipServerInstallation", false);
        alwaysMatch.put("appium:skipDeviceInitialization", false);

        Map<String, Object> capabilities = new LinkedHashMap<>();
        capabilities.put("alwaysMatch", alwaysMatch);

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("capabilities", capabilities);

        String sessionId = null;
        try {
            SeleniumConfig config = SeleniumConfig.getConfig();
            String requestBody = config.toJson(payload);
            String response = postJson(appiumSessionUrl, requestBody);

            Map<String, Object> parsed = config.fromJson(response, MAP_TYPE);
            sessionId = extractSessionId(parsed);

            if (sessionId == null) {
                LOGGER.error("Provisioning failed for device '{}': {}", serial, response);
                return false;
            }
            LOGGER.info("Provisioning session '{}' established for device '{}'", sessionId, serial);
            return true;
        } catch (IOException e) {
            LOGGER.error("Provisioning request failed for device '{}'", serial, e);
            return false;
        } finally {
            if (sessionId != null) {
                deleteQuietly(appiumSessionUrl + "/" + sessionId);
            }
        }
    }

    // org.openqa.selenium.json.Json.MAP_TYPE — present identically in both Selenium 3 and 4
    private static final Type MAP_TYPE = Json.MAP_TYPE;

    private String postJson(String url, String payload) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
        conn.setRequestMethod("POST");
        conn.setRequestProperty("Content-Type", "application/json");
        conn.setDoOutput(true);
        try (OutputStream os = conn.getOutputStream()) {
            os.write(payload.getBytes(StandardCharsets.UTF_8));
        }
        try (InputStream is = (conn.getResponseCode() < 400) ? conn.getInputStream() : conn.getErrorStream();
                BufferedReader reader = new BufferedReader(new InputStreamReader(is, StandardCharsets.UTF_8))) {
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            return sb.toString();
        }
    }

    private void deleteQuietly(String url) {
        try {
            HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setRequestMethod("DELETE");
            conn.getResponseCode(); // force execution
        } catch (IOException e) {
            LOGGER.warn("Failed to tear down provisioning session at {}", url, e);
        }
    }

    @SuppressWarnings("unchecked")
    private String extractSessionId(Map<String, Object> parsed) {
        Object value = parsed.get("value");
        if (value instanceof Map) {
            Object sessionId = ((Map<String, Object>) value).get("sessionId");
            return sessionId != null ? sessionId.toString() : null;
        }
        return null;
    }

    private static class ProvisioningException extends Exception {
        private static final long serialVersionUID = 1L;
        ProvisioningException(String message) {
            super(message);
        }
    }
}
