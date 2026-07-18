package com.nordstrom.automation.selenium.sidecar;

import java.io.IOException;
import java.util.List;

import org.eclipse.jetty.server.Server;

import com.nordstrom.automation.selenium.examples.ServletContainer;

/**
 * Selenium 4 implementation of {@link SidecarSupport}.
 * Uses standard Jetty ({@code org.eclipse.jetty}).
 *
 * @since 36.0.0
 */
public class SidecarSupportImpl implements SidecarSupport {

    private static Server sidecarServer;

    /**
     * {@inheritDoc}
     */
    @Override
    public void start(List<String> servletClasses, int port) throws IOException {
        try {
            sidecarServer = ServletContainer.start(port, servletClasses);
        } catch (Exception e) {
            throw new IOException("Failed starting sidecar servlet container on port " + port, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void await() throws InterruptedException {
        if (sidecarServer != null) {
            try {
                sidecarServer.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw e;
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isActive() {
        return sidecarServer != null && sidecarServer.isRunning();
    }
}
