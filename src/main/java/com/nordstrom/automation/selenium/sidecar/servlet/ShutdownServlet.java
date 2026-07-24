package com.nordstrom.automation.selenium.sidecar.servlet;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nordstrom.automation.selenium.sidecar.DefaultSidecarAuthStrategy;
import com.nordstrom.automation.selenium.sidecar.GridRegistry;
import com.nordstrom.automation.selenium.sidecar.SidecarAuthStrategy;

/**
 * Servlet that handles grid collection shutdown requests.
 * <p>
 * Accepts POST requests with an optional {@code hubPort} parameter. If
 * {@code hubPort} is provided, shuts down that collection; otherwise shuts
 * down all managed collections. Requires authorization via
 * {@link SidecarAuthStrategy}.
 *
 * @since 36.0.0
 */
@WebServlet(urlPatterns = { SidecarPathName.SHUTDOWN_PATH })
public class ShutdownServlet extends HttpServlet {

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
        String hubPortParam = req.getParameter("hubPort");
        if (hubPortParam != null) {
            GridRegistry.getInstance().shutdown(Integer.parseInt(hubPortParam));
        } else {
            GridRegistry.getInstance().shutdownAll();
        }
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
