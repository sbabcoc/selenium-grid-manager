package com.nordstrom.automation.selenium.sidecar;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.nordstrom.automation.selenium.AbstractSeleniumConfig.SeleniumSettings;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.core.GridServer;
import com.nordstrom.automation.selenium.core.GridUtility;
import com.nordstrom.automation.selenium.utility.HostUtils;
import com.nordstrom.common.uri.UriUtils;

/**
 * Background scanner that discovers unmanaged Selenium Grid instances by probing
 * a configurable port range in time-limited chunks.
 * <p>
 * The scanner maintains state between chunks so that a full port range scan can
 * span multiple scheduler invocations without blocking other sidecar activity.
 * Fine-toothed mode (step size 1) can be requested at any time; the scanner
 * resets to the start of the range and switches to step 1 immediately.
 * <p>
 * Ports occupied by managed grid collections are excluded from the scan.
 *
 * @since [next-major]
 */
public class GridInstanceScanner {

    private static final Logger LOGGER = LoggerFactory.getLogger(GridInstanceScanner.class);

    private final Set<Integer> managedPorts;
    private final int scanStart;
    private final int scanEnd;
    private final int scanStep;
    private final long chunkDurationMs;

    private volatile List<HubStatus> discoveredCache = Collections.emptyList();
    private volatile boolean fineToothedRequested = false;
    private volatile long lastScanTime = 0;
    private volatile boolean scanning = false;
    private volatile int scanProgress = 0;
    private volatile int lastScanFound = 0;
    private volatile long lastScanAdded = 0;
    private volatile long lastScanRemoved = 0;

    private final AtomicInteger currentPort;

    private final ScheduledExecutorService scheduler =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = new Thread(r, "grid-instance-scanner");
                t.setDaemon(true);
                return t;
            });

    /**
     * Constructor for grid instance scanner.
     *
     * @param managedPorts set of hub ports currently managed by this sidecar;
     *        these ports are excluded from the scan
     */
    public GridInstanceScanner(Set<Integer> managedPorts) {
        this.managedPorts = managedPorts;

        SeleniumConfig config = SeleniumConfig.getConfig();
        this.scanStart = config.getInt(SeleniumSettings.SIDECAR_SCAN_START.key());
        this.scanEnd = config.getInt(SeleniumSettings.SIDECAR_SCAN_END.key());
        this.scanStep = config.getInt(SeleniumSettings.SIDECAR_SCAN_STEP.key());
        this.chunkDurationMs = config.getLong(SeleniumSettings.SIDECAR_SCAN_CHUNK_DURATION.key());
        this.currentPort = new AtomicInteger(scanStart);

        long intervalSeconds = config.getLong(SeleniumSettings.SIDECAR_SCAN_INTERVAL.key());
        if (intervalSeconds > 0) {
            scheduler.scheduleWithFixedDelay(this::scanChunk, 0,
                    intervalSeconds * 1000, TimeUnit.MILLISECONDS);
            LOGGER.debug("Grid instance scanner started: ports {}-{}, step {}, chunk {}ms, interval {}s",
                    scanStart, scanEnd, scanStep, chunkDurationMs, intervalSeconds);
        } else {
            LOGGER.debug("Grid instance scanner disabled");
        }
    }

    /**
     * Request a fine-toothed scan (step size 1) starting from the beginning of the port range.
     * The scanner resets immediately and switches to step 1 for the next chunk.
     */
    public void requestFineToothed() {
        fineToothedRequested = true;
        currentPort.set(scanStart);
        LOGGER.debug("Fine-toothed scan requested — resetting to port {}", scanStart);
    }

    /** @return cached list of discovered unmanaged hub instances */
    public List<HubStatus> getDiscovered() { return discoveredCache; }

    /** @return time of the last completed scan chunk in epoch milliseconds; 0 if no scan has completed */
    public long lastScanTime() { return lastScanTime; }

    /** @return {@code true} if a scan chunk is currently in progress */
    public boolean isScanning() { return scanning; }

    /** @return progress of the current scan as a percentage (0-100) */
    public int scanProgress() { return scanProgress; }

    /** @return number of unmanaged instances found in the last completed full scan */
    public int lastScanFound() { return lastScanFound; }

    /** @return number of instances added in the last completed full scan */
    public long lastScanAdded() { return lastScanAdded; }

    /** @return number of instances removed in the last completed full scan */
    public long lastScanRemoved() { return lastScanRemoved; }

    /**
     * Shut down the background scanner.
     */
    public void shutdown() {
        scheduler.shutdownNow();
        LOGGER.debug("Grid instance scanner shut down");
    }

    /**
     * Execute one scan chunk, probing ports from the current position up to the
     * chunk duration limit. Resets to the start of the range when the end is reached.
     */
    private void scanChunk() {
        // handle fine-toothed reset
        boolean isFineToothed = fineToothedRequested;
        if (isFineToothed) {
            fineToothedRequested = false;
            currentPort.set(scanStart);
        }

        int step = isFineToothed ? 1 : scanStep;
        int port = currentPort.get();
        boolean isNewPass = (port == scanStart);

        scanning = true;
        long chunkStart = System.currentTimeMillis();
        long chunkEnd = chunkStart + chunkDurationMs;

        List<HubStatus> found = isNewPass ? new ArrayList<>() : new ArrayList<>(discoveredCache);
        Set<URL> previousUrls = new HashSet<>();
        for (HubStatus h : discoveredCache) previousUrls.add(h.getHubUrl());

        LOGGER.debug("Scan chunk starting at port {} (step={}, fineToothed={})", port, step, isFineToothed);

        while (port <= scanEnd && System.currentTimeMillis() < chunkEnd) {
            if (!managedPorts.contains(port)) {
                URL candidate = buildHubUrl(port);
                if (candidate != null && GridServer.isHubActive(candidate)) {
                    int apiVersion = probeApiVersion(candidate);
                    HubStatus status = apiVersion == 4
                            ? HubStatus.discoveredSelenium4(candidate)
                            : HubStatus.discoveredSelenium3(candidate);
                    // only add if not already in the cache
                    if (!previousUrls.contains(candidate)) {
                        found.add(status);
                        LOGGER.debug("Discovered unmanaged Selenium {} Grid at: {}", apiVersion, candidate);
                    }
                }
            }
            port += step;
            scanProgress = (int) (((long)(port - scanStart) * 100) / (scanEnd - scanStart + 1));
        }

        if (port > scanEnd) {
            // completed a full pass
            List<HubStatus> previous = discoveredCache;
            List<HubStatus> current = Collections.unmodifiableList(found);

            Set<URL> currentUrls = new HashSet<>();
            for (HubStatus h : current) currentUrls.add(h.getHubUrl());

            long additions = current.stream().filter(h -> !previousUrls.contains(h.getHubUrl())).count();
            long removals = previous.stream().filter(h -> !currentUrls.contains(h.getHubUrl())).count();

            previous.stream().filter(h -> !currentUrls.contains(h.getHubUrl()))
                    .forEach(h -> LOGGER.debug("Lost unmanaged Selenium {} Grid at: {}",
                            h.getApiVersion(), h.getHubUrl()));

            discoveredCache = current;
            lastScanTime = System.currentTimeMillis();
            lastScanFound = current.size();
            lastScanAdded = additions;
            lastScanRemoved = removals;
            scanProgress = 100;

            LOGGER.debug("Scan pass complete: {} instance(s) found ({} added, {} removed)",
                    current.size(), additions, removals);

            // reset for next pass
            currentPort.set(scanStart);
        } else {
            // chunk ended mid-pass — save position for next chunk
            currentPort.set(port);
            LOGGER.debug("Scan chunk complete at port {} — resuming next interval", port);
        }

        scanning = false;
    }

    /**
     * Build a hub URL for the specified port on the local host.
     *
     * @param port port to probe
     * @return {@link URL} for the local hub at the specified port; {@code null} if URL construction fails
     */
    private static URL buildHubUrl(int port) {
        try {
            return UriUtils.makeBasicURI("http", HostUtils.getLocalHost(), port).toURL();
        } catch (MalformedURLException e) {
            return null;
        }
    }

    /**
     * Probe the API version of a confirmed-active server of unknown provenance.
     * Probing is appropriate here because the server was not launched by this JVM.
     * For servers we launch ourselves, the API version is always passed explicitly.
     *
     * @param hubUrl {@link URL} of the hub to probe
     * @return 4 if the server appears to be Selenium 4; otherwise 3
     */
    private static int probeApiVersion(URL hubUrl) {
        // Selenium 4 exposes /ui
        if (GridUtility.isHostActive(hubUrl, "/ui")) return 4;
        // Selenium 3 exposes /grid/console
        if (GridUtility.isHostActive(hubUrl, "/grid/console")) return 3;
        // Unknown version — log warning and default to 3 for backward compatibility
        LOGGER.warn("Unable to determine API version for Grid hub at {} — defaulting to Selenium 3", hubUrl);
        return 3;
    }
}
