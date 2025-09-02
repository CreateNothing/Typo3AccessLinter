package com.typo3.fluid.linter.inspections;

import org.junit.Test;

import java.util.List;

public class PageLanguageInspectionQuickFixTest extends BaseInspectionTest {

    @Test
    public void testShouldOfferFix_whenHtmlMissingLang() {
        String html = "<html><head></head><body><main>Hi</main></body></html>";

        var highlights = highlight("test.html", html, new PageLanguageInspection());
        List<String> fixes = quickFixTexts();
        // getName(): Add lang="en" attribute
        assertFixesContain(fixes, "Add lang=\"en\" attribute");
    }
}

