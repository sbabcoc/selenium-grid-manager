package com.nordstrom.automation.selenium.plugins;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.plugins.HtmlUnitPlugin;
import com.nordstrom.automation.selenium.utility.AbstractGridTest;

public class HtmlUnitGridTest extends AbstractGridTest {

    private final ManagedDriverPlugin plugin = new HtmlUnitPlugin();

    @Override
    public ManagedDriverPlugin getPlugin() {
        return plugin;
    }
}
