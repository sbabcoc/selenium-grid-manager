package com.nordstrom.automation.selenium.sidecar;

/**
 * Strategy for allocating ports for local Selenium Grid hub instances.
 * <p>
 * The default implementation is {@code DefaultGridPortAllocationStrategy}, whose class name
 * is registered via {@code getDefaults()} in the appropriate {@link com.nordstrom.automation.selenium.SeleniumConfig}
 * subclass in {@code selenium-foundation}. Custom implementations must be no-arg-constructible
 * and are activated by setting {@link com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings#GRID_PORT_ALLOCATOR}
 * in {@code settings.properties} or as a system property.
 *
 * @since 36.0.0
 */
public interface GridPortAllocationStrategy {

    /**
     * Allocate a bundle of three ports for a Selenium 4 Grid hub — the hub port and the
     * event bus publisher and subscriber ports. All three ports are verified to be
     * simultaneously available before being returned.
     *
     * @param seed port number at which to begin searching
     * @return {@link GridHubPortAllocator.GridPorts} containing the allocated port bundle
     * @throws RuntimeException if no free port bundle is found
     */
    GridHubPortAllocator.GridPorts allocateBundle(int seed);

    /**
     * Allocate a single port for a Selenium 3 Grid hub.
     *
     * @param seed port number at which to begin searching
     * @return allocated hub port number
     * @throws RuntimeException if no free hub port is found
     */
    int allocateHub(int seed);
}
