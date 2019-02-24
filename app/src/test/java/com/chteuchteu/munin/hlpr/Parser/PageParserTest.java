package com.chteuchteu.munin.hlpr.Parser;

import com.chteuchteu.munin.BaseUnitTest;
import com.chteuchteu.munin.resources.TestFlavour;

import org.junit.Test;

import static org.junit.Assert.*;

public class PageParserTest extends BaseUnitTest {
    @Test
    public void test_detectPageType() {
        // Test for each flavour
        for (TestFlavour flavour : TestFlavour.values()) {
            this.test_detectPageType_flavour(flavour, "nodes-list", PageType.NODES_LIST);
            this.test_detectPageType_flavour(flavour, "plugins-list", PageType.PLUGINS_LIST);
        }
    }

    private void test_detectPageType_flavour(TestFlavour flavour, String pageName, PageType expectedType)
    {
        String testFile = readResource(this.getClass(), "html/"+flavour.toString()+"/"+pageName+".html");

        PageType detectedPageType = PageParser.detectPageType(testFile, "");
        // Ensure we detected the expected page type
        assertEquals("Expected page type should match for flavour = "+flavour.toString()+", pageName = "+pageName, expectedType, detectedPageType);
    }

    @Test
    public void test_findMasterName() {
        assertEquals("ping.uio.no", PageParser.findMasterName(readResource(this.getClass(), "html/munin-1.4/nodes-list.html"), ""));
        assertEquals("munin-for-android.com", PageParser.findMasterName(readResource(this.getClass(), "html/munin-2/nodes-list.html"), ""));
        assertNull(PageParser.findMasterName(readResource(this.getClass(), "html/munstrap/nodes-list.html"), ""));
    }
}
