package com.nordstrom.utility;

import static org.junit.Assert.assertEquals;
import static org.junit.Assume.assumeNoException;
import static com.nordstrom.automation.selenium.examples.ExamplePage.*;
import static org.junit.Assert.assertArrayEquals;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.openqa.selenium.WebDriver;

import com.nordstrom.automation.selenium.ManagedDriverPlugin;
import com.nordstrom.automation.selenium.SeleniumConfig;
import com.nordstrom.automation.selenium.annotations.InitialPage;
import com.nordstrom.automation.selenium.core.GridUtility;
import com.nordstrom.automation.selenium.core.SeleniumGrid;
import com.nordstrom.automation.selenium.examples.ExamplePage;
import com.nordstrom.automation.selenium.examples.FrameComponent;
import com.nordstrom.automation.selenium.examples.ShadowRootComponent;
import com.nordstrom.automation.selenium.examples.TableComponent;
import com.nordstrom.automation.selenium.exceptions.ShadowRootContextException;
import com.nordstrom.automation.selenium.interfaces.DriverProvider;
import com.nordstrom.automation.selenium.junit.JUnitBase;

@InitialPage(ExamplePage.class)
public abstract class AbstractGridTest extends JUnitBase implements DriverProvider {

    private static SeleniumGrid seleniumGrid = null;

    @Before
    public void beforeTest() {
        launchSeleniumGrid();
        ExamplePage.setHubAsTarget();
    }

    @Test
    public void testBasicPage() {
        ExamplePage page = getInitialPage();
        assertEquals(TITLE, page.getTitle());
    }

    @Test
    public void testParagraphs() {
        ExamplePage page = getInitialPage();
        List<String> paraList = page.getParagraphs();
        assertEquals(4, paraList.size());
        assertArrayEquals(PARAS, paraList.toArray());
    }

    @Test
    public void testTable() {
        ExamplePage page = getInitialPage();
        TableComponent component = page.getTable();
        verifyTable(component);
    }

    /**
     * Verify the contents of the specified table component
     *
     * @param component table component to be verified
     */
    private static void verifyTable(TableComponent component) {
        assertArrayEquals(HEADINGS, component.getHeadings().toArray());
        List<List<String>> content = component.getContent();
        assertEquals(3, content.size());
        assertArrayEquals(CONTENT[0], content.get(0).toArray());
        assertArrayEquals(CONTENT[1], content.get(1).toArray());
        assertArrayEquals(CONTENT[2], content.get(2).toArray());
    }

    @Test
    public void testFrameByLocator() {
        ExamplePage page = getInitialPage();
        FrameComponent component = page.getFrameByLocator();
        assertEquals(FRAME_A, component.getPageContent());
    }

    @Test
    public void testFrameByElement() {
        ExamplePage page = getInitialPage();
        FrameComponent component = page.getFrameByElement();
        assertEquals(FRAME_B, component.getPageContent());
    }

    @Test
    public void testFrameByIndex() {
        ExamplePage page = getInitialPage();
        FrameComponent component = page.getFrameByIndex();
        assertEquals(FRAME_C, component.getPageContent());
    }

    @Test
    @Ignore // "frame by ID" is deprecated
    public void testFrameById() {
        ExamplePage page = getInitialPage();
        FrameComponent component = page.getFrameById();
        assertEquals(FRAME_D, component.getPageContent());
    }

    @Test
    public void testComponentList() {
        ExamplePage page = getInitialPage();
        List<TableComponent> componentList = page.getTableList();
        verifyTable(componentList.get(0));
    }

    @Test
    public void testComponentMap() {
        ExamplePage page = getInitialPage();
        Map<Object, TableComponent> componentMap = page.getTableMap();
        verifyTable(componentMap.get(TABLE_ID));
    }

    @Test
    public void testFrameList() {
        ExamplePage page = getInitialPage();
        List<FrameComponent> frameList = page.getFrameList();
        assertEquals(4, frameList.size());
        assertEquals(FRAME_A, frameList.get(0).getPageContent());
        assertEquals(FRAME_B, frameList.get(1).getPageContent());
        assertEquals(FRAME_C, frameList.get(2).getPageContent());
        assertEquals(FRAME_D, frameList.get(3).getPageContent());
    }

    @Test
    public void testFrameMap() {
        ExamplePage page = getInitialPage();
        Map<Object, FrameComponent> frameMap = page.getFrameMap();
        assertEquals(4, frameMap.size());
        assertEquals(FRAME_A, frameMap.get(FRAME_A).getPageContent());
        assertEquals(FRAME_B, frameMap.get(FRAME_B).getPageContent());
        assertEquals(FRAME_C, frameMap.get(FRAME_C).getPageContent());
        assertEquals(FRAME_D, frameMap.get(FRAME_D).getPageContent());
    }

    @Test
    public void testShadowRootByLocator() {
        ExamplePage page = getInitialPage();
        try {
            ShadowRootComponent shadowRoot = page.getShadowRootByLocator();
            assertEquals(SHADOW_DOM_A, shadowRoot.getHeading());
        } catch (ShadowRootContextException e) {
            assumeNoException(e);
        }
    }

    @Test
    public void testShadowRootByElement() {
        ExamplePage page = getInitialPage();
        try {
            ShadowRootComponent shadowRoot = page.getShadowRootByElement();
            assertEquals(SHADOW_DOM_B, shadowRoot.getHeading());
        } catch (ShadowRootContextException e) {
            assumeNoException(e);
        }
    }

    @Test
    public void testShadowRootList() {
        ExamplePage page = getInitialPage();
        try {
            List<ShadowRootComponent> shadowRootList = page.getShadowRootList();
            assertEquals(2, shadowRootList.size());
            assertEquals(SHADOW_DOM_A, shadowRootList.get(0).getHeading());
            assertEquals(SHADOW_DOM_B, shadowRootList.get(1).getHeading());
        } catch (ShadowRootContextException e) {
            assumeNoException(e);
        }
    }

    @Test
    public void testShadowRootMap() {
        ExamplePage page = getInitialPage();
        try {
            Map<Object, ShadowRootComponent> shadowRootMap = page.getShadowRootMap();
            assertEquals(2, shadowRootMap.size());
            assertEquals(SHADOW_DOM_A, shadowRootMap.get(SHADOW_DOM_A).getHeading());
            assertEquals(SHADOW_DOM_B, shadowRootMap.get(SHADOW_DOM_B).getHeading());
        } catch (ShadowRootContextException e) {
            assumeNoException(e);
        }
    }

    @Test
    public void testRefresh() {
        ExamplePage page = getInitialPage();
        TableComponent component = page.getTable();
        verifyTable(component);

        int pageRefreshCount = page.getRefreshCount();
        int tableRefreshCount = component.getRefreshCount();
        int headRefreshCount = component.getHeadRefreshCount();
        int[] bodyRefreshCounts = component.getBodyRefreshCounts();

        assertEquals(0, pageRefreshCount);
        assertEquals(0, tableRefreshCount);
        assertEquals(0, headRefreshCount);
        assertArrayEquals(new int[] {0, 0, 0}, bodyRefreshCounts);

        page.getWrappedDriver().navigate().refresh();
        verifyTable(component);

        pageRefreshCount = page.getRefreshCount();
        tableRefreshCount = component.getRefreshCount();
        headRefreshCount = component.getHeadRefreshCount();
        bodyRefreshCounts = component.getBodyRefreshCounts();

        assertEquals(2, pageRefreshCount);
        assertEquals(8, tableRefreshCount);
        assertEquals(1, headRefreshCount);
        assertArrayEquals(new int[] {1, 1, 1}, bodyRefreshCounts);

        verifyTable(component);

        pageRefreshCount = page.getRefreshCount();
        tableRefreshCount = component.getRefreshCount();
        headRefreshCount = component.getHeadRefreshCount();
        bodyRefreshCounts = component.getBodyRefreshCounts();

        assertEquals(2, pageRefreshCount);
        assertEquals(8, tableRefreshCount);
        assertEquals(1, headRefreshCount);
        assertArrayEquals(new int[] {1, 1, 1}, bodyRefreshCounts);
    }

    @Override
    public WebDriver provideDriver(Method method) {
        SeleniumConfig config = SeleniumConfig.getConfig();
        return GridUtility.getDriver(config.getHubUrl(), config.getCurrentCapabilities());
    }

    private void launchSeleniumGrid() {
        if (seleniumGrid == null) {
            seleniumGrid = launchGrid();
        }
    }

    protected SeleniumGrid launchGrid() {
        return GridLauncher.launch(getPlugin());
    }

    /**
     * Get the driver plugin for this test class.
     *
     * @return {@link ManagedDriverPlugin} object for the target browser
     */
    public abstract ManagedDriverPlugin getPlugin();
}
