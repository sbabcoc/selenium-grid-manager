package com.nordstrom.automation.selenium.core.registration;

import com.nordstrom.automation.selenium.core.LocalGridServer;

/**
 * Strategy for registering a grid server with the sidecar registry
 * after it has been successfully started.
 * <p>
 * All implementations live in {@code selenium-grid-manager}. The interface
 * and all implementations are version-agnostic or version-specific within
 * that project.
 * <p>
 * Implementations are called only in the launching JVM and are never
 * on the sidecar classpath.
 *
 * @since 36.0.0
 */
public interface RegistrationStrategy {
    /**
     * Register the specified server with the sidecar registry.
     *
     * @param server the {@link LocalGridServer} that has been started
     * @param process the {@link Process} in which the server is running
     */
    void register(LocalGridServer server, Process process);
}
