package com.nordstrom.automation.selenium.sidecar.servlet;

import java.io.IOException;
import java.io.PrintWriter;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.JsUtility;
import com.nordstrom.automation.selenium.servlet.ExamplePagePathName;
import com.nordstrom.automation.selenium.sidecar.DefaultSidecarAuthStrategy;
import com.nordstrom.automation.selenium.sidecar.GridInstanceScanner;
import com.nordstrom.automation.selenium.sidecar.GridRegistry;
import com.nordstrom.automation.selenium.sidecar.HubStatus;
import com.nordstrom.automation.selenium.sidecar.SidecarAuthStrategy;
import com.nordstrom.automation.selenium.utility.HostUtils;
import com.nordstrom.common.uri.UriUtils;

/**
 * Servlet that serves the sidecar management console HTML page.
 * <p>
 * Displays managed and discovered grid instances, with controls for shutdown,
 * scanning, and stopping the sidecar.
 *
 * @since 36.0.0
 */
@WebServlet(urlPatterns = { SidecarPathName.CONSOLE_PATH })
public class ConsoleServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final long SCAN_COMPLETE_DISPLAY_MS = 8000;
    private static final SidecarAuthStrategy AUTH = new DefaultSidecarAuthStrategy();

    /** cached console page script */
    private String consoleScript;

    @Override
    public void init() throws ServletException {
        consoleScript = JsUtility.getScriptResource("console.js");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("true".equals(req.getParameter("fineToothed"))) {
            GridRegistry.getInstance().getScanner().requestFineToothed();
            resp.sendRedirect(SidecarPathName.CONSOLE_PATH);
            return;
        }

        GridRegistry registry = GridRegistry.getInstance();
        GridInstanceScanner scanner = registry.getScanner();
        List<HubStatus> managed = registry.getHubStatuses();
        List<HubStatus> discovered = scanner.getDiscovered();
        boolean authenticated = AUTH.isAuthenticated(req);
        boolean scanning = scanner.isScanning();
        int progress = scanner.scanProgress();
        long lastScan = scanner.lastScanTime();
        long now = System.currentTimeMillis();
        boolean recentScan = !scanning && lastScan > 0
                && (now - lastScan) < SCAN_COMPLETE_DISPLAY_MS;

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/html");
        PrintWriter out = resp.getWriter();

        out.println("<!DOCTYPE html><html><head><title>Selenium Grid Manager</title>");
        if (!scanning) {
            out.println("<meta http-equiv='refresh' content='5'/>");
        }
        out.println("<style>");
        out.println("body { font-family: sans-serif; margin: 2em; }");
        out.println("h2 { margin-top: 1.5em; }");
        out.println("table { border-collapse: collapse; width: 100%; margin-bottom: 1em; }");
        out.println("th, td { border: 1px solid #ccc; padding: 8px; text-align: left; }");
        out.println("th { background-color: #f0f0f0; }");
        out.println(".active { color: green; } .inactive { color: red; }");
        out.println(".actions { margin-top: 2em; display: flex; gap: 1em; "
                + "align-items: center; flex-wrap: wrap; }");
        out.println(".scan-btn { padding: 4px 10px; border: 1px solid #aaa; "
                + "border-radius: 3px; text-decoration: none; color: inherit; }");
        out.println(".scan-btn.fine { background-color: #f0f0f0; }");
        out.println(".scan-btn.disabled { opacity: 0.5; pointer-events: none; }");
        out.println(".progress-row { display: flex; align-items: center; gap: 8px; }");
        out.println(".scan-complete { color: green; }");
        out.println("</style></head><body>");
        out.println("<h1>Selenium Grid Manager</h1>");

        out.println("<h2>Managed Grid Collections</h2>");
        if (managed.isEmpty()) {
            out.println("<p>No managed grid collections.</p>");
        } else {
            renderGridTable(out, managed, true, authenticated);
        }
        
        // show example site link if active
        if (SeleniumConfig.getConfig().getBoolean(SeleniumSettings.SERVE_EXAMPLE_SITE.key())) {
            int port = SeleniumConfig.getConfig().getInt(SeleniumSettings.SIDECAR_PORT.key());
            String exampleUrl = UriUtils.makeBasicURI("http", HostUtils.getLocalHost(), port,
                    ExamplePagePathName.EXAMPLE_PAGE_PATH).toString();
            out.println("<p><strong>Example Site:</strong> <a href='" + exampleUrl
                    + "' target='_blank'>" + exampleUrl + "</a></p>");
        }
        
        out.println("<h2>Discovered Grid Instances</h2>");
        if (discovered.isEmpty()) {
            out.println("<p>No unmanaged grid instances discovered.</p>");
        } else {
            renderGridTable(out, discovered, false, authenticated);
        }

        out.println("<div class='actions'>");

        if (recentScan) {
            out.println("<span class='scan-complete'>&#x2713; Scan complete &mdash; "
                    + scanner.lastScanFound() + " found, "
                    + scanner.lastScanAdded() + " added, "
                    + scanner.lastScanRemoved() + " removed</span>");
        } else if (lastScan > 0 && !scanning) {
            out.println("<span>Last scan: "
                    + Instant.ofEpochMilli(lastScan).atZone(ZoneId.systemDefault())
                            .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"))
                    + "</span>");
        }

        if (scanning) {
            out.println("<div class='progress-row'><span>Scanning...</span>"
                    + "<progress value='" + progress + "' max='100' "
                    + "style='width:150px;'></progress>"
                    + "<span>" + progress + "%</span></div>");
        }

        String dc = scanning ? " disabled" : "";
        out.println("<a href='" + (scanning ? "#" : SidecarPathName.CONSOLE_PATH)
                + "' class='scan-btn" + dc + "' "
                + "title='Scan ports (stepped)'>Scan Now</a>");
        out.println("<a href='" + (scanning ? "#" : SidecarPathName.CONSOLE_PATH + "?fineToothed=true")
                + "' class='scan-btn fine" + dc + "' "
                + "title='Scan all ports'>Scan All Ports &#x1F50D;</a>");

        out.println("<form method='post' action='" + SidecarPathName.STOP_PATH + "' class='grid-action-form'>");
        if (requiresToken() && !authenticated) {
            out.println("Token: <input type='password' name='token'/> ");
        }
        out.println("<button type='submit'>Stop Sidecar</button></form>");
        out.println("</div>");
        out.println("<script>" + consoleScript + "</script>");
        out.println("</body></html>");
    }

    /**
     * Render a grid status table.
     *
     * @param out response writer
     * @param statuses list of hub statuses to render
     * @param managed {@code true} if these are managed instances (show shutdown button)
     * @param authenticated {@code true} if the current user is authenticated
     */
    private void renderGridTable(PrintWriter out, List<HubStatus> statuses,
            boolean managed, boolean authenticated) {
        out.println("<table><tr><th>Hub URL</th><th>API</th><th>Status</th>"
                + "<th>PUB Port</th><th>SUB Port</th><th>Console</th>");
        if (managed) out.println("<th>Action</th>");
        out.println("</tr>");

        for (HubStatus s : statuses) {
            String consoleUrl;
            try {
                consoleUrl = s.getApiVersion() == 4
                        ? UriUtils.uriForPath(s.getHubUrl(), "/ui/").toString()
                        : UriUtils.uriForPath(s.getHubUrl(), "/grid/console").toString();
            } catch (Exception e) {
                consoleUrl = s.getHubUrl().toString();
            }
            out.println("<tr>"
                    + "<td>" + s.getHubUrl() + "</td>"
                    + "<td>Selenium " + s.getApiVersion() + "</td>"
                    + "<td class='" + (s.isActive() ? "active" : "inactive") + "'>"
                    + (s.isActive() ? "Active" : "Inactive") + "</td>"
                    + "<td>" + (s.getPubPort() != null ? s.getPubPort() : "-") + "</td>"
                    + "<td>" + (s.getSubPort() != null ? s.getSubPort() : "-") + "</td>"
                    + "<td><a href='" + consoleUrl + "' target='_blank'>Open</a></td>");

            if (managed) {
                out.println("<td><form method='post' action='" + SidecarPathName.SHUTDOWN_PATH
                        + "' class='grid-action-form'><input type='hidden' name='hubPort' value='"
                        + s.getHubUrl().getPort() + "'/>");
                if (requiresToken() && !authenticated) {
                    out.println("Token: <input type='password' name='token'/> ");
                }
                out.println("<button type='submit'>Shutdown</button></form></td>");
            }
            out.println("</tr>");
        }
        out.println("</table>");
    }

    /**
     * Determine if a stop token is configured.
     *
     * @return {@code true} if a stop token is configured; otherwise {@code false}
     */
    private boolean requiresToken() {
        String token = SeleniumConfig.getConfig()
                .getString(SeleniumSettings.SIDECAR_STOP_TOKEN.key());
        return token != null && !token.isEmpty();
    }
}
