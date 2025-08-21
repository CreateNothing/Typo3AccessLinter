package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.typo3.fluid.linter.utils.AccessibilityUtils;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced inspection for heading hierarchy with context-aware intelligence.
 * Features:
 * - HTML5 sectioning elements awareness (section, article, aside) where H1 resets are valid
 * - Fluid layout/section structure understanding  
 * - Navigation vs content heading distinction
 * - Comprehensive hierarchy repair suggestions
 * - Sectioning root context analysis
 */
public class HeadingHierarchyInspection extends FluidAccessibilityInspection {
    
    // Pattern to match heading tags (h1-h6)
    private static final Pattern HEADING_PATTERN = Pattern.compile(
        "<h([1-6])\\s*[^>]*>(.*?)</h\\1>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Pattern to detect HTML5 sectioning elements that create new sectioning contexts
    private static final Pattern SECTIONING_PATTERN = Pattern.compile(
        "<(section|article|aside|nav)\\s*[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern to detect Fluid layout/section markers
    private static final Pattern FLUID_SECTION_PATTERN = Pattern.compile(
        "<f:section\\s+name\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>|" +
        "<f:layout\\s+name\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>|" +
        "<f:render\\s+section\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    // Pattern to detect navigation contexts
    private static final Pattern NAVIGATION_PATTERN = Pattern.compile(
        "<nav\\b[^>]*>|" +
        "<(?:div|section)\\s+[^>]*(?:class|role)\\s*=\\s*[\"'][^\"']*(?:nav|menu|breadcrumb)[^\"']*[\"'][^>]*>|" +
        "<ul\\s+[^>]*class\\s*=\\s*[\"'][^\"']*(?:nav|menu)[^\"']*[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Pattern to detect if content is only whitespace or contains only images without alt text
    private static final Pattern EMPTY_CONTENT_PATTERN = Pattern.compile(
        "^\\s*$|^\\s*<img[^>]*>\\s*$",
        Pattern.CASE_INSENSITIVE
    );
    
    // Navigation-related heading patterns
    private static final Set<String> NAVIGATION_HEADINGS = new HashSet<>(Arrays.asList(
        "navigation", "menu", "breadcrumb", "breadcrumbs", "sitemap", "site map",
        "table of contents", "contents", "toc", "skip links", "skip navigation"
    ));
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Heading hierarchy issues";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "HeadingHierarchy";
    }
    
    @NotNull
    @Override
    public String getGroupDisplayName() {
        return "Fluid Accessibility";
    }
    
    @Override
    public boolean isEnabledByDefault() {
        return true;
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Analyze document structure first
        DocumentStructure structure = analyzeDocumentStructure(content);
        List<EnhancedHeading> headings = collectEnhancedHeadings(content, structure);
        
        if (headings.isEmpty()) {
            return;
        }
        
        // Enhanced checks with context awareness
        checkMultipleH1sWithSectioning(headings, structure, file, holder);
        checkSkippedLevelsWithContext(headings, structure, file, holder);
        checkEmptyHeadings(headings, file, holder);
        checkGenericHeadings(headings, file, holder);
        checkNavigationHeadingContext(headings, structure, file, holder);
        checkFluidSectionHeadings(headings, structure, file, holder);
    }
    
    private DocumentStructure analyzeDocumentStructure(String content) {
        DocumentStructure structure = new DocumentStructure();
        
        // Find HTML5 sectioning elements
        Matcher sectionMatcher = SECTIONING_PATTERN.matcher(content);
        while (sectionMatcher.find()) {
            String elementType = sectionMatcher.group(1);
            structure.addSectioningElement(sectionMatcher.start(), elementType);
        }
        
        // Find Fluid sections
        Matcher fluidMatcher = FLUID_SECTION_PATTERN.matcher(content);
        while (fluidMatcher.find()) {
            String sectionName = fluidMatcher.group(1) != null ? fluidMatcher.group(1) : 
                                fluidMatcher.group(2) != null ? fluidMatcher.group(2) : fluidMatcher.group(3);
            structure.addFluidSection(fluidMatcher.start(), sectionName);
        }
        
        // Find navigation contexts
        Matcher navMatcher = NAVIGATION_PATTERN.matcher(content);
        while (navMatcher.find()) {
            structure.addNavigationContext(navMatcher.start(), navMatcher.end());
        }
        
        return structure;
    }
    
    private List<EnhancedHeading> collectEnhancedHeadings(String content, DocumentStructure structure) {
        List<EnhancedHeading> headings = new ArrayList<>();
        Matcher matcher = HEADING_PATTERN.matcher(content);
        
        while (matcher.find()) {
            int level = Integer.parseInt(matcher.group(1));
            String headingContent = matcher.group(2);
            int offset = matcher.start();
            
            // Extract text content (remove HTML tags)
            String textContent = AccessibilityUtils.extractTextContent(headingContent);
            
            // Determine context
            HeadingContext context = determineHeadingContext(offset, structure, textContent);
            
            headings.add(new EnhancedHeading(level, textContent, headingContent, offset, context));
        }
        
        // Sort by offset to maintain document order
        headings.sort(Comparator.comparingInt(h -> h.offset));
        
        return headings;
    }
    
    private HeadingContext determineHeadingContext(int offset, DocumentStructure structure, String textContent) {
        // Check if in navigation
        if (structure.isInNavigationContext(offset)) {
            return HeadingContext.NAVIGATION;
        }
        
        // Check if in sectioning element
        if (structure.isInSectioningElement(offset)) {
            return HeadingContext.SECTIONING_CONTENT;
        }
        
        // Check if in Fluid section
        if (structure.isInFluidSection(offset)) {
            return HeadingContext.FLUID_SECTION;
        }
        
        // Check if text suggests navigation
        String lowerText = textContent.toLowerCase().trim();
        if (NAVIGATION_HEADINGS.contains(lowerText)) {
            return HeadingContext.NAVIGATION;
        }
        
        return HeadingContext.MAIN_CONTENT;
    }
    
    private void checkMultipleH1sWithSectioning(List<EnhancedHeading> headings, DocumentStructure structure, 
                                                PsiFile file, ProblemsHolder holder) {
        Map<HeadingContext, List<EnhancedHeading>> h1sByContext = new HashMap<>();
        
        // Group H1s by context
        for (EnhancedHeading heading : headings) {
            if (heading.level == 1) {
                h1sByContext.computeIfAbsent(heading.context, k -> new ArrayList<>()).add(heading);
            }
        }
        
        // Check main content - should have only one H1
        List<EnhancedHeading> mainH1s = h1sByContext.getOrDefault(HeadingContext.MAIN_CONTENT, Collections.emptyList());
        if (mainH1s.size() > 1) {
            for (int i = 1; i < mainH1s.size(); i++) {
                EnhancedHeading heading = mainH1s.get(i);
                registerProblem(file, holder, heading.offset,
                    "Multiple H1 elements in main content - only one H1 should exist per page",
                    ProblemHighlightType.ERROR,
                    new ChangeHeadingLevelQuickFix(1, 2));
            }
        }
        
        // Check sectioning content - each section can have an H1
        List<EnhancedHeading> sectioningH1s = h1sByContext.getOrDefault(HeadingContext.SECTIONING_CONTENT, Collections.emptyList());
        if (!sectioningH1s.isEmpty() && !mainH1s.isEmpty()) {
            // If we have H1 in main content and in sections, suggest converting sectioning H1s
            for (EnhancedHeading heading : sectioningH1s) {
                registerProblem(file, holder, heading.offset,
                    "H1 in sectioning element when main H1 exists. Consider H2 for section heading",
                    ProblemHighlightType.WARNING,
                    new ChangeHeadingLevelQuickFix(1, 2));
            }
        }
        
        // Navigation headings can be H1 but suggest alternatives
        List<EnhancedHeading> navH1s = h1sByContext.getOrDefault(HeadingContext.NAVIGATION, Collections.emptyList());
        if (!navH1s.isEmpty() && (!mainH1s.isEmpty() || !sectioningH1s.isEmpty())) {
            for (EnhancedHeading heading : navH1s) {
                registerProblem(file, holder, heading.offset,
                    "Navigation heading as H1 may compete with main content. Consider H2 or aria-label on nav element",
                    ProblemHighlightType.WARNING,
                    new ImproveNavigationHeadingFix());
            }
        }
    }
    
    private void checkSkippedLevelsWithContext(List<EnhancedHeading> headings, DocumentStructure structure,
                                              PsiFile file, ProblemsHolder holder) {
        if (headings.size() < 2) return;
        
        // Group headings by context for separate hierarchy analysis
        Map<HeadingContext, List<EnhancedHeading>> headingsByContext = new HashMap<>();
        for (EnhancedHeading heading : headings) {
            headingsByContext.computeIfAbsent(heading.context, k -> new ArrayList<>()).add(heading);
        }
        
        // Check each context separately
        for (Map.Entry<HeadingContext, List<EnhancedHeading>> entry : headingsByContext.entrySet()) {
            HeadingContext context = entry.getKey();
            List<EnhancedHeading> contextHeadings = entry.getValue();
            
            if (context == HeadingContext.NAVIGATION) {
                // Navigation headings have more flexibility
                checkNavigationHierarchy(contextHeadings, file, holder);
            } else {
                // Standard hierarchy rules apply
                checkStandardHierarchy(contextHeadings, context, file, holder);
            }
        }
        
        // Also check overall document flow ignoring navigation
        List<EnhancedHeading> contentHeadings = headings.stream()
            .filter(h -> h.context != HeadingContext.NAVIGATION)
            .collect(java.util.stream.Collectors.toList());
        
        if (contentHeadings.size() > 1) {
            checkOverallDocumentHierarchy(contentHeadings, file, holder);
        }
    }
    
    private void checkStandardHierarchy(List<EnhancedHeading> headings, HeadingContext context, 
                                       PsiFile file, ProblemsHolder holder) {
        for (int i = 0; i < headings.size(); i++) {
            EnhancedHeading current = headings.get(i);
            
            // Find the nearest previous heading with lower level in same context
            Integer parentLevel = null;
            for (int j = i - 1; j >= 0; j--) {
                if (headings.get(j).level < current.level) {
                    parentLevel = headings.get(j).level;
                    break;
                }
            }
            
            if (parentLevel != null) {
                // Check if we're skipping levels
                if (current.level - parentLevel > 1) {
                    int expectedLevel = parentLevel + 1;
                    
                    String contextMsg = getContextMessage(context);
                    registerProblem(file, holder, current.offset,
                        String.format("Heading level skipped in %s: H%d follows H%d (expected H%d)",
                            contextMsg, current.level, parentLevel, expectedLevel),
                        ProblemHighlightType.WARNING,
                        new ContextAwareHeadingFix(current.level, expectedLevel, context));
                }
            } else if (current.level > 2 && context == HeadingContext.SECTIONING_CONTENT) {
                // In sectioning content, starting with H3+ without H2 is problematic
                registerProblem(file, holder, current.offset,
                    String.format("Section starts with H%d - consider H1 for section title or H2 to continue hierarchy",
                        current.level),
                    ProblemHighlightType.WARNING,
                    new SectionHeadingFix(current.level));
            }
        }
    }
    
    private void checkNavigationHierarchy(List<EnhancedHeading> navHeadings, PsiFile file, ProblemsHolder holder) {
        // Navigation headings can be more flexible, but still should be logical
        for (EnhancedHeading heading : navHeadings) {
            if (heading.level > 3) {
                registerProblem(file, holder, heading.offset,
                    "Navigation heading H" + heading.level + " is quite deep. Consider H2-H3 for navigation or use aria-label on nav element",
                    ProblemHighlightType.INFORMATION,
                    new ImproveNavigationHeadingFix());
            }
        }
    }
    
    private void checkOverallDocumentHierarchy(List<EnhancedHeading> headings, PsiFile file, ProblemsHolder holder) {
        // Check the overall flow of content headings across different contexts
        for (int i = 1; i < headings.size(); i++) {
            EnhancedHeading current = headings.get(i);
            EnhancedHeading previous = headings.get(i - 1);
            
            // If jumping from main content to fluid section or vice versa, different rules apply
            if (current.context != previous.context) {
                if (current.level > previous.level + 2) {
                    registerProblem(file, holder, current.offset,
                        String.format("Large heading level jump between contexts: H%d to H%d. Consider intermediate heading levels",
                            previous.level, current.level),
                        ProblemHighlightType.INFORMATION,
                        new AddIntermediateHeadingsFix(previous.level, current.level));
                }
            }
        }
    }
    
    private void checkEmptyHeadings(List<EnhancedHeading> headings, PsiFile file, ProblemsHolder holder) {
        for (EnhancedHeading heading : headings) {
            if (AccessibilityUtils.isEmptyOrWhitespace(heading.textContent)) {
                // Check if it contains only images without alt text
                if (heading.htmlContent.contains("<img")) {
                    registerProblem(file, holder, heading.offset,
                        String.format("H%d contains only images - ensure images have alt text or add heading text",
                            heading.level),
                        ProblemHighlightType.ERROR,
                        null);
                } else {
                    String contextMsg = getContextMessage(heading.context);
                    registerProblem(file, holder, heading.offset,
                        String.format("Empty H%d element in %s - headings must contain text content",
                            heading.level, contextMsg),
                        ProblemHighlightType.ERROR,
                        new AddHeadingTextQuickFix(heading.level));
                }
            }
        }
    }
    
    private void checkGenericHeadings(List<EnhancedHeading> headings, PsiFile file, ProblemsHolder holder) {
        for (EnhancedHeading heading : headings) {
            if (AccessibilityUtils.isGenericText(heading.textContent)) {
                String contextMsg = getContextMessage(heading.context);
                registerProblem(file, holder, heading.offset,
                    String.format("H%d in %s contains generic text \"%s\" - use descriptive heading text",
                        heading.level, contextMsg, heading.textContent),
                    ProblemHighlightType.WARNING,
                    null);
            }
            
            // Check for headings that are just numbers or single letters
            String trimmed = heading.textContent.trim();
            if (trimmed.matches("^[0-9]+$") || trimmed.matches("^[a-zA-Z]$")) {
                String contextMsg = getContextMessage(heading.context);
                registerProblem(file, holder, heading.offset,
                    String.format("H%d in %s contains non-descriptive text \"%s\"",
                        heading.level, contextMsg, trimmed),
                    ProblemHighlightType.WARNING,
                    null);
            }
        }
    }
    
    private void checkNavigationHeadingContext(List<EnhancedHeading> headings, DocumentStructure structure,
                                              PsiFile file, ProblemsHolder holder) {
        for (EnhancedHeading heading : headings) {
            if (heading.context == HeadingContext.NAVIGATION) {
                // Check if navigation would be better served by aria-label
                if (NAVIGATION_HEADINGS.contains(heading.textContent.toLowerCase().trim())) {
                    registerProblem(file, holder, heading.offset,
                        String.format("Navigation heading '%s' could be replaced with aria-label on nav element for cleaner markup",
                            heading.textContent),
                        ProblemHighlightType.INFORMATION,
                        new ReplaceWithAriaLabelFix(heading.textContent));
                }
            }
        }
    }
    
    private void checkFluidSectionHeadings(List<EnhancedHeading> headings, DocumentStructure structure,
                                          PsiFile file, ProblemsHolder holder) {
        for (EnhancedHeading heading : headings) {
            if (heading.context == HeadingContext.FLUID_SECTION) {
                // Fluid sections often represent modular content pieces
                if (heading.level == 1) {
                    registerProblem(file, holder, heading.offset,
                        "H1 in Fluid section may conflict with main page heading. Consider H2+ for section content",
                        ProblemHighlightType.WARNING,
                        new ChangeHeadingLevelQuickFix(1, 2));
                }
            }
        }
    }
    
    private String getContextMessage(HeadingContext context) {
        switch (context) {
            case NAVIGATION: return "navigation";
            case SECTIONING_CONTENT: return "sectioning element";
            case FLUID_SECTION: return "Fluid section";
            case MAIN_CONTENT: 
            default: return "main content";
        }
    }
    
    private void registerProblem(PsiFile file, ProblemsHolder holder, int offset,
                                String description, ProblemHighlightType type, LocalQuickFix fix) {
        PsiElement element = file.findElementAt(offset);
        if (element != null) {
            if (fix != null) {
                holder.registerProblem(element, description, type, fix);
            } else {
                holder.registerProblem(element, description, type);
            }
        }
    }
    
    // Enhanced data structures
    private enum HeadingContext {
        MAIN_CONTENT,      // Regular document content
        NAVIGATION,        // Inside nav elements or navigation-related
        SECTIONING_CONTENT, // Inside HTML5 sectioning elements
        FLUID_SECTION      // Inside Fluid sections/layouts
    }
    
    private static class DocumentStructure {
        private List<SectioningElement> sectioningElements = new ArrayList<>();
        private List<FluidSection> fluidSections = new ArrayList<>();
        private List<NavigationContext> navigationContexts = new ArrayList<>();
        
        void addSectioningElement(int start, String elementType) {
            sectioningElements.add(new SectioningElement(start, elementType));
        }
        
        void addFluidSection(int start, String sectionName) {
            fluidSections.add(new FluidSection(start, sectionName));
        }
        
        void addNavigationContext(int start, int end) {
            navigationContexts.add(new NavigationContext(start, end));
        }
        
        boolean isInNavigationContext(int offset) {
            return navigationContexts.stream().anyMatch(nav -> offset >= nav.start && offset <= nav.end);
        }
        
        boolean isInSectioningElement(int offset) {
            // This is a simplified check - in real implementation would need to track element boundaries
            return sectioningElements.stream().anyMatch(sec -> offset >= sec.start);
        }
        
        boolean isInFluidSection(int offset) {
            return fluidSections.stream().anyMatch(fluid -> offset >= fluid.start);
        }
        
        private static class SectioningElement {
            final int start;
            final String elementType;
            
            SectioningElement(int start, String elementType) {
                this.start = start;
                this.elementType = elementType;
            }
        }
        
        private static class FluidSection {
            final int start;
            final String sectionName;
            
            FluidSection(int start, String sectionName) {
                this.start = start;
                this.sectionName = sectionName;
            }
        }
        
        private static class NavigationContext {
            final int start;
            final int end;
            
            NavigationContext(int start, int end) {
                this.start = start;
                this.end = end;
            }
        }
    }
    
    // Enhanced heading class with context awareness
    private static class EnhancedHeading {
        final int level;
        final String textContent;
        final String htmlContent;
        final int offset;
        final HeadingContext context;
        
        EnhancedHeading(int level, String textContent, String htmlContent, int offset, HeadingContext context) {
            this.level = level;
            this.textContent = textContent;
            this.htmlContent = htmlContent;
            this.offset = offset;
            this.context = context;
        }
    }
    
    // Enhanced Quick Fixes
    private static class ContextAwareHeadingFix implements LocalQuickFix {
        private final int currentLevel;
        private final int suggestedLevel;
        private final HeadingContext context;
        
        public ContextAwareHeadingFix(int currentLevel, int suggestedLevel, HeadingContext context) {
            this.currentLevel = currentLevel;
            this.suggestedLevel = suggestedLevel;
            this.context = context;
        }
        
        @NotNull
        @Override
        public String getName() {
            return String.format("Change to H%d (suitable for %s)", suggestedLevel, getContextName());
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix heading hierarchy with context awareness";
        }
        
        private String getContextName() {
            switch (context) {
                case NAVIGATION: return "navigation";
                case SECTIONING_CONTENT: return "section";
                case FLUID_SECTION: return "Fluid section";
                default: return "content";
            }
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would change heading level
        }
    }
    
    private static class ImproveNavigationHeadingFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Replace with aria-label on nav element or use H2-H3";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Improve navigation heading";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest aria-label alternatives
        }
    }
    
    private static class SectionHeadingFix implements LocalQuickFix {
        private final int currentLevel;
        
        public SectionHeadingFix(int currentLevel) {
            this.currentLevel = currentLevel;
        }
        
        @NotNull
        @Override
        public String getName() {
            return "Fix section heading (use H1 for section title or H2 to continue hierarchy)";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix sectioning element heading";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would provide options for section headings
        }
    }
    
    private static class AddIntermediateHeadingsFix implements LocalQuickFix {
        private final int fromLevel;
        private final int toLevel;
        
        public AddIntermediateHeadingsFix(int fromLevel, int toLevel) {
            this.fromLevel = fromLevel;
            this.toLevel = toLevel;
        }
        
        @NotNull
        @Override
        public String getName() {
            return String.format("Add intermediate headings between H%d and H%d", fromLevel, toLevel);
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add intermediate headings";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest adding intermediate heading levels
        }
    }
    
    private static class ReplaceWithAriaLabelFix implements LocalQuickFix {
        private final String headingText;
        
        public ReplaceWithAriaLabelFix(String headingText) {
            this.headingText = headingText;
        }
        
        @NotNull
        @Override
        public String getName() {
            return String.format("Replace heading with aria-label='%s' on nav element", headingText);
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Convert navigation heading to aria-label";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would replace heading with aria-label
        }
    }
    
    /**
     * Enhanced Quick fix to change heading level
     */
    private static class ChangeHeadingLevelQuickFix implements LocalQuickFix {
        private final int currentLevel;
        private final int newLevel;
        
        public ChangeHeadingLevelQuickFix(int currentLevel, int newLevel) {
            this.currentLevel = currentLevel;
            this.newLevel = newLevel;
        }
        
        @NotNull
        @Override
        public String getName() {
            return String.format("Change to H%d", newLevel);
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix heading hierarchy";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find the heading tag
            int headingStart = startOffset;
            while (headingStart > 0 && fileText.charAt(headingStart) != '<') {
                headingStart--;
            }
            
            // Find the end of opening tag
            int openTagEnd = fileText.indexOf('>', headingStart);
            if (openTagEnd == -1) return;
            
            // Find the closing tag
            String closeTag = String.format("</h%d>", currentLevel);
            int closeTagStart = fileText.indexOf(closeTag, openTagEnd);
            if (closeTagStart == -1) return;
            
            // Replace heading level in both opening and closing tags
            String openTag = fileText.substring(headingStart, openTagEnd + 1);
            String newOpenTag = openTag.replaceFirst("h" + currentLevel, "h" + newLevel);
            String newCloseTag = String.format("</h%d>", newLevel);
            
            String newContent = fileText.substring(0, headingStart) +
                              newOpenTag +
                              fileText.substring(openTagEnd + 1, closeTagStart) +
                              newCloseTag +
                              fileText.substring(closeTagStart + closeTag.length());
            
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
    
    /**
     * Quick fix to add text to empty heading
     */
    private static class AddHeadingTextQuickFix implements LocalQuickFix {
        private final int level;
        
        public AddHeadingTextQuickFix(int level) {
            this.level = level;
        }
        
        @NotNull
        @Override
        public String getName() {
            return "Add heading text";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix empty heading";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find the heading opening tag end
            int tagEnd = fileText.indexOf('>', startOffset);
            if (tagEnd == -1) return;
            
            // Find the closing tag start
            String closeTag = String.format("</h%d>", level);
            int closeTagStart = fileText.indexOf(closeTag, tagEnd);
            if (closeTagStart == -1) return;
            
            // Add placeholder text
            String placeholderText = "Heading text";
            
            String newContent = fileText.substring(0, tagEnd + 1) +
                              placeholderText +
                              fileText.substring(closeTagStart);
            
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
}