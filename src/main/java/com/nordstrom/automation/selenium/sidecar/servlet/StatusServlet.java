package com.nordstrom.automation.selenium.sidecar.servlet;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.sidecar.GridRegistry;
import com.nordstrom.automation.selenium.sidecar.GridScanResult;
import com.nordstrom.automation.selenium.utility.HostUtils;
import com.nordstrom.common.uri.UriUtils;

/**
 * Servlet that returns the current grid status as JSON.
 * <p>
 * Returns a {@link GridScanResult} containing the status of all managed and
 * discovered grid instances. If the {@code fineToothed} parameter is
 * {@code true}, requests a fine-toothed scan from the
 * {@link com.nordstrom.automation.selenium.sidecar.GridInstanceScanner}.
 *
 * @since [next-major]
 */
@WebServlet(urlPatterns = { "/grid/control/status" })
public class StatusServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if ("true".equals(req.getParameter("fineToothed"))) {
            GridRegistry.getInstance().getScanner().requestFineToothed();
        }

        String exampleSiteUrl = null;
        if (SeleniumConfig.getConfig().getBoolean(SeleniumSettings.SERVE_EXAMPLE_SITE.key())) {
            int port = SeleniumConfig.getConfig().getInt(SeleniumSettings.SIDECAR_PORT.key());
            exampleSiteUrl = UriUtils.makeBasicURI("http", HostUtils.getLocalHost(), port,
                    "/grid/admin/ExamplePageServlet").toString();
        }

        GridScanResult result = new GridScanResult(
                GridRegistry.getInstance().getHubStatuses(),
                GridRegistry.getInstance().getScanner().getDiscovered(),
                exampleSiteUrl);

        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().println(SeleniumConfig.getConfig().toJson(result));
    }
}
