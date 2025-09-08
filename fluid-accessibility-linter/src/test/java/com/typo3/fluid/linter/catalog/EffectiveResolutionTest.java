package com.typo3.fluid.linter.catalog;

import com.intellij.openapi.vfs.VirtualFile;
import com.typo3.fluid.linter.context.ContextId;
import com.typo3.fluid.linter.testsupport.FluidPluginTestBase;

import static org.junit.Assert.*;

public class EffectiveResolutionTest extends FluidPluginTestBase {

    public void testSiteOverrideIsEffective_thenFallbackToVendorOnDelete() throws Exception {
        ImplementationCatalogService catalog = getProject().getService(ImplementationCatalogService.class);
        assertNotNull(catalog);

        VirtualFile eff = catalog.effective(ContextId.DEFAULT, ImplementationKind.PARTIAL, "Navigation/Breadcrumb");
        assertNotNull(eff);
        assertTrue(eff.getPath(), eff.getPath().contains("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html"));

        // Delete site override and expect fallback to vendor
        write(() -> {
            VirtualFile f = findVFile("site_a/Resources/Private/Partials/Navigation/Breadcrumb.html");
            f.delete(this);
            return null;
        });
        com.typo3.fluid.linter.update.GraphUpdateServiceTestUtil.flush(getProject());

        VirtualFile eff2 = catalog.effective(ContextId.DEFAULT, ImplementationKind.PARTIAL, "Navigation/Breadcrumb");
        assertNotNull(eff2);
        assertTrue(eff2.getPath(), eff2.getPath().contains("ext_vendor/Resources/Private/Partials/Navigation/Breadcrumb.html"));
    }
}
