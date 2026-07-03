package com.nordstrom.automation.selenium.core;

import java.io.IOException;
import java.util.concurrent.TimeoutException;

import com.nordstrom.common.base.UncheckedThrow;

/**
 * This class registers the local Grid factory with {@link SeleniumGrid} when
 * {@code selenium-grid-manager} is on the classpath.
 */
public class GridManagerPluginImpl implements GridManagerPlugin {
    static {
        SeleniumGrid.registerLocalGridFactory((config, hubUrl) -> {
            try {
                SeleniumGrid grid = LocalSeleniumGrid.create(config, hubUrl);
                grid.activate();
                return grid;
            } catch (IOException | InterruptedException | TimeoutException e) {
                throw UncheckedThrow.throwUnchecked(e);
            }
        });
    }
}
