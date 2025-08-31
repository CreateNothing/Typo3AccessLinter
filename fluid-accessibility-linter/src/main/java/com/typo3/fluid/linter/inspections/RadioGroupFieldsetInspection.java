package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.*;
import com.intellij.openapi.project.Project;
import com.intellij.psi.*;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Inspection to check that radio button groups and related checkboxes
 * are properly grouped with fieldset and legend elements.
 */
public class RadioGroupFieldsetInspection extends FluidAccessibilityInspection {
    
    // Pattern to find radio buttons
    protected static final Pattern RADIO_PATTERN = Pattern.compile(
        "<input\\s+[^>]*type\\s*=\\s*[\"']radio[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Pattern to find checkboxes
    protected static final Pattern CHECKBOX_PATTERN = Pattern.compile(
        "<input\\s+[^>]*type\\s*=\\s*[\"']checkbox[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Fluid radio/checkbox patterns
    protected static final Pattern FLUID_RADIO_PATTERN = Pattern.compile(
        "<f:form\\.radio\\s+[^>]*/?\\s*>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    protected static final Pattern FLUID_CHECKBOX_PATTERN = Pattern.compile(
        "<f:form\\.checkbox\\s+[^>]*/?\\s*>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    // Fieldset and legend patterns
    protected static final Pattern FIELDSET_PATTERN = Pattern.compile(
        "<fieldset[^>]*>.*?</fieldset>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    protected static final Pattern LEGEND_PATTERN = Pattern.compile(
        "<legend[^>]*>.*?</legend>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Radio groups and checkbox groups missing fieldset/legend";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "RadioGroupFieldset";
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
        
        // Find all fieldsets and their content
        Set<RadioGroup> fieldsetRadioGroups = findRadioGroupsInFieldsets(content);
        Set<CheckboxGroup> fieldsetCheckboxGroups = findCheckboxGroupsInFieldsets(content);
        
        // Check radio button groups
        checkRadioGroups(content, file, holder, fieldsetRadioGroups);
        
        // Check checkbox groups (when multiple related checkboxes)
        checkCheckboxGroups(content, file, holder, fieldsetCheckboxGroups);
        
        // Check fieldsets have legends
        checkFieldsetsHaveLegends(content, file, holder);
    }
    
    protected Set<RadioGroup> findRadioGroupsInFieldsets(String content) {
        Set<RadioGroup> groups = new HashSet<>();
        Matcher fieldsetMatcher = FIELDSET_PATTERN.matcher(content);
        
        while (fieldsetMatcher.find()) {
            String fieldsetContent = fieldsetMatcher.group();
            
            // Find radio buttons within this fieldset
            Matcher radioMatcher = RADIO_PATTERN.matcher(fieldsetContent);
            Map<String, List<Integer>> nameGroups = new HashMap<>();
            
            while (radioMatcher.find()) {
                String radioTag = radioMatcher.group();
                String name = getAttributeValue(radioTag, "name");
                if (name != null) {
                    nameGroups.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(fieldsetMatcher.start() + radioMatcher.start());
                }
            }
            
            // Also check Fluid radios
            Matcher fluidRadioMatcher = FLUID_RADIO_PATTERN.matcher(fieldsetContent);
            while (fluidRadioMatcher.find()) {
                String radioTag = fluidRadioMatcher.group();
                String name = getAttributeValue(radioTag, "name");
                if (name == null) {
                    name = getAttributeValue(radioTag, "property");
                }
                if (name != null) {
                    nameGroups.computeIfAbsent(name, k -> new ArrayList<>())
                        .add(fieldsetMatcher.start() + fluidRadioMatcher.start());
                }
            }
            
            // Add all groups found in this fieldset
            for (Map.Entry<String, List<Integer>> entry : nameGroups.entrySet()) {
                if (entry.getValue().size() > 1) {
                    groups.add(new RadioGroup(entry.getKey(), entry.getValue()));
                }
            }
        }
        
        return groups;
    }
    
    protected Set<CheckboxGroup> findCheckboxGroupsInFieldsets(String content) {
        Set<CheckboxGroup> groups = new HashSet<>();
        Matcher fieldsetMatcher = FIELDSET_PATTERN.matcher(content);
        
        while (fieldsetMatcher.find()) {
            String fieldsetContent = fieldsetMatcher.group();
            List<Integer> checkboxOffsets = new ArrayList<>();
            
            // Find checkboxes within this fieldset
            Matcher checkboxMatcher = CHECKBOX_PATTERN.matcher(fieldsetContent);
            while (checkboxMatcher.find()) {
                checkboxOffsets.add(fieldsetMatcher.start() + checkboxMatcher.start());
            }
            
            // Also check Fluid checkboxes
            Matcher fluidCheckboxMatcher = FLUID_CHECKBOX_PATTERN.matcher(fieldsetContent);
            while (fluidCheckboxMatcher.find()) {
                checkboxOffsets.add(fieldsetMatcher.start() + fluidCheckboxMatcher.start());
            }
            
            // If multiple checkboxes in fieldset, consider it a group
            if (checkboxOffsets.size() > 2) {
                groups.add(new CheckboxGroup(checkboxOffsets));
            }
        }
        
        return groups;
    }
    
    private void checkRadioGroups(String content, PsiFile file, ProblemsHolder holder,
                                 Set<RadioGroup> fieldsetGroups) {
        // Find all radio buttons
        Map<String, List<Integer>> radioGroups = new HashMap<>();
        
        // HTML radios
        Matcher radioMatcher = RADIO_PATTERN.matcher(content);
        while (radioMatcher.find()) {
            String radioTag = radioMatcher.group();
            String name = getAttributeValue(radioTag, "name");
            if (name != null) {
                radioGroups.computeIfAbsent(name, k -> new ArrayList<>())
                    .add(radioMatcher.start());
            }
        }
        
        // Fluid radios
        Matcher fluidRadioMatcher = FLUID_RADIO_PATTERN.matcher(content);
        while (fluidRadioMatcher.find()) {
            String radioTag = fluidRadioMatcher.group();
            String name = getAttributeValue(radioTag, "name");
            if (name == null) {
                name = getAttributeValue(radioTag, "property");
            }
            if (name != null) {
                radioGroups.computeIfAbsent(name, k -> new ArrayList<>())
                    .add(fluidRadioMatcher.start());
            }
        }
        
        // Check each radio group
        for (Map.Entry<String, List<Integer>> entry : radioGroups.entrySet()) {
            if (entry.getValue().size() > 1) {
                // This is a radio group with multiple buttons
                RadioGroup group = new RadioGroup(entry.getKey(), entry.getValue());
                
                // Check if this group is already in a fieldset
                boolean inFieldset = false;
                for (RadioGroup fieldsetGroup : fieldsetGroups) {
                    if (fieldsetGroup.name.equals(group.name)) {
                        inFieldset = true;
                        break;
                    }
                }
                
                if (!inFieldset) {
                    // Report problem at first radio button
                    Integer firstOffset = entry.getValue().get(0);
                    registerProblem(file, holder, firstOffset,
                        String.format("Group related options ('%s') in a <fieldset> with a <legend> so the question is read with each option",
                            entry.getKey()),
                        ProblemHighlightType.ERROR,
                        new WrapInFieldsetQuickFix("radio", entry.getKey()));
                }
            }
        }
    }
    
    private void checkCheckboxGroups(String content, PsiFile file, ProblemsHolder holder,
                                    Set<CheckboxGroup> fieldsetGroups) {
        // Find potential checkbox groups (multiple checkboxes close together)
        List<Integer> allCheckboxOffsets = new ArrayList<>();
        
        // HTML checkboxes
        Matcher checkboxMatcher = CHECKBOX_PATTERN.matcher(content);
        while (checkboxMatcher.find()) {
            allCheckboxOffsets.add(checkboxMatcher.start());
        }
        
        // Fluid checkboxes
        Matcher fluidCheckboxMatcher = FLUID_CHECKBOX_PATTERN.matcher(content);
        while (fluidCheckboxMatcher.find()) {
            allCheckboxOffsets.add(fluidCheckboxMatcher.start());
        }
        
        // Sort offsets
        Collections.sort(allCheckboxOffsets);
        
        // Find groups of checkboxes that are close together (within 500 chars)
        List<CheckboxGroup> potentialGroups = new ArrayList<>();
        List<Integer> currentGroup = new ArrayList<>();
        
        for (int i = 0; i < allCheckboxOffsets.size(); i++) {
            if (currentGroup.isEmpty()) {
                currentGroup.add(allCheckboxOffsets.get(i));
            } else {
                int lastOffset = currentGroup.get(currentGroup.size() - 1);
                int currentOffset = allCheckboxOffsets.get(i);
                
                if (currentOffset - lastOffset < 500) {
                    currentGroup.add(currentOffset);
                } else {
                    if (currentGroup.size() > 2) {
                        potentialGroups.add(new CheckboxGroup(new ArrayList<>(currentGroup)));
                    }
                    currentGroup.clear();
                    currentGroup.add(currentOffset);
                }
            }
        }
        
        if (currentGroup.size() > 2) {
            potentialGroups.add(new CheckboxGroup(currentGroup));
        }
        
        // Check each potential group
        for (CheckboxGroup group : potentialGroups) {
            boolean inFieldset = false;
            
            // Check if this group is in a fieldset
            for (CheckboxGroup fieldsetGroup : fieldsetGroups) {
                if (groupsOverlap(group, fieldsetGroup)) {
                    inFieldset = true;
                    break;
                }
            }
            
            if (!inFieldset) {
                // Report as warning (not error) since checkbox groups are less strict
                registerProblem(file, holder, group.offsets.get(0),
                    "Group related checkboxes in a <fieldset> with a <legend> so the purpose is announced",
                    ProblemHighlightType.WARNING,
                    new WrapInFieldsetQuickFix("checkbox", null));
            }
        }
    }
    
    private void checkFieldsetsHaveLegends(String content, PsiFile file, ProblemsHolder holder) {
        Matcher fieldsetMatcher = FIELDSET_PATTERN.matcher(content);
        
        while (fieldsetMatcher.find()) {
            String fieldsetContent = fieldsetMatcher.group();
            int offset = fieldsetMatcher.start();
            
            // Check if fieldset has a legend
            Matcher legendMatcher = LEGEND_PATTERN.matcher(fieldsetContent);
            if (!legendMatcher.find()) {
                registerProblem(file, holder, offset,
                    "Add a <legend> to this fieldset to label the group",
                    ProblemHighlightType.ERROR,
                    new AddLegendQuickFix());
            } else {
                // Check if legend is first child
                String legend = legendMatcher.group();
                int legendStart = legendMatcher.start();
                
                // Find first non-whitespace content after <fieldset>
                String afterFieldsetTag = fieldsetContent.substring(
                    fieldsetContent.indexOf('>') + 1).trim();
                
                if (!afterFieldsetTag.startsWith("<legend")) {
                    registerProblem(file, holder, offset + legendStart,
                        "Place the <legend> first inside the <fieldset> so it’s read as the group label",
                        ProblemHighlightType.WARNING,
                        null);
                }
                
                // Check if legend has content
                String legendContent = legend.replaceAll("<[^>]+>", "").trim();
                if (legendContent.isEmpty()) {
                    registerProblem(file, holder, offset + legendStart,
                        "Legend element is empty",
                        ProblemHighlightType.ERROR,
                        null);
                }
            }
        }
    }
    
    private boolean groupsOverlap(CheckboxGroup group1, CheckboxGroup group2) {
        for (Integer offset1 : group1.offsets) {
            for (Integer offset2 : group2.offsets) {
                if (Math.abs(offset1 - offset2) < 10) {
                    return true;
                }
            }
        }
        return false;
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
    
    // Helper classes
    protected static class RadioGroup {
        final String name;
        final List<Integer> offsets;
        
        RadioGroup(String name, List<Integer> offsets) {
            this.name = name;
            this.offsets = offsets;
        }
    }
    
    protected static class CheckboxGroup {
        final List<Integer> offsets;
        
        CheckboxGroup(List<Integer> offsets) {
            this.offsets = offsets;
        }
    }
    
    /**
     * Quick fix to wrap elements in fieldset with legend
     */
    private static class WrapInFieldsetQuickFix implements LocalQuickFix {
        private final String type;
        private final String groupName;
        
        public WrapInFieldsetQuickFix(String type, String groupName) {
            this.type = type;
            this.groupName = groupName;
        }
        
        @NotNull
        @Override
        public String getName() {
            return "Wrap in <fieldset> with <legend>";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add fieldset grouping";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // This is a simplified implementation
            // In a real scenario, you'd need to find all related elements and wrap them
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find the start of the current line
            int lineStart = startOffset;
            while (lineStart > 0 && fileText.charAt(lineStart - 1) != '\n') {
                lineStart--;
            }
            
            String legendText = groupName != null ? 
                "Select " + groupName : 
                "Select options";
            
            String fieldsetStart = "<fieldset>\n  <legend>" + legendText + "</legend>\n  ";
            String fieldsetEnd = "\n</fieldset>";
            
            // This is simplified - in reality, you'd need to find all related elements
            String newContent = fileText.substring(0, lineStart) +
                              fieldsetStart +
                              fileText.substring(lineStart, startOffset + 100) +
                              fieldsetEnd +
                              fileText.substring(startOffset + 100);
            
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
    
    /**
     * Quick fix to add legend to fieldset
     */
    private static class AddLegendQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() {
            return "Add <legend> element";
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add fieldset legend";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            
            PsiFile file = element.getContainingFile();
            if (file == null) return;
            
            String fileText = file.getText();
            int startOffset = element.getTextOffset();
            
            // Find the fieldset opening tag end
            int fieldsetEnd = fileText.indexOf('>', startOffset);
            if (fieldsetEnd == -1) return;
            
            String legend = "\n  <legend>Group description</legend>";
            
            String newContent = fileText.substring(0, fieldsetEnd + 1) +
                              legend +
                              fileText.substring(fieldsetEnd + 1);
            
            PsiFile newFile = PsiFileFactory.getInstance(project)
                .createFileFromText(file.getName(), file.getFileType(), newContent);
            file.getNode().replaceAllChildrenToChildrenOf(newFile.getNode());
        }
    }
}
