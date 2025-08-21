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

/**
 * Comprehensive aria-label validation that detects:
 * - Missing aria-labels where required
 * - Redundant aria-labels that duplicate visible text
 * - Unnecessary aria-labels on elements that don't need them
 * - Poor aria-labels that override better visible text
 * - Conflicting labeling mechanisms
 */
public class AriaLabelValidationInspection extends FluidAccessibilityInspection {
    
    // Elements that should NOT have aria-label unless specific conditions
    private static final Set<String> NON_INTERACTIVE_ELEMENTS = new HashSet<>(Arrays.asList(
        "p", "div", "span", "section", "article", "header", "footer",
        "h1", "h2", "h3", "h4", "h5", "h6", "ul", "ol", "li", "dl", "dt", "dd",
        "blockquote", "pre", "code", "em", "strong", "b", "i", "u",
        "table", "tr", "td", "th", "tbody", "thead", "tfoot"
    ));
    
    // Elements that typically don't need aria-label if they have text content
    private static final Set<String> SELF_LABELING_ELEMENTS = new HashSet<>(Arrays.asList(
        "button", "a", "label", "legend", "caption", "figcaption",
        "option", "optgroup", "summary"
    ));
    
    // Elements that may need aria-label even with content
    private static final Set<String> MAY_NEED_ARIA_LABEL = new HashSet<>(Arrays.asList(
        "nav", "main", "aside", "search", "form", "region", "complementary"
    ));
    
    // Form elements that need labels
    private static final Set<String> FORM_ELEMENTS = new HashSet<>(Arrays.asList(
        "input", "select", "textarea", "progress", "meter", "output"
    ));
    
    private static final Pattern ELEMENT_PATTERN = Pattern.compile(
        "<([a-zA-Z][a-zA-Z0-9-]*)(\\s+[^>]*)?>(.*?)</\\1>|<([a-zA-Z][a-zA-Z0-9-]*)(\\s+[^>]*?)/>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "\\baria-label\\s*=\\s*[\"']([^\"']*)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_LABELLEDBY_PATTERN = Pattern.compile(
        "\\baria-labelledby\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_DESCRIBEDBY_PATTERN = Pattern.compile(
        "\\baria-describedby\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TITLE_PATTERN = Pattern.compile(
        "\\btitle\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ID_PATTERN = Pattern.compile(
        "\\bid\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FOR_PATTERN = Pattern.compile(
        "\\bfor\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ROLE_PATTERN = Pattern.compile(
        "\\brole\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TYPE_PATTERN = Pattern.compile(
        "\\btype\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
        "\\bplaceholder\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern VALUE_PATTERN = Pattern.compile(
        "\\bvalue\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Comprehensive aria-label validation";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "AriaLabelValidation";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Build label associations map
        Map<String, String> labelForMap = buildLabelForMap(content);
        Map<String, String> idToElementMap = buildIdToElementMap(content);
        
        // Check all elements
        Matcher elementMatcher = ELEMENT_PATTERN.matcher(content);
        
        while (elementMatcher.find()) {
            String tagName = elementMatcher.group(1) != null ? elementMatcher.group(1) : elementMatcher.group(4);
            String attributes = elementMatcher.group(2) != null ? elementMatcher.group(2) : elementMatcher.group(5);
            String elementContent = elementMatcher.group(3);
            
            if (tagName == null || attributes == null) continue;
            
            tagName = tagName.toLowerCase();
            
            // Check this element for aria-label issues
            checkAriaLabelUsage(tagName, attributes, elementContent, 
                               elementMatcher.start(), elementMatcher.end(),
                               file, holder, labelForMap, idToElementMap);
        }
        
        // Check for conflicting labeling mechanisms
        checkConflictingLabels(content, file, holder);
    }
    
    private void checkAriaLabelUsage(String tagName, String attributes, String content,
                                      int start, int end, PsiFile file, ProblemsHolder holder,
                                      Map<String, String> labelForMap, Map<String, String> idToElementMap) {
        
        Matcher ariaLabelMatcher = ARIA_LABEL_PATTERN.matcher(attributes);
        Matcher ariaLabelledbyMatcher = ARIA_LABELLEDBY_PATTERN.matcher(attributes);
        Matcher titleMatcher = TITLE_PATTERN.matcher(attributes);
        Matcher idMatcher = ID_PATTERN.matcher(attributes);
        Matcher roleMatcher = ROLE_PATTERN.matcher(attributes);
        
        boolean hasAriaLabel = ariaLabelMatcher.find();
        boolean hasAriaLabelledby = ariaLabelledbyMatcher.find();
        boolean hasTitle = titleMatcher.find();
        String elementId = idMatcher.find() ? idMatcher.group(1) : null;
        String role = roleMatcher.find() ? roleMatcher.group(1) : null;
        
        // Get visible text content (strip HTML tags)
        String visibleText = content != null ? stripHtmlTags(content).trim() : "";
        
        // Check for unnecessary aria-label on non-interactive elements
        if (hasAriaLabel && NON_INTERACTIVE_ELEMENTS.contains(tagName) && role == null) {
            String ariaLabelValue = ariaLabelMatcher.group(1);
            registerProblem(holder, file, start, end,
                "Unnecessary aria-label on non-interactive <" + tagName + "> element. Screen readers already read the content",
                new RemoveAriaLabelFix());
        }
        
        // Check for redundant aria-label on self-labeling elements
        if (hasAriaLabel && SELF_LABELING_ELEMENTS.contains(tagName)) {
            String ariaLabelValue = ariaLabelMatcher.group(1).trim();
            
            if (!visibleText.isEmpty()) {
                // Check if aria-label duplicates visible text
                if (ariaLabelValue.equalsIgnoreCase(visibleText)) {
                    registerProblem(holder, file, start, end,
                        "Redundant aria-label duplicates the element's text content",
                        new RemoveAriaLabelFix());
                }
                // Check if aria-label is less descriptive than visible text
                else if (visibleText.length() > ariaLabelValue.length() + 5 && 
                         !ariaLabelValue.isEmpty()) {
                    registerProblem(holder, file, start, end,
                        "aria-label '" + ariaLabelValue + "' overrides more descriptive visible text '" + 
                        truncate(visibleText, 30) + "'",
                        new RemoveAriaLabelFix());
                }
                // Check for generic aria-labels
                else if (isGenericLabel(ariaLabelValue) && !isGenericLabel(visibleText)) {
                    registerProblem(holder, file, start, end,
                        "Generic aria-label '" + ariaLabelValue + "' overrides specific visible text",
                        new RemoveAriaLabelFix());
                }
            }
        }
        
        // Check for form elements
        if (FORM_ELEMENTS.contains(tagName)) {
            checkFormElementLabeling(tagName, attributes, elementId, hasAriaLabel, 
                                    hasAriaLabelledby, hasTitle, start, end, 
                                    file, holder, labelForMap);
        }
        
        // Check for multiple labeling mechanisms
        if (hasAriaLabel && hasAriaLabelledby) {
            registerProblem(holder, file, start, end,
                "Element has both aria-label and aria-labelledby. Use only one labeling method",
                new RemoveAriaLabelFix());
        }
        
        // Check for aria-label with explicit label
        if (hasAriaLabel && elementId != null && labelForMap.containsKey(elementId)) {
            String ariaLabelValue = ariaLabelMatcher.group(1);
            registerProblem(holder, file, start, end,
                "Element has both <label> and aria-label. The aria-label will override the visible label",
                new RemoveAriaLabelFix());
        }
        
        // Check for empty aria-label
        if (hasAriaLabel) {
            String ariaLabelValue = ariaLabelMatcher.group(1).trim();
            if (ariaLabelValue.isEmpty()) {
                registerProblem(holder, file, start, end,
                    "Empty aria-label provides no accessible name",
                    new RemoveAriaLabelFix());
            }
        }
        
        // Check for aria-label on elements with aria-hidden
        if (hasAriaLabel && attributes.contains("aria-hidden=\"true\"")) {
            registerProblem(holder, file, start, end,
                "aria-label on element with aria-hidden='true' will be ignored",
                new RemoveAriaLabelFix());
        }
        
        // Check landmark elements that might benefit from aria-label
        if (MAY_NEED_ARIA_LABEL.contains(tagName) || MAY_NEED_ARIA_LABEL.contains(role)) {
            checkLandmarkLabeling(tagName, role, hasAriaLabel, hasAriaLabelledby, 
                                 start, end, file, holder);
        }
        
        // Check icon buttons (buttons with only icons)
        if ("button".equals(tagName) || "a".equals(tagName)) {
            checkIconButton(visibleText, hasAriaLabel, hasAriaLabelledby, hasTitle,
                          start, end, file, holder);
        }
    }
    
    private void checkFormElementLabeling(String tagName, String attributes, String elementId,
                                          boolean hasAriaLabel, boolean hasAriaLabelledby, 
                                          boolean hasTitle, int start, int end,
                                          PsiFile file, ProblemsHolder holder,
                                          Map<String, String> labelForMap) {
        
        // Check if input has a visible label
        boolean hasVisibleLabel = elementId != null && labelForMap.containsKey(elementId);
        
        // Special case for input type
        Matcher typeMatcher = TYPE_PATTERN.matcher(attributes);
        String inputType = typeMatcher.find() ? typeMatcher.group(1) : "text";
        
        // Inputs that don't need labels
        Set<String> selfLabelingTypes = new HashSet<>(Arrays.asList(
            "submit", "reset", "button", "image", "hidden"
        ));
        
        if (selfLabelingTypes.contains(inputType)) {
            if (hasAriaLabel) {
                // Check if value attribute provides label
                Matcher valueMatcher = VALUE_PATTERN.matcher(attributes);
                if (valueMatcher.find()) {
                    String value = valueMatcher.group(1);
                    String ariaLabel = ARIA_LABEL_PATTERN.matcher(attributes).find() ? 
                        ARIA_LABEL_PATTERN.matcher(attributes).group(1) : "";
                    
                    if (value.equalsIgnoreCase(ariaLabel)) {
                        registerProblem(holder, file, start, end,
                            "Redundant aria-label on input[type='" + inputType + "'] duplicates value attribute",
                            new RemoveAriaLabelFix());
                    }
                }
            }
            return;
        }
        
        // Check for placeholder being used as label (bad practice)
        Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(attributes);
        boolean hasPlaceholder = placeholderMatcher.find();
        
        if (!hasVisibleLabel && !hasAriaLabel && !hasAriaLabelledby && !hasTitle) {
            if (hasPlaceholder) {
                registerProblem(holder, file, start, end,
                    "Form input relies only on placeholder for labeling. Add a proper label or aria-label",
                    new AddAriaLabelFix());
            } else {
                // This is already caught by MissingFormLabelInspection, so we can skip
                return;
            }
        }
        
        // Check for redundant aria-label when visible label exists
        if (hasVisibleLabel && hasAriaLabel) {
            String labelText = labelForMap.get(elementId);
            String ariaLabel = ARIA_LABEL_PATTERN.matcher(attributes).group(1);
            
            if (labelText != null && labelText.equalsIgnoreCase(ariaLabel)) {
                registerProblem(holder, file, start, end,
                    "aria-label duplicates the visible <label> text",
                    new RemoveAriaLabelFix());
            }
        }
    }
    
    private void checkLandmarkLabeling(String tagName, String role, boolean hasAriaLabel,
                                       boolean hasAriaLabelledby, int start, int end,
                                       PsiFile file, ProblemsHolder holder) {
        // This is more of a suggestion than an error
        // Only suggest for multiple instances (would need more context to detect)
        // For now, we'll skip this as it requires page-wide analysis
    }
    
    private void checkIconButton(String visibleText, boolean hasAriaLabel, 
                                 boolean hasAriaLabelledby, boolean hasTitle,
                                 int start, int end, PsiFile file, ProblemsHolder holder) {
        // Check if button has only icon content (common patterns)
        if (visibleText.matches("^[×✕✖✓✔➜→←↑↓+−✎🗑️💾📎🔍❌⚙️☰≡╳]$") ||
            visibleText.matches("^<i[^>]*class=[\"'][^\"']*(?:fa-|icon-|glyphicon-)[^\"']*[\"'][^>]*>.*</i>$")) {
            
            if (!hasAriaLabel && !hasAriaLabelledby && !hasTitle) {
                registerProblem(holder, file, start, end,
                    "Icon-only button needs accessible text (aria-label, aria-labelledby, or title)",
                    new AddAriaLabelFix());
            }
        }
    }
    
    private void checkConflictingLabels(String content, PsiFile file, ProblemsHolder holder) {
        // Check for elements with multiple conflicting label methods
        Pattern multiLabelPattern = Pattern.compile(
            "<[^>]+(?:aria-label\\s*=\\s*[\"'][^\"']+[\"'][^>]*aria-labelledby\\s*=\\s*[\"'][^\"']+[\"']|" +
            "aria-labelledby\\s*=\\s*[\"'][^\"']+[\"'][^>]*aria-label\\s*=\\s*[\"'][^\"']+[\"'])[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = multiLabelPattern.matcher(content);
        while (matcher.find()) {
            registerProblem(holder, file, matcher.start(), matcher.end(),
                "Element has conflicting aria-label and aria-labelledby attributes",
                new ResolveConflictingLabelsFix());
        }
    }
    
    private Map<String, String> buildLabelForMap(String content) {
        Map<String, String> labelForMap = new HashMap<>();
        Pattern labelPattern = Pattern.compile(
            "<label[^>]*\\bfor\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</label>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL
        );
        
        Matcher matcher = labelPattern.matcher(content);
        while (matcher.find()) {
            String forId = matcher.group(1);
            String labelText = stripHtmlTags(matcher.group(2)).trim();
            labelForMap.put(forId, labelText);
        }
        
        return labelForMap;
    }
    
    private Map<String, String> buildIdToElementMap(String content) {
        Map<String, String> idMap = new HashMap<>();
        Pattern idPattern = Pattern.compile(
            "<([^>\\s]+)[^>]*\\bid\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher matcher = idPattern.matcher(content);
        while (matcher.find()) {
            String element = matcher.group(1);
            String id = matcher.group(2);
            idMap.put(id, element);
        }
        
        return idMap;
    }
    
    private String stripHtmlTags(String text) {
        if (text == null) return "";
        return text.replaceAll("<[^>]+>", "").replaceAll("\\s+", " ").trim();
    }
    
    private boolean isGenericLabel(String label) {
        Set<String> genericLabels = new HashSet<>(Arrays.asList(
            "button", "link", "click", "click here", "image", "icon",
            "menu", "navigation", "content", "text", "element"
        ));
        return genericLabels.contains(label.toLowerCase());
    }
    
    private String truncate(String text, int maxLength) {
        if (text.length() <= maxLength) return text;
        return text.substring(0, maxLength) + "...";
    }
    
    
    private static class RemoveAriaLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Remove unnecessary aria-label";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove the aria-label attribute
        }
    }
    
    private static class AddAriaLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-label for accessibility";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-label
        }
    }
    
    private static class ResolveConflictingLabelsFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Keep only aria-labelledby (recommended)";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove aria-label and keep aria-labelledby
        }
    }
}