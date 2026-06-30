package com.nordstrom.automation.selenium.plugins;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.plugins.EdgePlugin;
import com.nordstrom.automation.selenium.utility.AbstractGridTest;

public class EdgeGridTest extends AbstractGridTest {

    private final ManagedDriverPlugin plugin = new EdgePlugin();

    @Override
    public ManagedDriverPlugin getPlugin() {
        return plugin;
    }

}
