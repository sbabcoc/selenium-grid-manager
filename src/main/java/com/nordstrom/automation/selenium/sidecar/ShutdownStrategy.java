package com.nordstrom.automation.selenium.sidecar;

/**
 * Internal contract for sidecar shutdown strategy implementations.
 * <p>
 * This is an internal implementation detail of the sidecar — it is not a user extension point.
 * The appropriate implementation is selected automatically by {@link GridRegistry} based on
 * the {@link ShutdownMode} recorded in the {@link GridServerRegistration} at launch time.
 *
 * @since [next-major]
 */
interface ShutdownStrategy {

    /**
     * Shut down the server identified by the specified registration.
     *
     * @param registration {@link GridServerRegistration} of the server to shut down
     * @throws Exception if shutdown fails
     */
    void shutdown(GridServerRegistration registration) throws Exception;
}
