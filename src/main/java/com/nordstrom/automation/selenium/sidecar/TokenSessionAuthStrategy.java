package com.nordstrom.automation.selenium.sidecar;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;

/**
 * {@link SidecarAuthStrategy} implementation that authenticates via a session cookie
 * and authorizes via a configured stop token.
 * <p>
 * When a valid token is presented, a session cookie is established for the configured
 * TTL ({@link SeleniumSettings#SIDECAR_SESSION_TTL}), allowing subsequent requests
 * to be authorized without re-presenting the token.
 * <p>
 * If no stop token is configured, all requests are authorized unconditionally.
 *
 * @since [next-major]
 */
public class TokenSessionAuthStrategy implements SidecarAuthStrategy {

    private static final String SESSION_COOKIE_NAME = "sidecar-session";
    private static final String SESSION_COOKIE_PATH = "/grid/control";

    private final Map<String, Long> activeSessions = new ConcurrentHashMap<>();

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} if the request carries a valid session cookie.
     */
    @Override
    public boolean isAuthenticated(HttpServletRequest req) {
        Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (SESSION_COOKIE_NAME.equals(cookie.getName())
                        && isValidSession(cookie.getValue())) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} if the request is already authenticated, if no stop token
     * is configured, or if the request presents the correct stop token (establishing
     * a new session as a side effect).
     */
    @Override
    public boolean isAuthorized(HttpServletRequest req, HttpServletResponse resp) {
        if (isAuthenticated(req)) return true;

        String configuredToken = SeleniumConfig.getConfig()
                .getString(SeleniumSettings.SIDECAR_STOP_TOKEN.key());
        if (configuredToken == null || configuredToken.isEmpty()) return true;

        String providedToken = req.getParameter("token");
        if (configuredToken.equals(providedToken)) {
            establishSession(resp);
            return true;
        }

        return false;
    }

    /**
     * Determine if the specified session ID corresponds to a valid active session.
     *
     * @param sessionId session ID to check
     * @return {@code true} if the session is valid and not expired; otherwise {@code false}
     */
    private boolean isValidSession(String sessionId) {
        Long expiry = activeSessions.get(sessionId);
        if (expiry == null) return false;
        if (System.currentTimeMillis() > expiry) {
            activeSessions.remove(sessionId);
            return false;
        }
        return true;
    }

    /**
     * Establish a new session and set the session cookie on the response.
     *
     * @param resp outgoing {@link HttpServletResponse}
     */
    private void establishSession(HttpServletResponse resp) {
        long ttlMs = SeleniumConfig.getConfig()
                .getLong(SeleniumSettings.SIDECAR_SESSION_TTL.key()) * 60 * 1000;
        String sessionId = UUID.randomUUID().toString();
        activeSessions.put(sessionId, System.currentTimeMillis() + ttlMs);
        Cookie cookie = new Cookie(SESSION_COOKIE_NAME, sessionId);
        cookie.setHttpOnly(true);
        cookie.setMaxAge((int) (ttlMs / 1000));
        cookie.setPath(SESSION_COOKIE_PATH);
        resp.addCookie(cookie);
    }
}
