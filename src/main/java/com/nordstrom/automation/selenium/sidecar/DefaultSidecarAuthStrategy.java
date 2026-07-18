package com.nordstrom.automation.selenium.sidecar;

/**
 * Default {@link SidecarAuthStrategy} implementation combining token+session authentication
 * for browser clients and OS user identity authentication for CLI clients.
 * <p>
 * Its class name is registered as the built-in default via {@code getDefaults()} in the
 * appropriate {@link com.nordstrom.automation.selenium.SeleniumConfig} subclass in
 * {@code selenium-foundation}.
 *
 * @since 36.0.0
 */
public class DefaultSidecarAuthStrategy extends CompositeAuthStrategy {

    /**
     * Constructor for default sidecar auth strategy.
     * Chains {@link TokenSessionAuthStrategy} and {@link OsUserAuthStrategy}.
     */
    public DefaultSidecarAuthStrategy() {
        super(new TokenSessionAuthStrategy(), new OsUserAuthStrategy());
    }
}
