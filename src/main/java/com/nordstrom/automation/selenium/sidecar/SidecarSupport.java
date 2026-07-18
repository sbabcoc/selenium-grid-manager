package com.nordstrom.automation.selenium.sidecar;

import java.io.IOException;
import java.util.List;

/**
 * Version-specific support interface for the sidecar servlet container.
 * <p>
 * Implementations are registered via {@link java.util.ServiceLoader} and provide
 * the Jetty-backed servlet container lifecycle management appropriate for the
 * current Selenium API version. Discovered and used by {@link SidecarManager}.
 *
 * @since 36.0.0
 */
public interface SidecarSupport {

    /**
     * Start the sidecar servlet container on the specified port with the specified servlets.
     *
     * @param servletClasses fully-qualified names of servlet classes to register
     * @param port port on which the sidecar should listen
     * @throws IOException if the servlet container fails to start
     */
    void start(List<String> servletClasses, int port) throws IOException;

    /**
     * Await termination of the sidecar servlet container.
     * Blocks until the server stops.
     *
     * @throws InterruptedException if interrupted while waiting
     */
    void await() throws InterruptedException;

    /**
     * Determine if the sidecar servlet container is currently active.
     *
     * @return {@code true} if the sidecar is active; otherwise {@code false}
     */
    boolean isActive();
}
