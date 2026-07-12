package com.nordstrom.automation.selenium.sidecar;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.core.GridServer;

/**
 * Central registry for all grid server registrations managed by the sidecar.
 * <p>
 * Registrations are grouped by hub port — all servers belonging to the same grid
 * collection share the same hub port key. Shutdown proceeds from outer to inner:
 * PM2-managed Appium servers first, then non-hub PID/LIFECYCLE servers, then the hub.
 * <p>
 * This class is a static singleton accessed by sidecar servlets via {@link #getInstance()}.
 *
 * @since [next-major]
 */
public class GridRegistry {

    private static final Logger LOGGER = LoggerFactory.getLogger(GridRegistry.class);

    private static final GridRegistry INSTANCE = new GridRegistry();

    private final Map<Integer, List<GridServerRegistration>> registrations =
            new ConcurrentHashMap<>();

    private final GridInstanceScanner scanner;

    /**
     * Private constructor — use {@link #getInstance()}.
     */
    private GridRegistry() {
        this.scanner = new GridInstanceScanner(registrations.keySet());
    }

    /**
     * Get the singleton {@link GridRegistry} instance.
     *
     * @return singleton {@link GridRegistry} instance
     */
    public static GridRegistry getInstance() {
        return INSTANCE;
    }

    /**
     * Register the specified grid server with this registry.
     * <p>
     * <b>NOTE</b>: If the registration carries a PID and the sidecar is running on Java 8,
     * a warning is logged indicating that PID-based shutdown is unavailable and best-effort
     * command-line termination will be attempted instead.
     *
     * @param registration {@link GridServerRegistration} to register
     */
    public void register(GridServerRegistration registration) {
        if (registration.getPid() != null && !isJava9OrLater()) {
            LOGGER.warn("PID-based shutdown is not available in Java 8 environments — " +
                    "best-effort command-line termination will be attempted for server at {}",
                    registration.getServerUrl());
        }
        synchronized (getOrCreateList(registration.getHubPort())) {
            getOrCreateList(registration.getHubPort()).add(registration);
        }
        LOGGER.debug("Registered {} server at {} for grid collection on port {}",
                registration.isHub() ? "hub" : "node/relay/appium",
                registration.getServerUrl(), registration.getHubPort());
    }

    /**
     * Deregister all servers belonging to the grid collection on the specified hub port.
     *
     * @param hubPort hub port of the grid collection to deregister
     */
    public void deregister(int hubPort) {
        registrations.remove(hubPort);
        LOGGER.debug("Deregistered grid collection on port {}", hubPort);
    }

    /**
     * Shut down all servers belonging to the grid collection on the specified hub port.
     * Servers are shut down in order: PM2-managed Appium first, then non-hub servers,
     * then the hub.
     *
     * @param hubPort hub port of the grid collection to shut down
     */
    public void shutdown(int hubPort) {
        List<GridServerRegistration> servers;
        synchronized (this) {
            servers = registrations.remove(hubPort);
        }
        if (servers != null) {
            LOGGER.debug("Shutting down grid collection on port {}", hubPort);
            sortForShutdown(servers).forEach(this::shutdownServer);
        }
    }

    /**
     * Shut down all managed grid collections and stop the background scanner.
     */
    public void shutdownAll() {
        LOGGER.debug("Shutting down all grid collections");
        new HashSet<>(registrations.keySet()).forEach(this::shutdown);
        scanner.shutdown();
    }

    /**
     * Get the set of hub ports currently managed by this registry.
     *
     * @return unmodifiable set of managed hub ports
     */
    public Set<Integer> getManagedPorts() {
        return Collections.unmodifiableSet(registrations.keySet());
    }

    /**
     * Get the list of hub statuses for all managed grid collections.
     *
     * @return list of {@link HubStatus} objects for all managed hubs
     */
    public List<HubStatus> getHubStatuses() {
        List<HubStatus> statuses = new ArrayList<>();
        for (List<GridServerRegistration> servers : registrations.values()) {
            synchronized (servers) {
                for (GridServerRegistration r : servers) {
                    if (r.isHub()) {
                        boolean active = GridServer.isHubActive(r.getServerUrl());
                        HubStatus status = HubStatus.managed(r.getServerUrl(), r.getApiVersion(),
                                active, r.getPubPort(), r.getSubPort());
                        if (status != null) statuses.add(status);
                    }
                }
            }
        }
        return statuses;
    }

    /**
     * Get the {@link GridInstanceScanner} owned by this registry.
     *
     * @return {@link GridInstanceScanner} instance
     */
    public GridInstanceScanner getScanner() {
        return scanner;
    }

    /**
     * Sort the specified list of registrations for shutdown order:
     * PM2-managed Appium first, then non-hub servers, then the hub.
     *
     * @param servers list of registrations to sort
     * @return sorted list
     */
    private List<GridServerRegistration> sortForShutdown(List<GridServerRegistration> servers) {
        List<GridServerRegistration> sorted = new ArrayList<>(servers);
        sorted.sort(Comparator
                .comparingInt((GridServerRegistration r) -> r.getShutdownMode() == ShutdownMode.PM2 ? 0 : 1)
                .thenComparingInt(r -> r.isHub() ? 1 : 0));
        return sorted;
    }

    /**
     * Shut down the specified server using the appropriate shutdown strategy.
     *
     * @param registration {@link GridServerRegistration} of the server to shut down
     */
    private void shutdownServer(GridServerRegistration registration) {
        try {
            strategyFor(registration).shutdown(registration);
            LOGGER.debug("Shut down {} server at {} for grid on port {}",
                    registration.isHub() ? "hub" : "node/relay/appium",
                    registration.getServerUrl(), registration.getHubPort());
        } catch (Exception e) {
            LOGGER.warn("Failed shutting down server at {}: {}",
                    registration.getServerUrl(), e.getMessage());
        }
    }

    /**
     * Select the appropriate shutdown strategy for the specified registration.
     *
     * @param registration {@link GridServerRegistration} of the server to shut down
     * @return appropriate shutdown strategy
     */
    private ShutdownStrategy strategyFor(GridServerRegistration registration) {
        switch (registration.getShutdownMode()) {
            case PM2:       return new PM2ShutdownStrategy();
            case LIFECYCLE: return new LifecycleShutdownStrategy();
            case PID:
            default:        return new PidShutdownStrategy();
        }
    }

    /**
     * Get or create the registration list for the specified hub port.
     *
     * @param hubPort hub port key
     * @return registration list for the specified hub port
     */
    private List<GridServerRegistration> getOrCreateList(int hubPort) {
        return registrations.computeIfAbsent(hubPort, k -> new ArrayList<>());
    }

    /**
     * Determine if the sidecar is running on Java 9 or later.
     *
     * @return {@code true} if running on Java 9 or later; otherwise {@code false}
     */
    private static boolean isJava9OrLater() {
        try {
            Runtime.class.getMethod("version");
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
