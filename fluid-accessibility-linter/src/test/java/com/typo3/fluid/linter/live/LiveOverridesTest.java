package com.typo3.fluid.linter.live;

import com.intellij.openapi.vfs.VirtualFile;
import com.typo3.fluid.linter.catalog.ImplementationCatalogService;
import com.typo3.fluid.linter.catalog.ImplementationKind;
import com.typo3.fluid.linter.context.ContextId;
import com.typo3.fluid.linter.testsupport.FluidPluginTestBase;

import static org.junit.Assert.*;

public class LiveOverridesTest extends FluidPluginTestBase {

    public void testAddingSiteOverrideSwitchesResolution() {
        ImplementationCatalogService catalog = getProject().getService(ImplementationCatalogService.class);
        assertNotNull(catalog);

        // Ensure vendor active by deleting site file if present
        try {
            write(() -> { var f = findVFile("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html"); f.delete(this); return null; });
        } catch (AssertionError | Exception ignored) {}
        com.typo3.fluid.linter.update.GraphUpdateServiceTestUtil.flush(getProject());

        VirtualFile effVendor = catalog.effective(ContextId.DEFAULT, ImplementationKind.PARTIAL, "Navigation/Breadcrumb");
        assertNotNull(effVendor);
        assertTrue(effVendor.getPath(), effVendor.getPath().contains("ext_vendor/Resources/Private/Partials/Navigation/Breadcrumb.html"));

        // Add high-priority override
        String text = "<nav aria-label=\"breadcrumb\"><h3>SiteA Breadcrumb</h3></nav>";
        createFile("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html", text);
        com.typo3.fluid.linter.update.GraphUpdateServiceTestUtil.flush(getProject());

        VirtualFile effSite = catalog.effective(ContextId.DEFAULT, ImplementationKind.PARTIAL, "Navigation/Breadcrumb");
        assertNotNull(effSite);
        assertTrue(effSite.getPath(), effSite.getPath().contains("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html"));
    }
}
