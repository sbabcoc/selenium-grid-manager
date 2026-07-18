package com.nordstrom.automation.selenium.sidecar.servlet;

import java.io.IOException;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.sidecar.DefaultSidecarAuthStrategy;
import com.nordstrom.automation.selenium.sidecar.GridRegistry;
import com.nordstrom.automation.selenium.sidecar.SidecarAuthStrategy;

/**
 * Servlet that handles sidecar stop requests.
 * <p>
 * Shuts down all managed grid collections and then stops the sidecar JVM.
 * Requires authorization via {@link SidecarAuthStrategy}.
 *
 * @since 36.0.0
 */
@WebServlet(urlPatterns = { "/grid/control/stop" })
public class StopServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;
    private static final Logger LOGGER = LoggerFactory.getLogger(StopServlet.class);
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
        GridRegistry.getInstance().shutdownAll();
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("text/plain");
        resp.getWriter().println("Sidecar shutting down...");
        resp.flushBuffer();
        LOGGER.info("Sidecar stop requested — shutting down");
        new Thread(() -> {
            try { Thread.sleep(500); } catch (InterruptedException ignored) {}
            System.exit(0);
        }, "sidecar-stop").start();
    }
}
