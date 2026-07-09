package com.nordstrom.automation.selenium.sidecar;

import java.io.IOException;
import java.net.ServerSocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Default implementation of {@link GridPortAllocationStrategy}.
 * <p>
 * Allocates port bundles by opening {@link ServerSocket}s on all required ports simultaneously
 * to verify they are all free, then closing them before returning. This prevents race conditions
 * where individual port checks succeed but the full bundle is unavailable.
 * <p>
 * For Selenium 4 bundles, the event bus publisher port is {@code hub - 2} and the subscriber
 * port is {@code hub - 1}, forming a tight cluster around the hub port.
 *
 * @since [next-major]
 */
public class DefaultGridPortAllocationStrategy implements GridPortAllocationStrategy {

    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultGridPortAllocationStrategy.class);
    private static final int PORT_STEP = 10;

    /**
     * {@inheritDoc}
     */
    @Override
    public GridHubPortAllocator.GridPorts allocateBundle(int seed) {
        int hub = seed;
        while (hub < 65530) {
            int pub = hub - 2;
            int sub = hub - 1;
            if (pub < 1024) {
                hub += PORT_STEP;
                continue;
            }
            Reservation r = reserve(hub, pub, sub);
            if (r.success) {
                r.close();
                LOGGER.debug("Allocated port bundle: hub={}, pub={}, sub={}", hub, pub, sub);
                return new GridHubPortAllocator.GridPorts(hub, pub, sub);
            }
            hub += PORT_STEP;
        }
        throw new RuntimeException("No free Grid port bundle found");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int allocateHub(int seed) {
        int hub = seed;
        while (hub < 65535) {
            Reservation r = reserve(hub);
            if (r.success) {
                r.close();
                LOGGER.debug("Allocated hub port: {}", hub);
                return hub;
            }
            hub += PORT_STEP;
        }
        throw new RuntimeException("No free hub port found");
    }

    /**
     * Attempt to reserve three ports simultaneously.
     *
     * @param hub hub port
     * @param pub event bus publisher port
     * @param sub event bus subscriber port
     * @return {@link Reservation} indicating success or failure
     */
    private Reservation reserve(int hub, int pub, int sub) {
        ServerSocket hubSocket = null;
        ServerSocket pubSocket = null;
        ServerSocket subSocket = null;
        try {
            hubSocket = new ServerSocket(hub);
            pubSocket = new ServerSocket(pub);
            subSocket = new ServerSocket(sub);
            return new Reservation(true, hubSocket, pubSocket, subSocket);
        } catch (IOException e) {
            closeQuiet(hubSocket);
            closeQuiet(pubSocket);
            closeQuiet(subSocket);
            return new Reservation(false, null, null, null);
        }
    }

    /**
     * Attempt to reserve a single port.
     *
     * @param port port to reserve
     * @return {@link Reservation} indicating success or failure
     */
    private Reservation reserve(int port) {
        try {
            return new Reservation(true, new ServerSocket(port), null, null);
        } catch (IOException e) {
            return new Reservation(false, null, null, null);
        }
    }

    /**
     * Close the specified socket quietly, ignoring any exception.
     *
     * @param socket socket to close; may be {@code null}
     */
    private static void closeQuiet(ServerSocket socket) {
        if (socket != null) {
            try {
                socket.close();
            } catch (IOException ignored) {
                // nothing to do here
            }
        }
    }

    /**
     * Holds references to reserved sockets pending release.
     */
    private static class Reservation {
        final boolean success;
        final ServerSocket hub;
        final ServerSocket pub;
        final ServerSocket sub;

        Reservation(boolean success, ServerSocket hub, ServerSocket pub, ServerSocket sub) {
            this.success = success;
            this.hub = hub;
            this.pub = pub;
            this.sub = sub;
        }

        void close() {
            closeQuiet(hub);
            closeQuiet(pub);
            closeQuiet(sub);
        }

        private static void closeQuiet(ServerSocket socket) {
            if (socket != null) {
                try {
                    socket.close();
                } catch (IOException ignored) {
                    // nothing to do here
                }
            }
        }
    }
}
