package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import com.typo3.fluid.linter.utils.AccessibilityUtils;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Inspection to check that all form inputs have proper labels.
 * Checks for:
 * - Explicit labels with for/id association
 * - Implicit labels (input nested in label)
 * - ARIA labeling (aria-label, aria-labelledby)
 * - Title attribute as fallback
 */
public class MissingFormLabelInspection extends FluidAccessibilityInspection {
    
    // Pattern for form inputs that need labels
    private static final Pattern INPUT_PATTERN = Pattern.compile(
        "<input\\s+[^>]*>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern TEXTAREA_PATTERN = Pattern.compile(
        "<textarea\\s+[^>]*>.*?</textarea>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern SELECT_PATTERN = Pattern.compile(
        "<select\\s+[^>]*>.*?</select>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Fluid form ViewHelpers
    private static final Pattern FLUID_INPUT_PATTERN = Pattern.compile(
        "<f:form\\.(?:textfield|password|hidden|submit|checkbox|radio|textarea|select|upload|datePicker|button)\\s+[^>]*/?\\s*>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Fieldset patterns for group labeling
    private static final Pattern FIELDSET_PATTERN = Pattern.compile(
        "<fieldset\\s*[^>]*>.*?</fieldset>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern LEGEND_PATTERN = Pattern.compile(
        "<legend\\s*[^>]*>(.*?)</legend>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Placeholder-only anti-pattern detection
    private static final Pattern PLACEHOLDER_PATTERN = Pattern.compile(
        "\\bplaceholder\\s*=\\s*[\\\"']([^\\\"']+)[\\\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    // Required field patterns
    private static final Pattern REQUIRED_PATTERN = Pattern.compile(
        "\\b(?:required|aria-required\\s*=\\s*[\\\"']true[\\\"'])",
        Pattern.CASE_INSENSITIVE
    );
    
    // Date/time component patterns
    private static final Pattern DATE_TIME_GROUPS = Pattern.compile(
        "(?:date|time|month|year|day|hour|minute|second|am|pm)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Label patterns
    private static final Pattern LABEL_PATTERN = Pattern.compile(
        "<label\\s+[^>]*>.*?</label>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Input types that don't need labels
    private static final Set<String> NO_LABEL_REQUIRED = new HashSet<>();
    static {
        NO_LABEL_REQUIRED.add("submit");
        NO_LABEL_REQUIRED.add("reset");
        NO_LABEL_REQUIRED.add("button");
        NO_LABEL_REQUIRED.add("hidden");
    }
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Form inputs missing labels";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "MissingFormLabel";
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
        com.typo3.fluid.linter.settings.RuleSettingsState st = com.typo3.fluid.linter.settings.RuleSettingsState.getInstance(file.getProject());
        if (st != null && st.isUniversalEnabled() && st.isSuppressLegacyDuplicates()) {
            return; // suppressed when Universal is enabled and suppression is active
        }
        String content = file.getText();
        
        // Collect all labels and their associations
        Map<String, String> labelForMap = collectLabels(content);
        Set<String> implicitLabeledInputs = findImplicitlyLabeledInputs(content);
        
        // Check HTML inputs
        checkInputElements(content, file, holder, labelForMap, implicitLabeledInputs);
        
        // Check textareas
        checkTextareaElements(content, file, holder, labelForMap, implicitLabeledInputs);
        
        // Check selects
        checkSelectElements(content, file, holder, labelForMap, implicitLabeledInputs);
        
        // Check Fluid form ViewHelpers
        checkFluidFormElements(content, file, holder, labelForMap);
    }
    
    private Map<String, String> collectLabels(String content) {
        Map<String, String> labelForMap = new HashMap<>();
        Matcher matcher = LABEL_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String label = matcher.group();
            String forAttr = getAttributeValue(label, "for");
            if (forAttr != null && !forAttr.isEmpty()) {
                labelForMap.put(forAttr, label);
            }
        }
        
        return labelForMap;
    }
    
    private Set<String> findImplicitlyLabeledInputs(String content) {
        Set<String> implicitlyLabeled = new HashSet<>();
        Matcher labelMatcher = LABEL_PATTERN.matcher(content);
        
        while (labelMatcher.find()) {
            String labelContent = labelMatcher.group();
            
            // Check if label contains input elements (implicit labeling)
            if (labelContent.contains("<input") || 
                labelContent.contains("<textarea") || 
                labelContent.contains("<select") ||
                labelContent.contains("<f:form.")) {
                
                // Extract IDs of nested inputs
                Pattern idPattern = Pattern.compile("\\bid\\s*=\\s*[\"']([^\"']+)[\"']");
                Matcher idMatcher = idPattern.matcher(labelContent);
                while (idMatcher.find()) {
                    implicitlyLabeled.add(idMatcher.group(1));
                }
            }
        }
        
        return implicitlyLabeled;
    }
    
    private void checkInputElements(String content, PsiFile file, ProblemsHolder holder,
                                   Map<String, String> labelForMap, Set<String> implicitLabeledInputs) {
        Matcher matcher = INPUT_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String inputTag = matcher.group();
            int offset = matcher.start();
            
            // Check input type
            String type = getAttributeValue(inputTag, "type");
            if (type == null) type = "text"; // Default type
            
            // Skip inputs that don't need labels
            if (NO_LABEL_REQUIRED.contains(type.toLowerCase())) {
                continue;
            }
            
            // Check if input has proper labeling
            if (!hasProperLabeling(inputTag, labelForMap, implicitLabeledInputs)) {
                String description = String.format(
                    "%s missing label for accessibility",
                    AccessibilityUtils.getInputTypeDescription(type)
                );
                
                registerProblem(file, holder, offset, description,
                    ProblemHighlightType.ERROR,
                    new AddLabelQuickFix(type));
            }
        }
    }
    
    private void checkTextareaElements(String content, PsiFile file, ProblemsHolder holder,
                                      Map<String, String> labelForMap, Set<String> implicitLabeledInputs) {
        Matcher matcher = TEXTAREA_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String textarea = matcher.group();
            int offset = matcher.start();
            
            if (!hasProperLabeling(textarea, labelForMap, implicitLabeledInputs)) {
                registerProblem(file, holder, offset,
                    "Textarea missing label for accessibility",
                    ProblemHighlightType.ERROR,
                    new AddLabelQuickFix("textarea"));
            }
        }
    }
    
    private void checkSelectElements(String content, PsiFile file, ProblemsHolder holder,
                                    Map<String, String> labelForMap, Set<String> implicitLabeledInputs) {
        Matcher matcher = SELECT_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String select = matcher.group();
            int offset = matcher.start();
            
            if (!hasProperLabeling(select, labelForMap, implicitLabeledInputs)) {
                registerProblem(file, holder, offset,
                    "Select element missing label for accessibility",
                    ProblemHighlightType.ERROR,
                    new AddLabelQuickFix("select"));
            }
        }
    }
    
    private void checkFluidFormElements(String content, PsiFile file, ProblemsHolder holder,
                                       Map<String, String> labelForMap) {
        Matcher matcher = FLUID_INPUT_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String viewHelper = matcher.group();
            int offset = matcher.start();
            
            // Extract ViewHelper type
            String tagName = extractTagName(viewHelper);
            if (tagName == null) continue;
            
            // Check specific types that don't need labels
            if (tagName.endsWith("submit") || tagName.endsWith("button") || tagName.endsWith("hidden")) {
                continue;
            }
            
            // Fluid ViewHelpers often have 'property' attribute that auto-generates labels
            // But we should still check for explicit labeling
            String id = getAttributeValue(viewHelper, "id");
            String property = getAttributeValue(viewHelper, "property");
            
            // If no id and no property, it's likely missing label
            if ((id == null || !labelForMap.containsKey(id)) && property == null) {
                if (!hasAttribute(viewHelper, "aria-label") && 
                    !hasAttribute(viewHelper, "aria-labelledby") &&
                    !hasAttribute(viewHelper, "title")) {
                    
                    String type = tagName.replace("f:form.", "");
                    registerProblem(file, holder, offset,
                        String.format("Fluid form ViewHelper '%s' missing label", type),
                        ProblemHighlightType.ERROR,
                        new AddFluidLabelQuickFix());
                }
            }
        }
    }
    
    private boolean hasProperLabeling(String element, Map<String, String> labelForMap, 
                                     Set<String> implicitLabeledInputs) {
        // Check for id and explicit label association
        String id = getAttributeValue(element, "id");
        if (id != null) {
            if (labelForMap.containsKey(id) || implicitLabeledInputs.contains(id)) {
                return true;
            }
        }
        
        // Check for ARIA labeling
        if (hasAttribute(element, "aria-label") || 
            hasAttribute(element, "aria-labelledby")) {
            return true;
        }
        
        // Check for title attribute (less ideal but acceptable)
        String title = getAttributeValue(element, "title");
        if (title != null && !title.trim().isEmpty()) {
            return true;
        }
        
        // For buttons/submits with value attribute
        String type = getAttributeValue(element, "type");
        if ("submit".equals(type) || "button".equals(type)) {
            String value = getAttributeValue(element, "value");
            if (value != null && !value.trim().isEmpty()) {
                return true;
            }
        }
        
        return false;
    }
    
    private void registerProblem(PsiFile file, ProblemsHolder holder, int offset,
                                String description, ProblemHighlightType type, LocalQuickFix fix) {
        PsiElement element = file.findElementAt(offset);
        if (element != null) {
            holder.registerProblem(element, description, type, fix);
        }
    }
    
    /**
     * Quick fix to add a label to form element
     */
    private static class AddLabelQuickFix implements LocalQuickFix {
        private final String elementType;
        
        public AddLabelQuickFix(String elementType) {
            this.elementType = elementType;
        }
        
        @NotNull
        @Override
        public String getName() {
            return "Add label for " + elementType;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add form label";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find the input element
            int inputStart = startOffset;
            while (inputStart > 0 && fileText.charAt(inputStart) != '<') {
                inputStart--;
            }
            
            // Generate unique ID if not present
            String inputElement = fileText.substring(inputStart, 
                fileText.indexOf('>', inputStart) + 1);
            
            String id = null;
            Pattern idPattern = Pattern.compile("\\bid\\s*=\\s*[\"']([^\"']+)[\"']");
            Matcher idMatcher = idPattern.matcher(inputElement);
            
            if (idMatcher.find()) {
                id = idMatcher.group(1);
            } else {
                // Generate ID
                id = elementType + "_" + System.currentTimeMillis();
                
                // Add ID to element
                int tagEnd = fileText.indexOf('>', inputStart);
                String beforeEnd = fileText.substring(0, tagEnd);
                String afterEnd = fileText.substring(tagEnd);
                
                fileText = beforeEnd + " id=\"" + id + "\"" + afterEnd;
            }
            
            // Add label before the input
            String labelHtml = String.format("<label for=\"%s\">Enter %s:</label>\n", 
                id, elementType);
            
            String newContent = fileText.substring(0, inputStart) + 
                              labelHtml + 
                              fileText.substring(inputStart);
            
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
    
    /**
     * Quick fix for Fluid form ViewHelpers
     */
    private static class AddFluidLabelQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Add label attribute or property";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add form label";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find the ViewHelper tag end
            int tagEnd = fileText.indexOf('>', startOffset);
            if (tagEnd == -1) return;
            
            String beforeTag = fileText.substring(0, tagEnd);
            String afterTag = fileText.substring(tagEnd);
            
            // Add aria-label as a quick solution
            if (beforeTag.endsWith("/")) {
                beforeTag = beforeTag.substring(0, beforeTag.length() - 1).trim() + 
                          " aria-label=\"\" /";
            } else {
                beforeTag = beforeTag + " aria-label=\"\"";
            }
            
            String newContent = beforeTag + afterTag;
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
    
    /**
     * Check for proper fieldset/legend group labeling scenarios
     */
    private void checkFieldsetGrouping(String content, PsiFile file, ProblemsHolder holder) {
        Matcher fieldsetMatcher = FIELDSET_PATTERN.matcher(content);
        
        while (fieldsetMatcher.find()) {
            String fieldsetContent = fieldsetMatcher.group();
            int fieldsetOffset = fieldsetMatcher.start();
            
            // Check for legend within fieldset
            Matcher legendMatcher = LEGEND_PATTERN.matcher(fieldsetContent);
            if (!legendMatcher.find()) {
                // Count inputs in fieldset to determine if legend is needed
                int inputCount = countInputsInFieldset(fieldsetContent);
                if (inputCount > 1) {
                    registerProblem(file, holder, fieldsetOffset,
                        "Fieldset with multiple form controls should have a legend element",
                        ProblemHighlightType.WARNING,
                        new AddLegendQuickFix());
                }
            } else {
                // Check if legend content is meaningful
                String legendText = legendMatcher.group(1).trim();
                if (legendText.isEmpty() || legendText.length() < 3) {
                    registerProblem(file, holder, fieldsetOffset + legendMatcher.start(),
                        "Legend should provide a meaningful description for the fieldset",
                        ProblemHighlightType.WARNING,
                        null);
                }
            }
        }
    }
    
    private int countInputsInFieldset(String fieldsetContent) {
        int count = 0;
        count += countOccurrences(fieldsetContent, "<input");
        count += countOccurrences(fieldsetContent, "<textarea");
        count += countOccurrences(fieldsetContent, "<select");
        count += countOccurrences(fieldsetContent, "<f:form.");
        return count;
    }
    
    private int countOccurrences(String text, String search) {
        int count = 0;
        int index = 0;
        while ((index = text.indexOf(search, index)) != -1) {
            count++;
            index += search.length();
        }
        return count;
    }
    
    /**
     * Detect placeholder-only anti-patterns
     */
    private void checkPlaceholderAntiPatterns(String content, PsiFile file, ProblemsHolder holder,
                                              Map<String, String> labelForMap, Set<String> implicitLabeledInputs) {
        Matcher inputMatcher = INPUT_PATTERN.matcher(content);
        
        while (inputMatcher.find()) {
            String inputTag = inputMatcher.group();
            int offset = inputMatcher.start();
            
            // Check if input has only placeholder but no proper label
            Matcher placeholderMatcher = PLACEHOLDER_PATTERN.matcher(inputTag);
            if (placeholderMatcher.find()) {
                String placeholderText = placeholderMatcher.group(1);
                
                // Check if this input has proper labeling beyond placeholder
                if (!hasProperLabeling(inputTag, labelForMap, implicitLabeledInputs)) {
                    // Placeholder shouldn't be the only form of labeling
                    if (isPlaceholderOnlyLabeling(inputTag, placeholderText)) {
                        registerProblem(file, holder, offset,
                            "Placeholder text should not be the only form of labeling. Add a proper label",
                            ProblemHighlightType.WARNING,
                            new ConvertPlaceholderToLabelQuickFix(placeholderText));
                    }
                }
                
                // Check for placeholder anti-patterns
                if (placeholderText.toLowerCase().contains("required") ||
                    placeholderText.toLowerCase().contains("*")) {
                    registerProblem(file, holder, offset,
                        "Placeholder should not indicate required status. Use aria-required or visual indicators",
                        ProblemHighlightType.WARNING,
                        new FixRequiredIndicatorQuickFix());
                }
            }
        }
    }
    
    private boolean isPlaceholderOnlyLabeling(String inputTag, String placeholderText) {
        // Check if placeholder looks like it's being used as a label
        return placeholderText.length() > 10 && // Substantial text
               !placeholderText.toLowerCase().contains("example") &&
               !placeholderText.toLowerCase().contains("e.g.") &&
               !placeholderText.toLowerCase().contains("format:");
    }
    
    /**
     * Validate required field consistency
     */
    private void checkRequiredFieldConsistency(String content, PsiFile file, ProblemsHolder holder) {
        Matcher inputMatcher = INPUT_PATTERN.matcher(content);
        
        while (inputMatcher.find()) {
            String inputTag = inputMatcher.group();
            int offset = inputMatcher.start();
            
            boolean hasRequired = hasAttribute(inputTag, "required");
            boolean hasAriaRequired = REQUIRED_PATTERN.matcher(inputTag).find();
            
            // Check for inconsistent required indicators
            if (hasRequired && hasAriaRequired) {
                String ariaValue = getAttributeValue(inputTag, "aria-required");
                if ("false".equals(ariaValue)) {
                    registerProblem(file, holder, offset,
                        "Conflicting required indicators: required attribute present but aria-required='false'",
                        ProblemHighlightType.ERROR,
                        new FixRequiredConsistencyQuickFix());
                }
            }
            
            // Check for visual required indicators when required is present
            if (hasRequired || hasAriaRequired) {
                String id = getAttributeValue(inputTag, "id");
                if (id != null) {
                    // Look for label and check if it has visual required indicator
                    Pattern labelForPattern = Pattern.compile(
                        "<label[^>]*\\bfor\\s*=\\s*[\\\"']" + Pattern.quote(id) + "[\\\"'][^>]*>(.*?)</label>",
                        Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
                    Matcher labelMatcher = labelForPattern.matcher(content);
                    if (labelMatcher.find()) {
                        String labelText = labelMatcher.group(1);
                        if (!labelText.contains("*") && !labelText.toLowerCase().contains("required")) {
                            registerProblem(file, holder, offset,
                                "Required field should have visual indication in its label (e.g., asterisk)",
                                ProblemHighlightType.INFORMATION,
                                null);
                        }
                    }
                }
            }
        }
    }
    
    /**
     * Handle related field groups (date/time components)
     */
    private void checkRelatedFieldGroups(String content, PsiFile file, ProblemsHolder holder) {
        // Find potential date/time input groups
        Pattern dateTimeGroupPattern = Pattern.compile(
            "<(?:div|span|fieldset)[^>]*(?:class|id)\\s*=\\s*[\\\"'][^\\\"']*(?:date|time)[^\\\"']*[\\\"'][^>]*>" +
            "(.*?)" +
            "</(?:div|span|fieldset)>",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        
        Matcher groupMatcher = dateTimeGroupPattern.matcher(content);
        
        while (groupMatcher.find()) {
            String groupContent = groupMatcher.group(1);
            int groupOffset = groupMatcher.start();
            
            // Count date/time related inputs in this group
            int dateTimeInputs = 0;
            Matcher inputMatcher = INPUT_PATTERN.matcher(groupContent);
            
            while (inputMatcher.find()) {
                String inputTag = inputMatcher.group();
                String name = getAttributeValue(inputTag, "name");
                String id = getAttributeValue(inputTag, "id");
                String type = getAttributeValue(inputTag, "type");
                
                if ((name != null && DATE_TIME_GROUPS.matcher(name).find()) ||
                    (id != null && DATE_TIME_GROUPS.matcher(id).find()) ||
                    (type != null && ("date".equals(type) || "time".equals(type) || "month".equals(type)))) {
                    dateTimeInputs++;
                }
            }
            
            // If we have multiple date/time inputs, suggest grouping
            if (dateTimeInputs >= 2) {
                // Check if they're in a fieldset
                if (!groupContent.contains("<fieldset")) {
                    registerProblem(file, holder, groupOffset,
                        "Related date/time inputs should be grouped in a fieldset with descriptive legend",
                        ProblemHighlightType.INFORMATION,
                        new GroupDateTimeFieldsQuickFix());
                }
            }
        }
    }
    
    /**
     * Quick fix to add a legend to a fieldset
     */
    private static class AddLegendQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Add legend to fieldset";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add fieldset legend";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add <legend> after <fieldset>
        }
    }
    
    /**
     * Quick fix to convert placeholder to proper label
     */
    private static class ConvertPlaceholderToLabelQuickFix implements LocalQuickFix {
        private final String placeholderText;
        
        public ConvertPlaceholderToLabelQuickFix(String placeholderText) {
            this.placeholderText = placeholderText;
        }
        
        @NotNull
        @Override
        public String getName() {
            return "Add label and use placeholder for hint";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix placeholder-only labeling";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add proper label and modify placeholder to be hint-only
        }
    }
    
    /**
     * Quick fix for required field indicators
     */
    private static class FixRequiredIndicatorQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Move required indication to proper attributes";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix required field indication";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-required and visual indicators
        }
    }
    
    /**
     * Quick fix for required field consistency
     */
    private static class FixRequiredConsistencyQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Fix required attribute consistency";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix required consistency";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would align required and aria-required attributes
        }
    }
    
    /**
     * Quick fix to group date/time fields
     */
    private static class GroupDateTimeFieldsQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Group related fields in fieldset";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Group related fields";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would wrap related fields in fieldset with legend
        }
    }
}
