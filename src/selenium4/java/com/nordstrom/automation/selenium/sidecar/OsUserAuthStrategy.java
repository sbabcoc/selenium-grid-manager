package com.nordstrom.automation.selenium.sidecar;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@link SidecarAuthStrategy} implementation that authenticates based on OS user identity.
 * <p>
 * A request is authenticated if it originates from the loopback interface AND carries
 * an {@code X-OS-User} header matching the OS user that owns the sidecar process.
 * This provides CLI-level authorization for tools running on the same machine as the sidecar.
 *
 * @since 36.0.0
 */
public class OsUserAuthStrategy implements SidecarAuthStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(OsUserAuthStrategy.class);
    private static final String OS_USER_HEADER = "X-OS-User";

    private final String sidecarOwner;

    /**
     * Constructor for OS user auth strategy.
     * Captures the OS user of the current sidecar process at construction time.
     */
    public OsUserAuthStrategy() {
        sidecarOwner = ProcessHandle.current().info().user().orElse(null);
        if (sidecarOwner == null) {
            LOGGER.warn("Unable to determine OS user for sidecar process — " +
                    "OS user authentication will not be available");
        } else {
            LOGGER.debug("OS user authentication initialized for user: {}", sidecarOwner);
        }
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} if the request originates from the loopback interface and
     * carries an {@code X-OS-User} header matching the sidecar process owner.
     */
    @Override
    public boolean isAuthenticated(HttpServletRequest req) {
        if (!isLoopback(req)) return false;
        String requestUser = req.getHeader(OS_USER_HEADER);
        return sidecarOwner != null && sidecarOwner.equals(requestUser);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAuthorized(HttpServletRequest req, HttpServletResponse resp) {
        return isAuthenticated(req);
    }

    /**
     * Determine if the specified request originates from the loopback interface.
     *
     * @param req incoming {@link HttpServletRequest}
     * @return {@code true} if the request originates from loopback; otherwise {@code false}
     */
    private boolean isLoopback(HttpServletRequest req) {
        String addr = req.getRemoteAddr();
        return "127.0.0.1".equals(addr) || "::1".equals(addr);
    }
}
