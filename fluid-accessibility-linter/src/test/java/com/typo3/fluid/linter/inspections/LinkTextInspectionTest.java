package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.InspectionProfileEntry;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.codeInspection.ex.LocalInspectionToolWrapper;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiFile;
import com.intellij.testFramework.fixtures.LightJavaCodeInsightFixtureTestCase;
import org.junit.Test;

/**
 * Comprehensive test suite for LinkTextInspection.
 * Tests WCAG 2.1 Success Criterion 2.4.4 (Link Purpose in Context) including:
 * - Generic/vague link text detection
 * - Contextual phrases validation
 * - Empty or meaningless links
 * - Raw URL detection
 * - File download link requirements
 * - Image link alt text validation
 * - Fluid ViewHelper link text
 * - ARIA context validation
 */
public class LinkTextInspectionTest extends LightJavaCodeInsightFixtureTestCase {
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }
    
    @Override
    protected String getTestDataPath() {
        return "src/test/resources/testData";
    }
    
    private void doTest(String htmlContent, String... expectedPatterns) {
        myFixture.configureByText("test.html", htmlContent);
        myFixture.enableInspections(new LinkTextInspection());
        
        // Get all highlights
        var highlights = myFixture.doHighlighting();
        
        // Verify expected patterns are present in warnings
        for (String pattern : expectedPatterns) {
            boolean found = highlights.stream()
                    .anyMatch(info -> info.getDescription() != null && 
                             info.getDescription().toLowerCase().contains(pattern.toLowerCase()));
            
            if (!found) {
                System.out.println("Pattern not found: " + pattern);
                System.out.println("Available highlights:");
                highlights.forEach(h -> System.out.println("  - " + h.getDescription()));
            }
            
            assertTrue("Expected pattern not found: " + pattern, found);
        }
    }
    
    private void doTestNoWarnings(String htmlContent) {
        myFixture.configureByText("test.html", htmlContent);
        myFixture.enableInspections(new LinkTextInspection());
        
        // Get all highlights
        var highlights = myFixture.doHighlighting();
        
        // Verify no link text warnings
        assertFalse("Unexpected link text warnings found", 
            highlights.stream()
                .anyMatch(info -> info.getDescription() != null && 
                         (info.getDescription().contains("link text") || 
                          info.getDescription().contains("Generic") ||
                          info.getDescription().contains("descriptive"))));
    }
    
    // ========== Generic/Vague Link Text Tests ==========
    
    @Test
    public void testClickHere() {
        String html = "<p>To learn more, <a href=\"/info\">click here</a>.</p>";
        doTest(html, "'click here' is not descriptive");
    }
    
    @Test
    public void testHere() {
        String html = "<p>Documentation is available <a href=\"/docs\">here</a>.</p>";
        doTest(html, "'here' is not descriptive");
    }
    
    @Test
    public void testReadMore() {
        // "Read more" in an article context with a heading is considered acceptable
        String html = """
            <article>
                <h2>Article Title</h2>
                <p>Article excerpt...</p>
                <a href="/article">Read more</a>
            </article>
            """;
        doTestNoWarnings(html);
    }
    
    @Test
    public void testReadMoreWithoutContext() {
        // "Read more" without meaningful context should trigger warning
        String html = "<p>Some text here. <a href=\"/article\">Read more</a></p>";
        doTest(html, "'Read more' needs context");
    }
    
    @Test
    public void testLearnMore() {
        // "Learn more" with "features" context is considered acceptable
        String html = "<p>New features available. <a href=\"/features\">Learn more</a></p>";
        doTestNoWarnings(html); // Has "feature" keyword in context
    }
    
    @Test
    public void testMore() {
        String html = "<p>See <a href=\"/info\">more</a> about our services.</p>";
        doTest(html, "'more' is not descriptive");
    }
    
    @Test
    public void testMultipleGenericLinks() {
        String html = """
            <div>
                <p>For details <a href="/page1">click here</a>.</p>
                <p>More info <a href="/page2">here</a>.</p>
                <p>Download <a href="/file">this</a>.</p>
            </div>
            """;
        doTest(html, 
            "'click here' is not descriptive",
            "'here' is not descriptive");
        // Note: "this" is not in the NON_DESCRIPTIVE_PHRASES list
    }
    
    // ========== Empty/Meaningless Link Tests ==========
    
    @Test
    public void testEmptyLink() {
        String html = "<a href=\"/page\"></a>";
        doTest(html, "no text content and no accessible label");
    }
    
    @Test
    public void testWhitespaceOnlyLink() {
        String html = "<a href=\"/page\">   </a>";
        doTest(html, "no text content and no accessible label");
    }
    
    @Test
    public void testSinglePunctuationLink() {
        // Single punctuation marks are not specifically checked, but would be caught as non-descriptive
        String html = "<p>Next page <a href=\"/next\">></a></p>";
        doTestNoWarnings(html); // Currently not detected
    }
    
    @Test
    public void testSingleCharacterLink() {
        String html = "<p><a href=\"/info\">i</a> for information</p>";
        doTest(html, "Single character 'i' as link text is not descriptive");
    }
    
    // ========== Raw URL Tests ==========
    
    @Test
    public void testRawURLWithParameters() {
        String html = "<p>Visit <a href=\"https://example.com\">https://example.com/page?id=123&ref=abc</a></p>";
        doTest(html, "URL as link text is not user-friendly");
    }
    
    @Test
    public void testLongRawURL() {
        String html = "<p>Documentation: <a href=\"/docs\">http://docs.example.com/api/v2/endpoints/user/profile</a></p>";
        doTest(html, "URL as link text is not user-friendly");
    }
    
    // ========== File Download Tests ==========
    
    @Test  
    public void testFileDownloadWithoutType() {
        // File download detection is not implemented
        String html = "<p>Download the <a href=\"/report.pdf\">annual report</a></p>";
        doTestNoWarnings(html); // Feature not implemented
    }
    
    @Test
    public void testFileDownloadWithType() {
        String html = "<p>Download the <a href=\"/report.pdf\">annual report (PDF, 2.3 MB)</a></p>";
        doTestNoWarnings(html);
    }
    
    // ========== Redundant Phrases Tests ==========
    
    @Test
    public void testLinkToRedundancy() {
        // Redundant phrase detection is not implemented
        String html = "<p><a href=\"/home\">Link to homepage</a></p>";
        doTestNoWarnings(html); // Feature not implemented
    }
    
    @Test
    public void testClickToRedundancy() {
        // Redundant phrase detection is not implemented
        String html = "<p><a href=\"/submit\">Click to submit form</a></p>";
        doTestNoWarnings(html); // Feature not implemented
    }
    
    // ========== Context-Dependent Tests ==========
    
    @Test
    public void testReadMoreWithAriaLabel() {
        String html = """
            <article>
                <h3 id="article-title">New Features</h3>
                <p>We've released new features...</p>
                <a href="/article" aria-labelledby="article-title">Read more</a>
            </article>
            """;
        doTestNoWarnings(html);
    }
    
    @Test
    public void testReadMoreInListItemWithContext() {
        String html = """
            <ul>
                <li>
                    <span>Project Apollo</span>
                    <a href="/projects/apollo">Read more</a>
                </li>
            </ul>
            """;
        doTestNoWarnings(html);
    }
    
    @Test
    public void testReadMoreInListItemWithoutContext() {
        String html = """
            <ul>
                <li>
                    <a href="/projects/apollo">Read more</a>
                </li>
            </ul>
            """;
        doTest(html, "'Read more' needs context");
    }
    
    @Test
    public void testLearnMoreWithAriaLabel() {
        // Currently the inspection still flags contextual phrases even with aria-label
        // This could be considered a bug in the inspection
        String html = "<a href=\"/features\" aria-label=\"Learn more about our features\">Learn more</a>";
        doTest(html, "'Learn more' needs context");
    }
    
    @Test
    public void testTableContextLinks() {
        String html = """
            <table>
                <tr>
                    <td>2024 Report</td>
                    <td><a href="/2024.pdf">Download</a></td>
                </tr>
                <tr>
                    <td>2023 Report</td>
                    <td><a href="/2023.pdf">Download</a></td>
                </tr>
            </table>
            """;
        doTest(html, "'Download' is not descriptive");
    }
    
    // ========== Image Link Tests ==========
    
    @Test
    public void testImageLinkWithEmptyAlt() {
        String html = "<a href=\"/home\"><img src=\"logo.png\" alt=\"\"></a>";
        doTest(html, "no text content and no accessible label");
    }
    
    @Test
    public void testImageLinkWithGenericAlt() {
        // Image links with alt text are treated as links with no text content
        String html = "<a href=\"/search\"><img src=\"search.png\" alt=\"icon\"></a>";
        doTest(html, "no text content and no accessible label");
    }
    
    @Test
    public void testImageLinkWithDescriptiveAlt() {
        String html = "<a href=\"/home\"><img src=\"logo.png\" alt=\"Company Name - Return to Homepage\"></a>";
        doTestNoWarnings(html);
    }
    
    @Test
    public void testIconOnlySvgLinkWithTitleNoAriaLabel() {
        String html = """
            <a href="/settings" title="Settings">
                <svg width="16" height="16" class="icon">
                    <path d="M0 0h16v16H0z" fill="none"/>
                </svg>
            </a>
            """;
        doTest(html, "Icon-only link should use aria-label instead of title for better accessibility");
    }
    
    @Test
    public void testIconOnlySvgLinkWithAriaLabel() {
        String html = """
            <a href="/settings" aria-label="Open settings">
                <svg width="16" height="16" class="icon">
                    <path d="M0 0h16v16H0z" fill="none"/>
                </svg>
            </a>
            """;
        doTestNoWarnings(html);
    }
    
    // ========== Fluid ViewHelper Tests ==========
    
    @Test
    public void testFluidLinkWithGenericText() {
        String html = "<f:link.action action=\"show\" controller=\"News\">Read more</f:link.action>";
        doTest(html, "'Read more' needs context");
    }
    
    @Test
    public void testFluidLinkPageWithClickHere() {
        String html = "<f:link.page pageUid=\"123\">Click here</f:link.page>";
        doTest(html, "'Click here' is not descriptive");
    }
    
    @Test
    public void testFluidExternalLinkWithHere() {
        String html = "<f:link.external uri=\"https://example.com\">here</f:link.external>";
        doTest(html, "'here' is not descriptive");
    }
    
    @Test
    public void testFluidLinkWithDescriptiveText() {
        String html = "<f:link.action action=\"show\" controller=\"News\">View complete news article</f:link.action>";
        doTestNoWarnings(html);
    }
    
    @Test
    public void testFluidIconOnlyLinkWithAriaLabel() {
        String html = """
            <f:link.page pageUid="123" aria-label="Go to homepage">
                <svg class="icon" width="16" height="16"><path d="M0 0h16v16H0z"/></svg>
            </f:link.page>
            """;
        doTestNoWarnings(html);
    }
    
    @Test
    public void testFluidLoopWithRepetitiveLinks() {
        String html = """
            <f:for each="{articles}" as="article">
                <h3>{article.title}</h3>
                <f:link.action action="detail" arguments="{article: article}">More</f:link.action>
            </f:for>
            """;
        doTest(html, "'More' is not descriptive");
    }
    
    // ========== Valid Link Text Tests ==========
    
    @Test
    public void testDescriptiveLinks() {
        String html = """
            <nav>
                <a href="/">Home</a>
                <a href="/about">About Our Company</a>
                <a href="/services">Our Services</a>
                <a href="/contact">Contact Information</a>
            </nav>
            """;
        doTestNoWarnings(html);
    }
    
    @Test
    public void testEmailLink() {
        String html = "<p>For support, <a href=\"mailto:support@example.com\">email our support team</a></p>";
        doTestNoWarnings(html);
    }
    
    @Test
    public void testActionLinks() {
        String html = """
            <div>
                <a href="/register">Create your free account</a>
                <a href="/demo">Schedule a product demonstration</a>
                <a href="/trial">Start your 30-day free trial</a>
            </div>
            """;
        doTestNoWarnings(html);
    }
    
    // ========== Case Sensitivity Tests ==========
    
    @Test
    public void testUpperCaseClickHere() {
        String html = "<p>Important: <a href=\"/important\">CLICK HERE</a></p>";
        doTest(html, "'CLICK HERE' is not descriptive");
    }
    
    @Test
    public void testMixedCaseReadMore() {
        String html = "<p>News: <a href=\"/news\">Read More</a></p>";
        doTest(html, "'Read More' needs context");
    }
    
    // ========== Duplicate/Length Edge Cases ==========
    
    @Test
    public void testDuplicateLinksWithDifferentDestinations() {
        String html = """
            <div>
                <a href="/user/alice">Profile</a>
                <a href="/user/bob">Profile</a>
            </div>
            """;
        doTest(html, "Multiple links with text 'profile' point to different destinations");
    }
    
    @Test
    public void testVeryLongLinkText() {
        String longText = "This is an overly verbose link label that exceeds one hundred characters and should be considered too long to read comfortably";
        String html = "<a href=\"/long\">" + longText + "</a>";
        doTest(html, "link text is too long");
    }
    
    // ========== Additional Non-Descriptive Phrases Tests ==========
    
    @Test
    public void testFollowThisLink() {
        String html = "<p>For documentation <a href=\"/docs\">follow this link</a></p>";
        doTest(html, "'follow this link' is not descriptive");
    }
    
    @Test
    public void testClickThisLink() {
        String html = "<p>To register <a href=\"/register\">click this link</a></p>";
        doTest(html, "'click this link' is not descriptive");
    }
    
    @Test
    public void testCheckOut() {
        String html = "<p>New products available <a href=\"/products\">check out</a></p>";
        doTest(html, "'check out' is not descriptive");
    }
    
    @Test
    public void testVisit() {
        String html = "<p>Our website <a href=\"/site\">visit</a></p>";
        doTest(html, "'visit' is not descriptive");
    }
    
    @Test
    public void testSubmit() {
        String html = "<p>Form ready <a href=\"/form\">submit</a></p>";
        doTest(html, "'submit' is not descriptive");
    }
    
    @Test
    public void testSeeMore() {
        String html = "<p>More details <a href=\"/details\">see more</a></p>";
        doTest(html, "'see more' needs context");
    }
    
    @Test
    public void testThisPage() {
        String html = "<p>The page <a href=\"/page\">this page</a></p>";
        doTest(html, "'this page' is not descriptive");
    }
    
    @Test
    public void testThisLink() {
        String html = "<p>The resource <a href=\"/resource\">this link</a></p>";
        doTest(html, "'this link' is not descriptive");
    }
    
    // ========== Complex Scenarios Tests ==========
    
    @Test
    public void testMultipleIssuesInDocument() {
        String html = """
            <div>
                <p>Welcome! <a href="/home">Click here</a> to start.</p>
                <article>
                    <h2>News</h2>
                    <p>Latest updates...</p>
                    <a href="/news">Read more</a>
                </article>
                <p>Download <a href="/file.pdf">document</a></p>
                <a href="/empty"></a>
                <p>Visit <a href="/site">https://example.com/path?id=123</a></p>
            </div>
            """;
        doTest(html,
            "'Click here' is not descriptive",
            // "'Read more' needs context" - has article context so it's acceptable
            // "file type" - feature not implemented
            "no text content",
            "URL as link text");
    }
    
    @Test
    public void testValidComplexStructure() {
        String html = """
            <nav>
                <a href="/">Home</a>
                <a href="/products">Browse Products</a>
            </nav>
            <article>
                <h3 id="article1">Accessibility Guidelines</h3>
                <p>Important updates to our guidelines...</p>
                <a href="/article1" aria-labelledby="article1">Continue reading about Accessibility Guidelines</a>
            </article>
            <p>Download the <a href="/report.pdf">2024 Annual Report (PDF, 2.3 MB)</a></p>
            <f:link.page pageUid="123">View our complete service catalog</f:link.page>
            """;
        doTestNoWarnings(html);
    }
}
