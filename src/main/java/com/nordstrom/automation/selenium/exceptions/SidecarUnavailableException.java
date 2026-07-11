package com.nordstrom.automation.selenium.exceptions;

import java.net.URL;

/**
 * Thrown when the sidecar servlet container is unavailable or returns an unexpected response.
 *
 * @since [next-major]
 */
public class SidecarUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** URL of the sidecar endpoint that was unavailable. */
    private final URL url;

    /**
     * Constructor for sidecar unavailable exception.
     *
     * @param url {@link URL} of the sidecar endpoint that was unavailable
     * @param cause the underlying cause
     */
    public SidecarUnavailableException(URL url, Throwable cause) {
        super("Sidecar unavailable at: " + url, cause);
        this.url = url;
    }

    /**
     * Constructor for sidecar unavailable exception without a cause.
     *
     * @param url {@link URL} of the sidecar endpoint that was unavailable
     * @param message additional detail message
     */
    public SidecarUnavailableException(URL url, String message) {
        super("Sidecar unavailable at: " + url + " — " + message);
        this.url = url;
    }

    /**
     * Get the URL of the sidecar endpoint that was unavailable.
     *
     * @return sidecar endpoint {@link URL}
     */
    public URL getUrl() {
        return url;
    }
}
