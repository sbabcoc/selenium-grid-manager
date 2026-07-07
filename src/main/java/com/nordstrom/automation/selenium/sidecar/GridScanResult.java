package com.nordstrom.automation.selenium.sidecar;

import java.util.List;

/**
 * Immutable data class representing the result of a grid instance scan.
 * <p>
 * Returned by the sidecar's {@code /grid/control/status} endpoint as a JSON object
 * containing lists of managed and discovered hub statuses.
 *
 * @since [next-major]
 */
public class GridScanResult {

    private final List<HubStatus> managedGrids;
    private final List<HubStatus> discoveredGrids;

    /**
     * Constructor for grid scan result.
     *
     * @param managedGrids list of managed hub statuses
     * @param discoveredGrids list of discovered hub statuses
     */
    public GridScanResult(List<HubStatus> managedGrids, List<HubStatus> discoveredGrids) {
        this.managedGrids = managedGrids;
        this.discoveredGrids = discoveredGrids;
    }

    /**
     * Get the list of managed hub statuses.
     * 
     * @return list of managed hub statuses
     */
    public List<HubStatus> getManagedGrids() { return managedGrids; }

    /**
     * Get the list of discovered hub statuses.
     * 
     * @return list of discovered hub statuses
     */
    public List<HubStatus> getDiscoveredGrids() { return discoveredGrids; }}
