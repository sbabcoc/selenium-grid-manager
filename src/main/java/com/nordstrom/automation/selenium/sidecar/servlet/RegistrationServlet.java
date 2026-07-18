package com.nordstrom.automation.selenium.sidecar.servlet;

import java.io.IOException;
import java.util.Map;
import java.util.stream.Collectors;

import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openqa.selenium.json.Json;

import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.sidecar.GridRegistry;
import com.nordstrom.automation.selenium.sidecar.GridServerRegistration;

/**
 * Servlet that handles grid server registration requests.
 * <p>
 * Accepts POST requests containing a JSON-serialized {@link GridServerRegistration}
 * and registers the server with the {@link GridRegistry}.
 *
 * @since 36.0.0
 */
@WebServlet(urlPatterns = { "/grid/control/register" })
public class RegistrationServlet extends HttpServlet {

    private static final long serialVersionUID = 1L;

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String body = req.getReader().lines().collect(Collectors.joining());
        Map<String, Object> map = SeleniumConfig.getConfig().fromJson(body, Json.MAP_TYPE);
        GridServerRegistration registration = GridServerRegistration.fromJson(map);
        GridRegistry.getInstance().register(registration);
        resp.setStatus(HttpServletResponse.SC_OK);
    }
}
