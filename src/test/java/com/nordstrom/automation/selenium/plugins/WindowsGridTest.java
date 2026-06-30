package com.nordstrom.automation.selenium.plugins;

import static org.junit.Assert.assertEquals;

import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.openqa.selenium.Keys;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.annotations.InitialPage;
import com.nordstrom.automation.selenium.core.SeleniumGrid;
import com.nordstrom.automation.selenium.examples.ExamplePage;
import com.nordstrom.automation.selenium.examples.NotepadApplication;
import com.nordstrom.automation.selenium.junit.JUnitBase;
import com.nordstrom.automation.selenium.utility.GridLauncher;
import com.nordstrom.common.file.OSInfo;
import com.nordstrom.common.file.OSInfo.OSType;

@InitialPage(NotepadApplication.class)
public class WindowsGridTest extends JUnitBase {
    
    private static SeleniumGrid seleniumGrid = null;
    private final ManagedDriverPlugin plugin = new WindowsPlugin();

    @BeforeClass
    public static void beforeClass() {
        Assume.assumeTrue(OSInfo.getDefault().getType() == OSType.WINDOWS);
    }

    @Before
    public void beforeTest() {
        createSeleniumGrid();
        ExamplePage.setHubAsTarget();
    }

    @Test
    public void testEditing() {
        NotepadApplication page = getInitialPage();
        page.modifyDocument("Hello world!");
        assertEquals(page.getDocumentContent(), "Hello world!");
        page.modifyDocument(Keys.CONTROL + "z");
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
