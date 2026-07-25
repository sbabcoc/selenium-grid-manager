package com.nordstrom.automation.selenium.sidecar.servlet;

import java.io.IOException;
import java.net.URL;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nordstrom.automation.selenium.sidecar.MonitoredGridRegistry;

/**
 * Servlet that handles monitored grid registration requests.
 * <p>
 * Accepts POST requests with {@code hubUrl} and {@code apiVersion} parameters.
 * No authorization is required — monitoring is a passive, read-visibility-only
 * operation with no ability to affect a hub's lifecycle.
 *
 * @since [next-major]
 */
@WebServlet(urlPatterns = { SidecarPathName.MONITOR_PATH })
public class MonitorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String hubUrlParam = req.getParameter("hubUrl");
        String apiVersionParam = req.getParameter("apiVersion");
        if (hubUrlParam == null || apiVersionParam == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        URL hubUrl = new URL(hubUrlParam);
        int apiVersion = Integer.parseInt(apiVersionParam);
        MonitoredGridRegistry.getInstance().add(hubUrl, apiVersion);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
