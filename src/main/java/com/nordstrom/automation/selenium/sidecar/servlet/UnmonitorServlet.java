package com.nordstrom.automation.selenium.sidecar.servlet;

import java.io.IOException;
import java.net.URL;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nordstrom.automation.selenium.sidecar.DefaultSidecarAuthStrategy;
import com.nordstrom.automation.selenium.sidecar.MonitoredGridRegistry;
import com.nordstrom.automation.selenium.sidecar.SidecarAuthStrategy;

/**
 * Servlet that handles monitored grid removal requests.
 * <p>
 * Accepts POST requests with a {@code hubUrl} parameter. Requires authorization
 * via {@link SidecarAuthStrategy}.
 *
 * @since [next-major]
 */
@WebServlet(urlPatterns = { SidecarPathName.UNMONITOR_PATH })
public class UnmonitorServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final SidecarAuthStrategy AUTH = new DefaultSidecarAuthStrategy();

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        if (!AUTH.isAuthorized(req, resp)) {
            resp.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }
        String hubUrlParam = req.getParameter("hubUrl");
        if (hubUrlParam == null) {
            resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            return;
        }
        MonitoredGridRegistry.getInstance().remove(new URL(hubUrlParam));
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
