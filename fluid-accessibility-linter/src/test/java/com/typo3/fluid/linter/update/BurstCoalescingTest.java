package com.typo3.fluid.linter.update;

import com.typo3.fluid.linter.testsupport.FluidPluginTestBase;

import static org.junit.Assert.*;

public class BurstCoalescingTest extends FluidPluginTestBase {
    public void testFiftyCreatesCoalesceIntoSingleRun() {
        GraphUpdateServiceTestUtil.resetRunCount(getProject());
        for (int i = 0; i < 50; i++) {
            createFile("ext_vendor/Resources/Private/Partials/Auto/Card_" + i + ".html", "<div>card" + i + "</div>");
        }
        GraphUpdateServiceTestUtil.flush(getProject());
        assertEquals(1, GraphUpdateServiceTestUtil.getRunCount(getProject()));
    }
}
