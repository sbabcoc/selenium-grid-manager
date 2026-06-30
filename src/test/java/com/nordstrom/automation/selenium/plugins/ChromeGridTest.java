package com.nordstrom.automation.selenium.plugins;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.plugins.ChromePlugin;
import com.nordstrom.automation.selenium.utility.AbstractGridTest;

public class ChromeGridTest extends AbstractGridTest {

    private final ManagedDriverPlugin plugin = new ChromePlugin();

    @Override
    public ManagedDriverPlugin getPlugin() {
        return plugin;
    }
}
