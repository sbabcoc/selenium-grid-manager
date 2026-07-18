package com.nordstrom.automation.selenium.sidecar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;

/**
 * Static facade for hub port allocation.
 * <p>
 * Resolves the configured {@link GridPortAllocationStrategy} implementation at class load time
 * and delegates all allocation calls to it. The default seed port is 4444.
 * <p>
 * The strategy implementation is resolved via
 * {@link SeleniumSettings#GRID_PORT_ALLOCATOR}, whose default class name is registered
 * via {@code getDefaults()} in the appropriate {@link SeleniumConfig} subclass.
 *
 * @since 36.0.0
 */
public class GridHubPortAllocator {

    private static final Logger LOGGER = LoggerFactory.getLogger(GridHubPortAllocator.class);
    private static final int PORT_SEED = 4444;

    private static final GridPortAllocationStrategy strategy = resolveStrategy();

    private GridHubPortAllocator() {
        throw new AssertionError("GridHubPortAllocator is a static utility class that cannot be instantiated");
    }

    /**
     * Allocate a bundle of three ports for a Selenium 4 Grid hub starting from the default seed.
     *
     * @return {@link GridPorts} containing the allocated hub, publisher, and subscriber ports
     */
    public static GridPorts allocate() {
        return strategy.allocateBundle(PORT_SEED);
    }

    /**
     * Allocate a bundle of three ports for a Selenium 4 Grid hub starting from the specified seed.
     *
     * @param seed port number at which to begin searching
     * @return {@link GridPorts} containing the allocated hub, publisher, and subscriber ports
     */
    public static GridPorts allocate(int seed) {
        return strategy.allocateBundle(seed);
    }

    /**
     * Allocate a single port for a Selenium 3 Grid hub starting from the default seed.
     *
     * @return allocated hub port number
     */
    public static int allocateHub() {
        return strategy.allocateHub(PORT_SEED);
    }

    /**
     * Allocate a single port for a Selenium 3 Grid hub starting from the specified seed.
     *
     * @param seed port number at which to begin searching
     * @return allocated hub port number
     */
    public static int allocateHub(int seed) {
        return strategy.allocateHub(seed);
    }

    /**
     * Resolve the configured {@link GridPortAllocationStrategy} implementation.
     *
     * @return resolved {@link GridPortAllocationStrategy} instance
     */
    private static GridPortAllocationStrategy resolveStrategy() {
        String className = SeleniumConfig.getConfig()
                .getString(SeleniumSettings.GRID_PORT_ALLOCATOR.key());
        try {
            GridPortAllocationStrategy instance = (GridPortAllocationStrategy)
                    Class.forName(className).getDeclaredConstructor().newInstance();
            LOGGER.debug("Resolved GridPortAllocationStrategy: {}", className);
            return instance;
        } catch (Exception e) {
            throw new RuntimeException("Failed instantiating GridPortAllocationStrategy: " + className, e);
        }
    }

    /**
     * Immutable value class holding an allocated Selenium 4 Grid port bundle.
     *
     * @since 36.0.0
     */
    public static class GridPorts {

        /** Hub port. */
        public final int hubPort;

        /** Event bus publisher port. */
        public final int eventBusPubPort;

        /** Event bus subscriber port. */
        public final int eventBusSubPort;

        /**
         * Constructor for grid port bundle.
         *
         * @param hubPort hub port
         * @param eventBusPubPort event bus publisher port
         * @param eventBusSubPort event bus subscriber port
         */
        public GridPorts(int hubPort, int eventBusPubPort, int eventBusSubPort) {
            this.hubPort = hubPort;
            this.eventBusPubPort = eventBusPubPort;
            this.eventBusSubPort = eventBusSubPort;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public String toString() {
            return "hub=" + hubPort + ", pub=" + eventBusPubPort + ", sub=" + eventBusSubPort;
        }
    }
}
