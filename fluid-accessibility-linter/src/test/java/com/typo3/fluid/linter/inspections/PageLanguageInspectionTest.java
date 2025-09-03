package com.typo3.fluid.linter.inspections;

import org.junit.Test;

public class PageLanguageInspectionTest extends BaseInspectionTest {

    @Test
    public void testShouldNotWarn_whenHtmlMissingLang() {
        String html = "<html><head></head><body></body></html>";
        var highlights = highlight(html, new PageLanguageInspection());
        assertNoHighlightsContaining(highlights, "Missing lang attribute on <html> element");
    }

    @Test
    public void testShouldWarn_whenInvalidLanguageCode() {
        String html = "<html lang=\"english\"><head></head><body></body></html>";
        var highlights = highlight(html, new PageLanguageInspection());
        assertHighlightsContain(highlights, "Invalid language code 'english' on <html>");
    }

    @Test
    public void testShouldWarn_whenXmlLangMismatch() {
        String html = "<html lang=\"en\" xml:lang=\"de\"><head></head><body></body></html>";
        var highlights = highlight(html, new PageLanguageInspection());
        assertHighlightsContain(highlights, "xml:lang='de' doesn't match lang='en'");
    }

    @Test
    public void testShouldInfo_whenLanguageChangeOnElement() {
        String html = "<html lang=\"en\"><head></head><body><p lang=\"de\">Hallo</p></body></html>";
        var highlights = highlight(html, new PageLanguageInspection());
        assertHighlightsContain(highlights, "Language change detected on <p> element (lang='de')");
    }

    @Test
    public void testShouldSkipFluidPartialsAndLayouts() {
        String html = "<html xmlns:f=\"http://typo3.org/ns/TYPO3/CMS/Fluid/ViewHelpers\"><f:section name=\"content\"/></html>";
        var highlights = highlight(html, new PageLanguageInspection());
        assertNoHighlightsContaining(highlights, "lang attribute", "Invalid language code", "Missing <html> element");
    }
}
