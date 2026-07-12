package com.nordstrom.automation.selenium.sidecar.servlet;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nordstrom.automation.selenium.sidecar.GridRegistry;

/**
 * Servlet that handles grid collection deregistration requests.
 * <p>
 * Accepts POST requests with a {@code hubPort} parameter and removes the
 * corresponding grid collection from the {@link GridRegistry}.
 *
 * @since [next-major]
 */
@WebServlet(urlPatterns = { "/grid/control/deregister" })
public class DeregistrationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        int hubPort = Integer.parseInt(req.getParameter("hubPort"));
        GridRegistry.getInstance().deregister(hubPort);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
