package com.nordstrom.automation.selenium.sidecar;

import java.util.List;

/**
 * Immutable data class representing the result of a grid instance scan.
 *
 * @since 36.0.0
 */
public class GridScanResult {

    private final List<HubStatus> managedGrids;
    private final List<HubStatus> discoveredGrids;
    private final String exampleSiteUrl;

    /**
     * Constructor for grid scan result.
     *
     * @param managedGrids list of managed hub statuses
     * @param discoveredGrids list of discovered hub statuses
     * @param exampleSiteUrl URL of the example page site; {@code null} if not active
     */
    public GridScanResult(List<HubStatus> managedGrids, List<HubStatus> discoveredGrids,
            String exampleSiteUrl) {
        this.managedGrids = managedGrids;
        this.discoveredGrids = discoveredGrids;
        this.exampleSiteUrl = exampleSiteUrl;
    }

    /** @return list of managed hub statuses */
    public List<HubStatus> getManagedGrids() { return managedGrids; }

    /** @return list of discovered hub statuses */
    public List<HubStatus> getDiscoveredGrids() { return discoveredGrids; }

    /** @return URL of the example page site; {@code null} if not active */
    public String getExampleSiteUrl() { return exampleSiteUrl; }
}
