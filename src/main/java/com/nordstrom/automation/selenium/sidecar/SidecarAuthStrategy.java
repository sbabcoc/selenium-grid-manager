package com.nordstrom.automation.selenium.sidecar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Strategy for authorizing sensitive sidecar operations.
 * <p>
 * The default implementation is {@code DefaultSidecarAuthStrategy}, whose class name is
 * registered via {@code getDefaults()} in the appropriate
 * {@link com.nordstrom.automation.selenium.SeleniumConfig} subclass in
 * {@code selenium-foundation}. Custom implementations must be no-arg-constructible and
 * are activated by setting
 * {@link com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings#SIDECAR_AUTH_STRATEGY}
 * in {@code settings.properties} or as a system property.
 *
 * @since [next-major]
 */
public interface SidecarAuthStrategy {

    /**
     * Determine if the sender of the specified request is authenticated.
     *
     * @param req incoming {@link HttpServletRequest}
     * @return {@code true} if the sender is authenticated; otherwise {@code false}
     */
    boolean isAuthenticated(HttpServletRequest req);

    /**
     * Determine if the sender of the specified request is authorized to perform
     * the requested operation. Implementations may establish a session as a
     * side effect of successful authorization.
     *
     * @param req incoming {@link HttpServletRequest}
     * @param resp outgoing {@link HttpServletResponse}
     * @return {@code true} if the sender is authorized; otherwise {@code false}
     */
    boolean isAuthorized(HttpServletRequest req, HttpServletResponse resp);
}
