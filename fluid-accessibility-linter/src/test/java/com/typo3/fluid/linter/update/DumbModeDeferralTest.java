package com.typo3.fluid.linter.update;

import com.intellij.openapi.project.DumbService;
import com.typo3.fluid.linter.testsupport.FluidPluginTestBase;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class DumbModeDeferralTest extends FluidPluginTestBase {

    public void testRecomputeDeferredInDumbMode_thenFlushOnSmart() throws Exception {
        GraphUpdateServiceTestUtil.resetRunCount(getProject());

        // Try to switch to dumb mode via DumbServiceImpl#setDumb
        try {
            Class<?> impl = Class.forName("com.intellij.openapi.project.impl.DumbServiceImpl");
            Method setDumb = impl.getMethod("setDumb", boolean.class);
            setDumb.invoke(DumbService.getInstance(getProject()), true);
        } catch (Throwable t) {
            // If not available in this test runtime, skip gracefully
            return;
        }

        // Create a new site override while dumb
        createFile("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html", "<nav><h3>SiteA Breadcrumb</h3></nav>");
        GraphUpdateServiceTestUtil.flush(getProject());

        // Expect no recompute while dumb
        assertEquals(0, GraphUpdateServiceTestUtil.getRunCount(getProject()));

        // Back to smart mode
        Class<?> impl = Class.forName("com.intellij.openapi.project.impl.DumbServiceImpl");
        Method setDumb = impl.getMethod("setDumb", boolean.class);
        setDumb.invoke(DumbService.getInstance(getProject()), false);

        GraphUpdateServiceTestUtil.flush(getProject());
        assertTrue(GraphUpdateServiceTestUtil.getRunCount(getProject()) >= 1);
        assertTrue(GraphUpdateServiceTestUtil.lastRunHadChanges(getProject()));
    }
}
