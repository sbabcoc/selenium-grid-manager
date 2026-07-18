package com.nordstrom.automation.selenium.servlet;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.openqa.grid.internal.RemoteProxy;
import org.openqa.grid.web.servlet.RegistryBasedServlet;
import org.openqa.selenium.json.Json;

/**
 * This servlet provides a JSON list of proxy URLs for all nodes registered
 * with the local Selenium 3 Grid hub.
 * <p>
 * This servlet is added to the hub configuration unconditionally, providing
 * a stable programmatic alternative to scraping the Grid console HTML.
 * It is the hub-side counterpart to the {@code LifecycleServlet} added
 * unconditionally to node configurations.
 * <p>
 * Endpoint: {@code GET /grid/admin/ProxyListServlet}
 * <p>
 * Response: JSON array of proxy URL objects:
 * <pre>
 * [{"id": "http://10.0.0.31:8658"}, {"id": "http://10.0.0.31:35909"}]
 * </pre>
 *
 * @since 36.0.0
 */
public class ProxyListServlet extends RegistryBasedServlet {

    private static final long serialVersionUID = 1L;

    /**
     * Constructor for {@code ProxyListServlet}.
     * The registry is resolved from the servlet context at first access.
     */
    public ProxyListServlet() {
        super(null);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        List<Map<String, String>> proxies = new ArrayList<>();
        for (RemoteProxy proxy : getRegistry().getAllProxies()) {
            Map<String, String> entry = new HashMap<>();
            entry.put("id", proxy.getRemoteHost().toString());
            proxies.add(entry);
        }
        resp.setStatus(HttpServletResponse.SC_OK);
        resp.setContentType("application/json");
        resp.getWriter().println(new Json().toJson(proxies));
    }
}
