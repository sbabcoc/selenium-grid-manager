package com.nordstrom.automation.selenium.sidecar;

/**
 * The relationship between the sidecar and a given hub instance.
 * <p>
 * A hub is exactly one of these three — never more than one — since a hub the
 * sidecar launched and manages is never simultaneously represented as merely
 * monitored, and a hub the sidecar is monitoring is by definition not one it
 * launched.
 *
 * @since 36.2.0
 */
public enum HubRelation {

    /**
     * Launched and managed by this sidecar. The sidecar owns this hub's
     * lifecycle and may shut it down.
     */
    MANAGED,

    /**
     * Not launched by this sidecar, but explicitly tracked for console
     * visibility and easy access. The sidecar never attempts to shut this
     * hub down.
     */
    MONITORED,

    /**
     * Found by the background {@link GridInstanceScanner}. Neither managed
     * nor monitored.
     */
    DISCOVERED
}
