package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Test;

/**
 * Comprehensive test suite for AriaRoleInspection.
 * Tests all ARIA role validation scenarios including:
 * - Invalid and abstract roles
 * - Required properties and states
 * - Redundant and conflicting ARIA
 * - Implicit roles and context appropriateness
 * - Semantic consistency and accessibility contracts
 */
public class AriaRoleInspectionTest extends LightJavaCodeInsightFixtureTestCase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }
    
    private void doTest(String htmlContent, String... expectedWarnings) {
        myFixture.configureByText("test.html", htmlContent);
        myFixture.enableInspections(new AriaRoleInspection());
        
        // Get all highlights
        var highlights = myFixture.doHighlighting();
        
        // Verify expected warnings are present
        for (String warning : expectedWarnings) {
            // Debug: print all found warnings
            if (highlights.isEmpty()) {
                System.out.println("No highlights found at all!");
            } else {
                System.out.println("Found " + highlights.size() + " highlights:");
                highlights.forEach(h -> System.out.println("  - " + h.getDescription()));
            }
            
            assertTrue("Expected warning not found: " + warning, 
                highlights.stream()
                    .anyMatch(info -> info.getDescription() != null && 
                             info.getDescription().contains(warning)));
        }
    }
    
    private void doTestNoWarnings(String htmlContent) {
        myFixture.configureByText("test.html", htmlContent);
        myFixture.enableInspections(new AriaRoleInspection());
        
        // Get all highlights
        var highlights = myFixture.doHighlighting();
        
        // Verify no ARIA-related warnings
        assertFalse("Unexpected ARIA warnings found", 
            highlights.stream()
                .anyMatch(info -> info.getDescription() != null && 
                         (info.getDescription().contains("ARIA") || 
                          info.getDescription().contains("role"))));
    }
    
    // ========== Invalid and Abstract Roles Tests ==========
    
    @Test
    public void testInvalidRole() {
        String html = "<div role=\"invalid-role\">Content</div>";
        doTest(html, "Invalid ARIA role 'invalid-role'");
    }
    
    @Test
    public void testAbstractRole() {
        String html = "<div role=\"command\">Content</div>";
        doTest(html, "Abstract ARIA role 'command' should not be used directly");
    }
    
    @Test
    public void testMultipleRoles() {
        String html = "<div role=\"button link\">Content</div>";
        doTest(html, "Multiple ARIA roles are not allowed");
    }
    
    @Test
    public void testValidRole() {
        String html = "<div role=\"button\">Click me</div>";
        doTestNoWarnings(html);
    }
    
    // ========== Required Properties Tests ==========
    
    @Test
    public void testCheckboxMissingAriaChecked() {
        String html = "<div role=\"checkbox\">Option</div>";
        doTest(html, "Role 'checkbox' requires 'aria-checked' property");
    }
    
    @Test
    public void testCheckboxWithAriaChecked() {
        String html = "<div role=\"checkbox\" aria-checked=\"false\">Option</div>";
        doTestNoWarnings(html);
    }
    
    @Test
    public void testSliderMissingRequiredProperties() {
        String html = "<div role=\"slider\">Slider</div>";
        doTest(html, 
            "Role 'slider' requires 'aria-valuenow' property",
            "Role 'slider' requires 'aria-valuemin' property",
            "Role 'slider' requires 'aria-valuemax' property");
    }
    
    @Test
    public void testSliderWithAllProperties() {
        String html = "<div role=\"slider\" aria-valuenow=\"50\" aria-valuemin=\"0\" aria-valuemax=\"100\">Slider</div>";
        doTestNoWarnings(html);
    }
    
    @Test
    public void testDialogMissingLabel() {
        String html = "<div role=\"dialog\">Dialog content</div>";
        doTest(html, "Role 'dialog' requires aria-labelledby or aria-label");
    }
    
    @Test
    public void testDialogWithLabel() {
        String html = "<div role=\"dialog\" aria-label=\"Settings Dialog\">Dialog content</div>";
        doTestNoWarnings(html);
    }
    
    // ========== Redundant ARIA Tests ==========
    
    @Test
    public void testRedundantRoleOnButton() {
        String html = "<button role=\"button\">Click me</button>";
        doTest(html, "Redundant role='button' on <button> element");
    }
    
    @Test
    public void testRedundantRoleOnNav() {
        String html = "<nav role=\"navigation\">Menu</nav>";
        doTest(html, "Redundant role='navigation' on <nav> element");
    }
    
    @Test
    public void testImplicitRoleOnLink() {
        String html = "<a href=\"#\" role=\"link\">Link text</a>";
        doTest(html, "Redundant role='link' on <a> element. This role is implicit");
    }
    
    @Test
    public void testNonRedundantRole() {
        String html = "<div role=\"button\">Click me</div>";
        doTestNoWarnings(html);
    }
    
    // ========== Conflicting ARIA Tests ==========
    
    @Test
    public void testAriaHiddenWithInteractive() {
        String html = "<button aria-hidden=\"true\">Hidden button</button>";
        doTest(html, "Element with aria-hidden='true' should not be interactive");
    }
    
    @Test
    public void testAriaHiddenWithTabindex() {
        String html = "<div aria-hidden=\"true\" tabindex=\"0\">Hidden but focusable</div>";
        doTest(html, "Element with aria-hidden='true' should not be interactive");
    }
    
    @Test
    public void testConflictingImplicitRole() {
        String html = "<button role=\"link\">Conflicting</button>";
        doTest(html, "Role='link' conflicts with implicit role 'button'");
    }
    
    // ========== Input Type Role Consistency Tests ==========
    
    @Test
    public void testInputCheckboxWithWrongRole() {
        String html = "<input type=\"checkbox\" role=\"button\">";
        doTest(html, "Role='button' is inappropriate for input type='checkbox'");
    }
    
    @Test
    public void testInputButtonWithCorrectRole() {
        String html = "<input type=\"button\" role=\"button\" value=\"Click\">";
        doTestNoWarnings(html);
    }
    
    @Test
    public void testInputSearchWithSearchboxRole() {
        String html = "<input type=\"search\" role=\"searchbox\">";
        doTestNoWarnings(html);
    }
    
    // ========== Context Appropriateness Tests ==========
    
    @Test
    public void testNavigationRoleInFormContext() {
        String html = "<form role=\"navigation\">Form content</form>";
        doTest(html, "Role 'navigation' may be inappropriate within <form> context");
    }
    
    @Test
    public void testAppropriateRoleInNavContext() {
        String html = "<nav><ul role=\"menu\"><li>Item</li></ul></nav>";
        doTestNoWarnings(html);
    }
    
    // ========== Semantic Consistency Tests ==========
    
    @Test
    public void testButtonStyledElementWithWrongRole() {
        String html = "<div class=\"btn btn-primary\" onclick=\"doSomething()\" role=\"link\">Click</div>";
        doTest(html, "Element appears to be styled as a button but has role='link'");
    }
    
    @Test
    public void testLinkStyledAsButtonWithoutRole() {
        String html = "<a href=\"#\" class=\"btn btn-primary\">Button Link</a>";
        doTest(html, "Link styled as button should have role='button'");
    }
    
    // ========== Accessibility Contract Tests ==========
    
    @Test
    public void testSemanticElementWithPresentationRole() {
        String html = "<h1 role=\"presentation\">Heading</h1>";
        doTest(html, "Using role='presentation' on semantic element <h1> removes accessibility information");
    }
    
    @Test
    public void testInteractiveElementWithNonInteractiveRole() {
        String html = "<button role=\"article\">Click me</button>";
        doTest(html, "Interactive element <button> with role='article' may not be accessible");
    }
    
    @Test
    public void testPresentationRoleOnDiv() {
        String html = "<div role=\"presentation\">Decorative content</div>";
        doTestNoWarnings(html);
    }
    
    // ========== Complex Scenarios Tests ==========
    
    @Test
    public void testComplexFormWithMultipleIssues() {
        String html = """
            <form>
                <div role=\"checkbox\">Accept Terms</div>
                <button role=\"button\" aria-hidden=\"true\">Submit</button>
                <input type=\"radio\" role=\"checkbox\">
                <nav role=\"navigation\">Form navigation</nav>
            </form>
            """;
        doTest(html,
            "Role 'checkbox' requires 'aria-checked' property",
            "Redundant role='button' on <button> element",
            "Element with aria-hidden='true' should not be interactive",
            "Role='checkbox' is inappropriate for input type='radio'");
    }
    
    @Test
    public void testValidComplexStructure() {
        String html = """
            <div role=\"tablist\" aria-label=\"Settings\">
                <button role=\"tab\" id=\"tab1\" aria-selected=\"true\" aria-controls=\"panel1\">General</button>
                <button role=\"tab\" id=\"tab2\" aria-selected=\"false\" aria-controls=\"panel2\">Advanced</button>
            </div>
            <div role=\"tabpanel\" id=\"panel1\" aria-labelledby=\"tab1\">
                <form>
                    <input type=\"text\" aria-label=\"Name\">
                    <div role=\"checkbox\" aria-checked=\"false\">Enable notifications</div>
                </form>
            </div>
            """;
        doTestNoWarnings(html);
    }
    
    // ========== Fluid Template Specific Tests ==========
    
    @Test
    public void testFluidViewHelperWithRole() {
        String html = """
            <f:link.action action="show" role="button" class="btn">
                Show Details
            </f:link.action>
            """;
        // Should not trigger warnings as Fluid ViewHelpers can have roles
        doTestNoWarnings(html);
    }
    
    @Test
    public void testFluidConditionalWithAriaIssues() {
        String html = """
            <f:if condition="{isActive}">
                <button role="button" aria-hidden="true">Active Button</button>
            </f:if>
            """;
        doTest(html,
            "Redundant role='button' on <button> element",
            "Element with aria-hidden='true' should not be interactive");
    }
}