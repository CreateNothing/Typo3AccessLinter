package com.typo3.fluid.linter.inspections;

import org.junit.Test;
import org.junit.Ignore;

import java.util.List;

public class PageLanguageInspectionQuickFixTest extends BaseInspectionTest {

    @Ignore("Disabled: html-root enforcement removed; quick-fix availability varies in fragment context")
    @Test
    public void testShouldOfferFix_whenXmlLangWithoutLang() {
        String html = "<html xml:lang=\"en\"><head></head><body><main>Hi</main></body></html>";

        var highlights = highlight("test.html", html, new PageLanguageInspection());
        // No assertions; this test is intentionally disabled
    }
}
