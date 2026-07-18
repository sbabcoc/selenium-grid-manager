package com.nordstrom.automation.selenium.sidecar;

import java.util.Arrays;
import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Chains multiple {@link SidecarAuthStrategy} implementations, authorizing if any
 * one of them authenticates or authorizes the request. Custom implementations may
 * extend this class to build composite strategies.
 *
 * @since 36.0.0
 */
public class CompositeAuthStrategy implements SidecarAuthStrategy {

    private final List<SidecarAuthStrategy> strategies;

    /**
     * Constructor for composite auth strategy.
     *
     * @param strategies one or more {@link SidecarAuthStrategy} implementations to chain
     */
    public CompositeAuthStrategy(SidecarAuthStrategy... strategies) {
        this.strategies = Arrays.asList(strategies);
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} if any chained strategy authenticates the request.
     */
    @Override
    public boolean isAuthenticated(HttpServletRequest req) {
        return strategies.stream().anyMatch(s -> s.isAuthenticated(req));
    }

    /**
     * {@inheritDoc}
     * <p>
     * Returns {@code true} if any chained strategy authorizes the request.
     */
    @Override
    public boolean isAuthorized(HttpServletRequest req, HttpServletResponse resp) {
        return strategies.stream().anyMatch(s -> s.isAuthorized(req, resp));
    }
}
