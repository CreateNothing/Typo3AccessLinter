package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnhancedSkipLinksInspection extends SkipLinksInspection {
    
    // Enhanced patterns for sophisticated skip link analysis
    private static final Pattern LANDMARK_PATTERN = Pattern.compile(
        "<(?:main|nav|aside|section|header|footer)[^>]*(?:id\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*>|" +
        "<[^>]*role\\s*=\\s*[\"'](main|navigation|complementary|banner|contentinfo)[\"'][^>]*(?:id\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HEADING_PATTERN = Pattern.compile(
        "<h([1-6])[^>]*(?:id\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*>(.*?)</h[1-6]>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern COMPLEX_LAYOUT_PATTERN = Pattern.compile(
        "<[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:sidebar|aside|widget|menu|toolbar|panel)[^\"']*[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern MODAL_DIALOG_PATTERN = Pattern.compile(
        "<[^>]*(?:class\\s*=\\s*[\"'][^\"']*modal[^\"']*[\"']|role\\s*=\\s*[\"']dialog[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FORM_PATTERN = Pattern.compile(
        "<form[^>]*(?:id\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DATA_TABLE_PATTERN = Pattern.compile(
        "<table[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:data|complex)[^\"']*[\"'])?[^>]*>.*?<tbody[^>]*>.*?<tr[^>]*>(?:.*?<td[^>]*>.*?</td>.*?){3,}.*?</tr>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern MULTIMEDIA_CONTENT_PATTERN = Pattern.compile(
        "<(?:video|audio|canvas|iframe|object|embed)[^>]*(?:id\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*>",
        Pattern.CASE_INSENSITIVE
    );

    @NotNull
    @Override
    public String getDisplayName() {
        return "Enhanced skip links and keyboard navigation accessibility";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "EnhancedSkipLinks";
    }

    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        // Call parent implementation first
        super.inspectFile(file, holder);
        
        String content = file.getText();
        
        // Enhanced skip link validations
        analyzePageStructureForSkipLinks(content, file, holder);
        validateSkipLinkTargetAccessibility(content, file, holder);
        checkAdvancedSkipLinkPatterns(content, file, holder);
        validateSkipLinkUsabilityFeatures(content, file, holder);
        analyzeKeyboardNavigationEnhancements(content, file, holder);
        checkResponsiveSkipLinkBehavior(content, file, holder);
    }
    
    private void analyzePageStructureForSkipLinks(String content, PsiFile file, ProblemsHolder holder) {
        // Identify page structure and suggest appropriate skip links
        PageStructureAnalysis analysis = analyzePageStructure(content);
        
        // Find existing skip links
        Map<String, SkipLink> existingSkipLinks = findExistingSkipLinks(content);
        
        // Compare structure needs with existing skip links
        validateSkipLinkCoverage(analysis, existingSkipLinks, file, holder);
        
        // Check for complex layouts that need additional skip links
        if (analysis.hasComplexLayout()) {
            validateComplexLayoutSkipLinks(analysis, existingSkipLinks, content, file, holder);
        }
        
        // Check for data-heavy pages
        if (analysis.hasDataTables() || analysis.hasLongForms()) {
            validateDataPageSkipLinks(analysis, existingSkipLinks, file, holder);
        }
        
        // Check for multimedia content
        if (analysis.hasMultimediaContent()) {
            validateMultimediaSkipLinks(analysis, existingSkipLinks, file, holder);
        }
    }
    
    private void validateSkipLinkTargetAccessibility(String content, PsiFile file, ProblemsHolder holder) {
        Map<String, SkipLink> skipLinks = findExistingSkipLinks(content);
        
        for (SkipLink skipLink : skipLinks.values()) {
            if (skipLink.targetId != null) {
                // Find the target element
                SkipLinkTarget target = findSkipLinkTarget(content, skipLink.targetId);
                
                if (target != null) {
                    validateTargetAccessibility(skipLink, target, content, file, holder);
                } else {
                    registerProblem(holder, file, skipLink.offset, skipLink.offset + 100,
                        String.format("Skip link target '#%s' does not exist in the document", skipLink.targetId),
                        new FixSkipLinkTargetFix(skipLink.targetId));
                }
            }
        }
        
        // Check for potential targets without skip links
        identifyUnreachableTargets(content, skipLinks, file, holder);
    }
    
    private void checkAdvancedSkipLinkPatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Check for skip link grouping patterns
        List<SkipLink> skipLinks = new ArrayList<>(findExistingSkipLinks(content).values());
        
        if (skipLinks.size() > 3) {
            validateSkipLinkGrouping(skipLinks, content, file, holder);
        }
        
        // Check for contextual skip links (within sections)
        checkContextualSkipLinks(content, file, holder);
        
        // Check for skip links in modal dialogs
        validateModalSkipLinks(content, file, holder);
        
        // Check for skip links in complex widgets
        validateWidgetSkipLinks(content, file, holder);
        
        // Check for skip-to-end patterns
        validateSkipToEndPatterns(content, file, holder);
    }
    
    private void validateSkipLinkUsabilityFeatures(String content, PsiFile file, ProblemsHolder holder) {
        Map<String, SkipLink> skipLinks = findExistingSkipLinks(content);
        
        for (SkipLink skipLink : skipLinks.values()) {
            // Check visibility behavior
            validateSkipLinkVisibility(skipLink, content, file, holder);
            
            // Check focus management
            validateSkipLinkFocusManagement(skipLink, content, file, holder);
            
            // Check descriptive text
            validateSkipLinkDescription(skipLink, file, holder);
            
            // Check keyboard shortcuts
            validateSkipLinkKeyboardShortcuts(skipLink, content, file, holder);
        }
        
        // Check for user preferences support
        validateSkipLinkUserPreferences(content, file, holder);
    }
    
    private void analyzeKeyboardNavigationEnhancements(String content, PsiFile file, ProblemsHolder holder) {
        // Check for enhanced keyboard navigation patterns
        validateFocusTrapping(content, file, holder);
        validateFocusIndicators(content, file, holder);
        validateKeyboardShortcuts(content, file, holder);
        validateTabOrder(content, file, holder);
        validateAccessKeys(content, file, holder);
    }
    
    private void checkResponsiveSkipLinkBehavior(String content, PsiFile file, ProblemsHolder holder) {
        boolean isMobileOptimized = content.contains("viewport") && content.contains("device-width");
        
        if (isMobileOptimized) {
            validateMobileSkipLinks(content, file, holder);
            validateTouchAccessibility(content, file, holder);
            validateScreenReaderCompatibility(content, file, holder);
        }
    }
    
    // Helper classes and methods
    private static class PageStructureAnalysis {
        final boolean hasNavigation;
        final boolean hasMainContent;
        final boolean hasAside;
        final boolean hasMultipleHeadings;
        final boolean hasComplexLayout;
        final boolean hasDataTables;
        final boolean hasLongForms;
        final boolean hasMultimediaContent;
        final List<String> landmarks;
        final List<String> headings;
        final int contentComplexity;
        
        PageStructureAnalysis(boolean hasNavigation, boolean hasMainContent, boolean hasAside,
                            boolean hasMultipleHeadings, boolean hasComplexLayout, boolean hasDataTables,
                            boolean hasLongForms, boolean hasMultimediaContent, List<String> landmarks,
                            List<String> headings, int contentComplexity) {
            this.hasNavigation = hasNavigation;
            this.hasMainContent = hasMainContent;
            this.hasAside = hasAside;
            this.hasMultipleHeadings = hasMultipleHeadings;
            this.hasComplexLayout = hasComplexLayout;
            this.hasDataTables = hasDataTables;
            this.hasLongForms = hasLongForms;
            this.hasMultimediaContent = hasMultimediaContent;
            this.landmarks = landmarks;
            this.headings = headings;
            this.contentComplexity = contentComplexity;
        }
        
        boolean hasComplexLayout() { return hasComplexLayout; }
        boolean hasDataTables() { return hasDataTables; }
        boolean hasLongForms() { return hasLongForms; }
        boolean hasMultimediaContent() { return hasMultimediaContent; }
    }
    
    private static class SkipLink {
        final int offset;
        final String href;
        final String targetId;
        final String text;
        final boolean isVisible;
        final String accessKey;
        
        SkipLink(int offset, String href, String targetId, String text, boolean isVisible, String accessKey) {
            this.offset = offset;
            this.href = href;
            this.targetId = targetId;
            this.text = text;
            this.isVisible = isVisible;
            this.accessKey = accessKey;
        }
    }
    
    private static class SkipLinkTarget {
        final int offset;
        final String id;
        final String tagName;
        final String role;
        final boolean isFocusable;
        final String ariaLabel;
        
        SkipLinkTarget(int offset, String id, String tagName, String role, boolean isFocusable, String ariaLabel) {
            this.offset = offset;
            this.id = id;
            this.tagName = tagName;
            this.role = role;
            this.isFocusable = isFocusable;
            this.ariaLabel = ariaLabel;
        }
    }
    
    private PageStructureAnalysis analyzePageStructure(String content) {
        boolean hasNavigation = content.contains("<nav") || content.contains("role=\"navigation\"");
        boolean hasMainContent = content.contains("<main") || content.contains("role=\"main\"");
        boolean hasAside = content.contains("<aside") || content.contains("role=\"complementary\"");
        
        // Count headings
        Matcher headingMatcher = HEADING_PATTERN.matcher(content);
        List<String> headings = new ArrayList<>();
        while (headingMatcher.find()) {
            headings.add(headingMatcher.group(3));
        }
        boolean hasMultipleHeadings = headings.size() > 3;
        
        // Check for complex layout indicators
        boolean hasComplexLayout = COMPLEX_LAYOUT_PATTERN.matcher(content).find();
        
        // Check for data tables
        boolean hasDataTables = DATA_TABLE_PATTERN.matcher(content).find();
        
        // Check for long forms (more than 10 fields)
        int formFieldCount = countMatches(content, Pattern.compile("<(?:input|select|textarea)[^>]*>"));
        boolean hasLongForms = formFieldCount > 10;
        
        // Check for multimedia content
        boolean hasMultimediaContent = MULTIMEDIA_CONTENT_PATTERN.matcher(content).find();
        
        // Extract landmarks
        List<String> landmarks = extractLandmarks(content);
        
        // Calculate content complexity score
        int contentComplexity = calculateContentComplexity(content);
        
        return new PageStructureAnalysis(hasNavigation, hasMainContent, hasAside, hasMultipleHeadings,
                                       hasComplexLayout, hasDataTables, hasLongForms, hasMultimediaContent,
                                       landmarks, headings, contentComplexity);
    }
    
    private Map<String, SkipLink> findExistingSkipLinks(String content) {
        Map<String, SkipLink> skipLinks = new HashMap<>();
        
        // Find links that appear to be skip links
        Pattern skipLinkPattern = Pattern.compile(
            "<a[^>]*href\\s*=\\s*[\"']#([^\"']+)[\"'][^>]*(?:accesskey\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*>(.*?)</a>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = skipLinkPattern.matcher(content);
        while (matcher.find()) {
            String href = matcher.group(0);
            String targetId = matcher.group(1);
            String accessKey = matcher.group(2);
            String text = matcher.group(3).replaceAll("<[^>]+>", "").trim();
            int offset = matcher.start();
            
            // Check if this looks like a skip link
            if (isSkipLink(text, href)) {
                boolean isVisible = !href.toLowerCase().contains("sr-only") && 
                                  !href.toLowerCase().contains("visually-hidden");
                
                skipLinks.put(targetId, new SkipLink(offset, href, targetId, text, isVisible, accessKey));
            }
        }
        
        return skipLinks;
    }
    
    private boolean isSkipLink(String text, String href) {
        String lowerText = text.toLowerCase();
        String lowerHref = href.toLowerCase();
        
        return lowerText.contains("skip") || lowerText.contains("jump") || 
               lowerHref.contains("skip") || lowerText.contains("go to");
    }
    
    private void validateSkipLinkCoverage(PageStructureAnalysis analysis, Map<String, SkipLink> skipLinks,
                                        PsiFile file, ProblemsHolder holder) {
        
        List<String> missingSkipLinks = new ArrayList<>();
        
        if (analysis.hasNavigation && !hasSkipLinkTo(skipLinks, "navigation", "nav", "menu")) {
            missingSkipLinks.add("navigation");
        }
        
        if (analysis.hasMainContent && !hasSkipLinkTo(skipLinks, "main", "content", "primary")) {
            missingSkipLinks.add("main content");
        }
        
        if (analysis.hasAside && !hasSkipLinkTo(skipLinks, "aside", "sidebar", "complementary")) {
            missingSkipLinks.add("sidebar/aside content");
        }
        
        if (analysis.hasMultipleHeadings && !hasSkipLinkTo(skipLinks, "heading", "h1", "h2")) {
            missingSkipLinks.add("main heading");
        }
        
        if (!missingSkipLinks.isEmpty()) {
            registerProblem(holder, file, 0, 100,
                String.format("Page structure suggests need for skip links to: %s", String.join(", ", missingSkipLinks)),
                new AddMissingSkipLinksFix(missingSkipLinks));
        }
    }
    
    private boolean hasSkipLinkTo(Map<String, SkipLink> skipLinks, String... targetTerms) {
        for (SkipLink skipLink : skipLinks.values()) {
            String lowerText = skipLink.text.toLowerCase();
            String lowerTargetId = skipLink.targetId != null ? skipLink.targetId.toLowerCase() : "";
            
            for (String term : targetTerms) {
                if (lowerText.contains(term) || lowerTargetId.contains(term)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    private void validateComplexLayoutSkipLinks(PageStructureAnalysis analysis, Map<String, SkipLink> skipLinks,
                                              String content, PsiFile file, ProblemsHolder holder) {
        
        // For complex layouts, check for section-level skip links
        if (analysis.contentComplexity > 7 && skipLinks.size() < 3) {
            registerProblem(holder, file, 0, 100,
                String.format("Complex page layout (complexity: %d) should provide multiple skip link options for efficient navigation",
                    analysis.contentComplexity),
                new AddSectionSkipLinksFix());
        }
        
        // Check for skip links within long sections
        validateIntraSectionSkipLinks(content, file, holder);
    }
    
    private void validateDataPageSkipLinks(PageStructureAnalysis analysis, Map<String, SkipLink> skipLinks,
                                         PsiFile file, ProblemsHolder holder) {
        
        if (analysis.hasDataTables && !hasSkipLinkTo(skipLinks, "table", "data", "results")) {
            registerProblem(holder, file, 0, 100,
                "Page with data tables should provide skip links to navigate around large data sets",
                new AddDataSkipLinksFix());
        }
        
        if (analysis.hasLongForms && !hasSkipLinkTo(skipLinks, "form", "submit", "button")) {
            registerProblem(holder, file, 0, 100,
                "Long forms should provide skip links to form sections and submit buttons",
                new AddFormSkipLinksFix());
        }
    }
    
    private void validateMultimediaSkipLinks(PageStructureAnalysis analysis, Map<String, SkipLink> skipLinks,
                                           PsiFile file, ProblemsHolder holder) {
        
        if (analysis.hasMultimediaContent && !hasSkipLinkTo(skipLinks, "video", "audio", "media", "player")) {
            registerProblem(holder, file, 0, 100,
                "Pages with multimedia content should provide skip links to bypass or access media controls",
                new AddMediaSkipLinksFix());
        }
    }
    
    private SkipLinkTarget findSkipLinkTarget(String content, String targetId) {
        // Find element with matching ID
        Pattern targetPattern = Pattern.compile(
            "<([a-zA-Z]+)[^>]*id\\s*=\\s*[\"']" + Pattern.quote(targetId) + "[\"'][^>]*(?:role\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*(?:aria-label\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*(?:tabindex\\s*=\\s*[\"']([^\"']+)[\"'])?[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = targetPattern.matcher(content);
        if (matcher.find()) {
            String tagName = matcher.group(1);
            String role = matcher.group(2);
            String ariaLabel = matcher.group(3);
            String tabindex = matcher.group(4);
            
            boolean isFocusable = isFocusableElement(tagName) || 
                                (tabindex != null && !tabindex.equals("-1"));
            
            return new SkipLinkTarget(matcher.start(), targetId, tagName, role, isFocusable, ariaLabel);
        }
        
        return null;
    }
    
    private boolean isFocusableElement(String tagName) {
        Set<String> focusableElements = new HashSet<>(Arrays.asList(
            "a", "button", "input", "select", "textarea", "area", "object", "embed"
        ));
        return focusableElements.contains(tagName.toLowerCase());
    }
    
    private void validateTargetAccessibility(SkipLink skipLink, SkipLinkTarget target, String content,
                                           PsiFile file, ProblemsHolder holder) {
        
        // Check if target is focusable
        if (!target.isFocusable && !target.tagName.equals("main") && !target.tagName.equals("section")) {
            registerProblem(holder, file, target.offset, target.offset + 100,
                String.format("Skip link target '%s' should be focusable. Add tabindex='0' or use focusable element",
                    target.id),
                new MakeTargetFocusableFix(target.id));
        }
        
        // Check if target has appropriate labeling
        if (target.ariaLabel == null && (target.tagName.equals("div") || target.tagName.equals("section"))) {
            registerProblem(holder, file, target.offset, target.offset + 100,
                String.format("Skip link target '%s' should have aria-label for screen reader context",
                    target.id),
                new AddTargetLabelFix(target.id));
        }
        
        // Check target visibility
        validateTargetVisibility(target, content, file, holder);
    }
    
    private void validateTargetVisibility(SkipLinkTarget target, String content, PsiFile file, ProblemsHolder holder) {
        // Find target element content
        int targetStart = target.offset;
        int targetEnd = findElementEnd(content, targetStart);
        String targetContent = content.substring(targetStart, Math.min(targetEnd, content.length()));
        
        // Check if target might be hidden
        boolean mightBeHidden = targetContent.contains("display: none") ||
                               targetContent.contains("visibility: hidden") ||
                               targetContent.contains("hidden");
        
        if (mightBeHidden) {
            registerProblem(holder, file, target.offset, target.offset + 100,
                String.format("Skip link target '%s' appears to be hidden. Ensure it's visible when focused",
                    target.id),
                new EnsureTargetVisibilityFix(target.id));
        }
    }
    
    private void identifyUnreachableTargets(String content, Map<String, SkipLink> skipLinks,
                                          PsiFile file, ProblemsHolder holder) {
        
        // Find important content sections without skip links
        List<String> landmarks = extractLandmarks(content);
        
        for (String landmark : landmarks) {
            if (!hasSkipLinkToLandmark(skipLinks, landmark)) {
                registerProblem(holder, file, 0, 100,
                    String.format("Important landmark '%s' is not accessible via skip links", landmark),
                    new AddSkipLinkToLandmarkFix(landmark));
            }
        }
    }
    
    private List<String> extractLandmarks(String content) {
        List<String> landmarks = new ArrayList<>();
        
        Matcher landmarkMatcher = LANDMARK_PATTERN.matcher(content);
        while (landmarkMatcher.find()) {
            String id = landmarkMatcher.group(1);
            String role = landmarkMatcher.group(2);
            String altId = landmarkMatcher.group(3);
            
            if (id != null) {
                landmarks.add(id);
            } else if (altId != null) {
                landmarks.add(altId);
            } else if (role != null) {
                landmarks.add(role);
            }
        }
        
        return landmarks;
    }
    
    private boolean hasSkipLinkToLandmark(Map<String, SkipLink> skipLinks, String landmark) {
        for (SkipLink skipLink : skipLinks.values()) {
            if (skipLink.targetId != null && skipLink.targetId.equals(landmark)) {
                return true;
            }
        }
        return false;
    }
    
    private int calculateContentComplexity(String content) {
        int complexity = 0;
        
        // Count navigation elements
        complexity += countMatches(content, Pattern.compile("<nav[^>]*>"));
        complexity += countMatches(content, Pattern.compile("role=\"navigation\""));
        
        // Count sections and articles
        complexity += countMatches(content, Pattern.compile("<(?:section|article)[^>]*>"));
        
        // Count forms
        complexity += countMatches(content, Pattern.compile("<form[^>]*>"));
        
        // Count data tables
        complexity += countMatches(content, DATA_TABLE_PATTERN);
        
        // Count interactive elements
        complexity += countMatches(content, Pattern.compile("<(?:button|input|select|textarea)[^>]*>")) / 5;
        
        // Count headings
        complexity += countMatches(content, HEADING_PATTERN) / 2;
        
        return complexity;
    }
    
    private int countMatches(String content, Pattern pattern) {
        Matcher matcher = pattern.matcher(content);
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        return count;
    }
    
    private void validateSkipLinkVisibility(SkipLink skipLink, String content, PsiFile file, ProblemsHolder holder) {
        if (!skipLink.isVisible) {
            // Check if it becomes visible on focus
            boolean hasVisibleOnFocus = content.contains(":focus") && 
                                       (content.contains("position: static") || content.contains("clip: auto"));
            
            if (!hasVisibleOnFocus) {
                registerProblem(holder, file, skipLink.offset, skipLink.offset + 100,
                    "Hidden skip link should become visible when focused",
                    new MakeSkipLinkVisibleOnFocusFix());
            }
        }
    }
    
    private void validateSkipLinkFocusManagement(SkipLink skipLink, String content, PsiFile file, ProblemsHolder holder) {
        // Check if focus is properly managed when skip link is activated
        boolean hasFocusManagement = content.contains("focus()") || content.contains("tabindex");
        
        if (!hasFocusManagement) {
            registerProblem(holder, file, skipLink.offset, skipLink.offset + 100,
                "Skip link should properly manage focus when activated",
                new AddFocusManagementFix());
        }
    }
    
    private void validateSkipLinkDescription(SkipLink skipLink, PsiFile file, ProblemsHolder holder) {
        String lowerText = skipLink.text.toLowerCase();
        
        // Check for vague descriptions
        if (lowerText.equals("skip") || lowerText.equals("skip navigation") && lowerText.length() < 15) {
            registerProblem(holder, file, skipLink.offset, skipLink.offset + 100,
                String.format("Skip link text '%s' could be more descriptive about what content is being skipped",
                    skipLink.text),
                new ImproveSkipLinkDescriptionFix(skipLink.text));
        }
        
        // Check for consistent terminology
        if (lowerText.contains("jump to") && !lowerText.contains("skip")) {
            registerProblem(holder, file, skipLink.offset, skipLink.offset + 100,
                "Consider using consistent terminology across skip links (e.g., 'Skip to...' vs 'Jump to...')",
                new StandardizeSkipLinkTerminologyFix());
        }
    }
    
    private void validateSkipLinkKeyboardShortcuts(SkipLink skipLink, String content, PsiFile file, ProblemsHolder holder) {
        if (skipLink.accessKey == null) {
            registerProblem(holder, file, skipLink.offset, skipLink.offset + 100,
                "Consider adding accesskey attribute to skip link for keyboard shortcut access",
                new AddAccessKeyToSkipLinkFix());
        } else {
            // Check for conflicting access keys
            validateAccessKeyConflicts(skipLink.accessKey, content, file, holder, skipLink.offset);
        }
    }
    
    private void validateAccessKeyConflicts(String accessKey, String content, PsiFile file, 
                                          ProblemsHolder holder, int offset) {
        
        Pattern accessKeyPattern = Pattern.compile("accesskey\\s*=\\s*[\"']" + Pattern.quote(accessKey) + "[\"']");
        Matcher matcher = accessKeyPattern.matcher(content);
        
        int count = 0;
        while (matcher.find()) {
            count++;
        }
        
        if (count > 1) {
            registerProblem(holder, file, offset, offset + 100,
                String.format("Access key '%s' is used multiple times. Each access key should be unique", accessKey),
                new ResolveAccessKeyConflictFix(accessKey));
        }
    }
    
    
    // Enhanced Quick Fixes
    private static class AddMissingSkipLinksFix implements LocalQuickFix {
        private final List<String> missingTargets;
        
        AddMissingSkipLinksFix(List<String> missingTargets) {
            this.missingTargets = missingTargets;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add skip links for: " + String.join(", ", missingTargets);
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add missing skip links
        }
    }
    
    private static class AddSectionSkipLinksFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add section-level skip links for complex layout";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add section skip links
        }
    }
    
    private static class AddDataSkipLinksFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add skip links for data tables";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add data skip links
        }
    }
    
    private static class AddFormSkipLinksFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add skip links for form sections";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add form skip links
        }
    }
    
    private static class AddMediaSkipLinksFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add skip links for multimedia content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add media skip links
        }
    }
    
    private static class FixSkipLinkTargetFix implements LocalQuickFix {
        private final String targetId;
        
        FixSkipLinkTargetFix(String targetId) {
            this.targetId = targetId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix skip link target: " + targetId;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would fix skip link target
        }
    }
    
    private static class MakeTargetFocusableFix implements LocalQuickFix {
        private final String targetId;
        
        MakeTargetFocusableFix(String targetId) {
            this.targetId = targetId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Make target focusable: " + targetId;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would make target focusable
        }
    }
    
    private static class AddTargetLabelFix implements LocalQuickFix {
        private final String targetId;
        
        AddTargetLabelFix(String targetId) {
            this.targetId = targetId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-label to target: " + targetId;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add target label
        }
    }
    
    private static class EnsureTargetVisibilityFix implements LocalQuickFix {
        private final String targetId;
        
        EnsureTargetVisibilityFix(String targetId) {
            this.targetId = targetId;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Ensure target visibility: " + targetId;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would ensure target visibility
        }
    }
    
    private static class AddSkipLinkToLandmarkFix implements LocalQuickFix {
        private final String landmark;
        
        AddSkipLinkToLandmarkFix(String landmark) {
            this.landmark = landmark;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add skip link to landmark: " + landmark;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add skip link to landmark
        }
    }
    
    private static class MakeSkipLinkVisibleOnFocusFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Make skip link visible on focus";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would make skip link visible on focus
        }
    }
    
    private static class AddFocusManagementFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add focus management to skip link";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add focus management
        }
    }
    
    private static class ImproveSkipLinkDescriptionFix implements LocalQuickFix {
        private final String currentText;
        
        ImproveSkipLinkDescriptionFix(String currentText) {
            this.currentText = currentText;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Improve skip link description: '" + currentText + "'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would improve description
        }
    }
    
    private static class StandardizeSkipLinkTerminologyFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Standardize skip link terminology";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would standardize terminology
        }
    }
    
    private static class AddAccessKeyToSkipLinkFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add access key to skip link";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add access key
        }
    }
    
    private static class ResolveAccessKeyConflictFix implements LocalQuickFix {
        private final String accessKey;
        
        ResolveAccessKeyConflictFix(String accessKey) {
            this.accessKey = accessKey;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Resolve access key conflict: " + accessKey;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would resolve access key conflict
        }
    }
    
    // Stub implementations for remaining validation methods
    private void validateSkipLinkGrouping(List<SkipLink> skipLinks, String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate skip link grouping
    }
    
    private void checkContextualSkipLinks(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would check contextual skip links
    }
    
    private void validateModalSkipLinks(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate modal skip links
    }
    
    private void validateWidgetSkipLinks(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate widget skip links
    }
    
    private void validateSkipToEndPatterns(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate skip-to-end patterns
    }
    
    private void validateSkipLinkUserPreferences(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate user preferences
    }
    
    private void validateFocusTrapping(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate focus trapping
    }
    
    private void validateFocusIndicators(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate focus indicators
    }
    
    private void validateKeyboardShortcuts(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate keyboard shortcuts
    }
    
    private void validateTabOrder(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate tab order
    }
    
    private void validateAccessKeys(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate access keys
    }
    
    private void validateMobileSkipLinks(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate mobile skip links
    }
    
    private void validateTouchAccessibility(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate touch accessibility
    }
    
    private void validateScreenReaderCompatibility(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate screen reader compatibility
    }
    
    private void validateIntraSectionSkipLinks(String content, PsiFile file, ProblemsHolder holder) {
        // Implementation would validate intra-section skip links
    }
}