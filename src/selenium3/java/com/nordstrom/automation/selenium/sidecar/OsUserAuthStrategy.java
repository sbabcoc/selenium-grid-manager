package com.nordstrom.automation.selenium.sidecar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;

/**
 * {@link SidecarAuthStrategy} implementation that authenticates based on OS user identity.
 * <p>
 * <b>NOTE</b>: This Selenium 3 implementation always returns {@code false} since
 * {@code ProcessHandle} is not available in Java 8. Configure
 * {@link SeleniumSettings#SIDECAR_STOP_TOKEN} to protect sensitive sidecar operations
 * in Selenium 3 environments.
 *
 * @since 36.0.0
 */
public class OsUserAuthStrategy implements SidecarAuthStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsUserAuthStrategy.class);

    /**
     * Constructor for OS user auth strategy.
     * <p>
     * Logs a warning since OS user authentication is unavailable in Java 8 environments.
     */
    public OsUserAuthStrategy() {
        LOGGER.warn("OS user authentication is not available in Java 8 environments — " +
                "configure {} to protect sensitive sidecar operations",
                SeleniumSettings.SIDECAR_STOP_TOKEN.key());
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always returns {@code false} — OS user authentication is not available in Java 8.
     */
    @Override
    public boolean isAuthenticated(HttpServletRequest req) {
        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * Always returns {@code false} — OS user authentication is not available in Java 8.
     */
    @Override
    public boolean isAuthorized(HttpServletRequest req, HttpServletResponse resp) {
        return false;
    }
}
