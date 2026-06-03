package com.nordstrom.automation.selenium.core;

import java.io.IOException;
import com.nordstrom.automation.selenium.core.GridManagerPlugin;
import com.nordstrom.automation.selenium.core.LocalSeleniumGrid;
import com.nordstrom.automation.selenium.core.SeleniumGrid;
import com.nordstrom.common.base.UncheckedThrow;

/**
 * This class registers the local Grid factory with {@link SeleniumGrid} when
 * {@code selenium-grid-manager} is on the classpath.
 */
public class GridManagerPluginImpl implements GridManagerPlugin {
    static {
        SeleniumGrid.registerLocalGridFactory((config, hubUrl) -> {
            try {
                return LocalSeleniumGrid.create(config, hubUrl);
            } catch (IOException e) {
                throw UncheckedThrow.throwUnchecked(e);
            }
        });
    }
}
