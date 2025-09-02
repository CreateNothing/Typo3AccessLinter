package com.typo3.fluid.linter.inspections;

import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;

public class AriaLabelValidationInspectionTest extends LightJavaCodeInsightFixtureTestCase {

    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }

    @Test
    public void testAriaLabelValidation() throws IOException {
        myFixture.enableInspections(new AriaLabelValidationInspection());
        String testData = new String(Files.readAllBytes(Paths.get(getTestDataPath() + "/aria-label-validation-test.html")));
        myFixture.configureByText("aria-label-validation-test.html", testData);
        
        List<String> expectedWarnings = List.of(
            "Unnecessary aria-label on non-interactive <p> element. Screen readers already read the content",
            "Unnecessary aria-label on non-interactive <div> element. Screen readers already read the content",
            "Unnecessary aria-label on non-interactive <span> element. Screen readers already read the content",
            "Unnecessary aria-label on non-interactive <h3> element. Screen readers already read the content",
            "Unnecessary aria-label on non-interactive <ul> element. Screen readers already read the content",
            "Redundant aria-label duplicates the element's text content",
            "aria-label 'Button' overrides more descriptive visible text 'Add to Shopping Cart...'",
            "Generic aria-label 'Link' overrides specific visible text",
            "Element has both aria-label and aria-labelledby. Use only one labeling method",
            "Element has both <label> and aria-label. The aria-label will override the visible label",
            "Empty aria-label provides no accessible name",
            "aria-label on element with aria-hidden='true' will be ignored",
            "Redundant aria-label on input[type='submit'] duplicates value attribute",
            "Form input relies only on placeholder for labeling. Add a proper label or aria-label",
            "aria-label duplicates the visible <label> text"
        );

        List<String> actualWarnings = myFixture.doHighlighting().stream()
                .map(h -> h.getDescription())
                .filter(d -> d != null)
                .toList();

        for (String expectedWarning : expectedWarnings) {
            assertTrue("Expected warning not found: " + expectedWarning, actualWarnings.stream().anyMatch(aw -> aw.contains(expectedWarning)));
        }
    }
}
