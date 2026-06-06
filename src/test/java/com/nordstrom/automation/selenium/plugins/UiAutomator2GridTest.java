package com.nordstrom.automation.selenium.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.Before;
import org.junit.Test;
import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.annotations.InitialPage;
import com.nordstrom.automation.selenium.core.SeleniumGrid;
import com.nordstrom.automation.selenium.examples.AndroidPage;
import com.nordstrom.automation.selenium.examples.ExamplePage;
import com.nordstrom.automation.selenium.junit.JUnitBase;
import com.nordstrom.utility.GridLauncher;

@InitialPage(AndroidPage.class)
public class UiAutomator2GridTest extends JUnitBase {
    
    private static SeleniumGrid seleniumGrid = null;
    private final ManagedDriverPlugin plugin = new UiAutomator2Plugin();

    @Before
    public void beforeTest() {
        createSeleniumGrid();
        ExamplePage.setHubAsTarget();
    }

    @Test
    public void testSearchActivity() {
        AndroidPage page = getInitialPage();
        page.submitSearchQuery("Hello world!");
        assertEquals("Hello world!", page.getSearchResult());
    }

    private void createSeleniumGrid() {
        if (seleniumGrid == null) {
            seleniumGrid = createGrid();
        }
    }
    
    private SeleniumGrid createGrid() {
        return GridLauncher.create(getPlugin());
    }
    
    private ManagedDriverPlugin getPlugin() {
        return plugin;
    }
    
}
