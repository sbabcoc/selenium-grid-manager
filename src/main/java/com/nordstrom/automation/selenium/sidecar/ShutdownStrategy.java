package com.nordstrom.automation.selenium.sidecar;

interface ShutdownStrategy {
    /**
     * Shut down the server identified by the specified registration.
     *
     * @param registration {@link GridServerRegistration} of the server to shut down
     * @throws Exception if shutdown fails
     */
    void shutdown(GridServerRegistration registration) throws Exception;
}
