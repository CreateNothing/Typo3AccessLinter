package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.command.WriteCommandAction;
import com.intellij.openapi.editor.Document;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiDocumentManager;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Enhanced Modal Dialog Inspection with sophisticated accessibility validation.
 * Features:
 * - Focus management pattern validation
 * - Proper backdrop handling verification
 * - Keyboard trap implementation checking
 * - Modal dismissal method validation
 * - Focus restoration and containment analysis
 */
public class ModalDialogInspection extends FluidAccessibilityInspection {
    
    private static final Pattern DIALOG_PATTERN = Pattern.compile(
        "<[^>]+\\brole\\s*=\\s*[\"'](dialog|alertdialog)[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_MODAL_PATTERN = Pattern.compile(
        "\\baria-modal\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_LABELLEDBY_PATTERN = Pattern.compile(
        "\\baria-labelledby\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "\\baria-label\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ARIA_DESCRIBEDBY_PATTERN = Pattern.compile(
        "\\baria-describedby\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern TABINDEX_PATTERN = Pattern.compile(
        "\\btabindex\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern FOCUSABLE_ELEMENTS_PATTERN = Pattern.compile(
        "<(button|a|input|select|textarea|[^>]+\\btabindex\\s*=\\s*[\"'][0-9-]+[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CLOSE_BUTTON_PATTERN = Pattern.compile(
        "<button[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:close|dismiss|cancel)[^\"']*[\"']|" +
        "aria-label\\s*=\\s*[\"'][^\"']*(?:close|dismiss|cancel)[^\"']*[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    // Focus management patterns
    private static final Pattern FOCUS_TRAP_PATTERN = Pattern.compile(
        "(?:focus-trap|trap-focus|modal-focus)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Backdrop patterns
    private static final Pattern BACKDROP_PATTERN = Pattern.compile(
        "<div[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:backdrop|overlay|modal-backdrop)[^\"']*[\"']|" +
        "data-[^>]*backdrop)[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    // ESC key handler patterns
    private static final Pattern ESC_HANDLER_PATTERN = Pattern.compile(
        "(?:keydown|keyup|onkeydown|onkeyup).*?(?:27|Escape|ESC)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Focus restoration patterns
    private static final Pattern FOCUS_RESTORE_PATTERN = Pattern.compile(
        "(?:restoreFocus|returnFocus|previousFocus)",
        Pattern.CASE_INSENSITIVE
    );
    
    // Scroll lock patterns
    private static final Pattern SCROLL_LOCK_PATTERN = Pattern.compile(
        "(?:overflow\\s*:\\s*hidden|body-scroll-lock|scroll-lock)",
        Pattern.CASE_INSENSITIVE
    );
    
    @NotNull
    @Override
    public String getDisplayName() {
        return "Modal dialog accessibility issues";
    }
    
    @NotNull
    @Override
    public String getShortName() {
        return "ModalDialog";
    }
    
    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Find all dialog elements
        Matcher dialogMatcher = DIALOG_PATTERN.matcher(content);
        
        while (dialogMatcher.find()) {
            int dialogStart = dialogMatcher.start();
            int dialogEnd = findElementEnd(content, dialogStart);
            
            if (dialogEnd > dialogStart) {
                String dialogContent = content.substring(dialogStart, Math.min(dialogEnd, content.length()));
                String dialogTag = content.substring(dialogStart, dialogMatcher.end());
                
                // Check for aria-modal attribute
                checkAriaModal(dialogTag, file, holder, dialogStart, dialogMatcher.end());
                
                // Check for proper labeling
                checkDialogLabeling(dialogTag, file, holder, dialogStart, dialogMatcher.end());
                
                // Check for focusable elements
                checkFocusableElements(dialogContent, file, holder, dialogStart);
                
                // Check for close mechanism
                checkCloseButton(dialogContent, file, holder, dialogStart);
                
                // Check tabindex on dialog
                checkDialogTabindex(dialogTag, file, holder, dialogStart, dialogMatcher.end());
            }
        }
        
        // Check for modals without proper role
        checkModalsWithoutRole(content, file, holder);
        
        // Enhanced accessibility checks
        checkFocusManagement(content, file, holder);
        checkBackdropHandling(content, file, holder);
        checkKeyboardTrapImplementation(content, file, holder);
        checkDismissalMethods(content, file, holder);
    }
    
    private void checkAriaModal(String dialogTag, PsiFile file, ProblemsHolder holder, 
                                 int start, int end) {
        Matcher modalMatcher = ARIA_MODAL_PATTERN.matcher(dialogTag);
        
        if (!modalMatcher.find()) {
            registerProblem(holder, file, start, end,
                "Modal dialog should have aria-modal='true' to indicate it's a modal",
                new AddAriaModalFix());
        } else {
            String value = modalMatcher.group(1);
            if (!"true".equals(value)) {
                registerProblem(holder, file, start, end,
                    "Modal dialog should have aria-modal='true', not '" + value + "'",
                    new FixAriaModalValueFix());
            }
        }
    }
    
    private void checkDialogLabeling(String dialogTag, PsiFile file, ProblemsHolder holder,
                                      int start, int end) {
        boolean hasLabelledBy = ARIA_LABELLEDBY_PATTERN.matcher(dialogTag).find();
        boolean hasLabel = ARIA_LABEL_PATTERN.matcher(dialogTag).find();
        
        if (!hasLabelledBy && !hasLabel) {
            registerProblem(holder, file, start, end,
                "Dialog must have an accessible name via aria-labelledby or aria-label",
                new AddDialogLabelFix());
        }
        
        // Check if both are present (not an error, but could be confusing)
        if (hasLabelledBy && hasLabel) {
            registerProblem(holder, file, start, end,
                "Dialog has both aria-labelledby and aria-label. Consider using only one for clarity",
                null);
        }
    }
    
    private void checkFocusableElements(String dialogContent, PsiFile file, ProblemsHolder holder,
                                         int baseOffset) {
        Matcher focusableMatcher = FOCUSABLE_ELEMENTS_PATTERN.matcher(dialogContent);
        
        if (!focusableMatcher.find()) {
            registerProblems(holder, file, baseOffset, baseOffset + Math.min(100, dialogContent.length()),
                "Dialog should contain at least one focusable element",
                new LocalQuickFix[]{ new AddFocusableElementFix(), new AddProgrammaticFocusFix() });
        }
    }
    
    private void checkCloseButton(String dialogContent, PsiFile file, ProblemsHolder holder,
                                   int baseOffset) {
        // Check for close button or X button
        Matcher closeMatcher = CLOSE_BUTTON_PATTERN.matcher(dialogContent);
        
        if (!closeMatcher.find()) {
            // Also check for common close patterns
            Pattern xButtonPattern = Pattern.compile(
                "<button[^>]*>[×xX✕✖]</button>|" +
                "<button[^>]*aria-label\\s*=\\s*[\"'][^\"']*close[^\"']*[\"'][^>]*>",
                Pattern.CASE_INSENSITIVE
            );
            
            if (!xButtonPattern.matcher(dialogContent).find()) {
                registerProblem(holder, file, baseOffset, baseOffset + Math.min(100, dialogContent.length()),
                    "Dialog should have a clear close button or mechanism",
                    new AddCloseButtonFix());
            }
        }
    }
    
    private void checkDialogTabindex(String dialogTag, PsiFile file, ProblemsHolder holder,
                                      int start, int end) {
        Matcher tabindexMatcher = TABINDEX_PATTERN.matcher(dialogTag);
        
        if (tabindexMatcher.find()) {
            String value = tabindexMatcher.group(1);
            try {
                int tabindex = Integer.parseInt(value);
                if (tabindex > 0) {
                    registerProblem(holder, file, start, end,
                        "Dialog should not have positive tabindex. Use tabindex='-1' or '0'",
                        new FixDialogTabindexFix());
                }
            } catch (NumberFormatException e) {
                // Invalid tabindex value
                registerProblem(holder, file, start, end,
                    "Invalid tabindex value: " + value,
                    new FixDialogTabindexFix());
            }
        }
    }
    
    private void checkModalsWithoutRole(String content, PsiFile file, ProblemsHolder holder) {
        // Common modal patterns without proper role
        Pattern modalPattern = Pattern.compile(
            "<div[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:modal|dialog|popup|overlay)[^\"']*[\"']" +
            "|id\\s*=\\s*[\"'][^\"']*(?:modal|dialog|popup)[^\"']*[\"'])[^>]*>",
            Pattern.CASE_INSENSITIVE
        );
        
        Matcher modalMatcher = modalPattern.matcher(content);
        
        while (modalMatcher.find()) {
            String modalTag = modalMatcher.group();
            
            // Check if it already has a dialog role
            if (!DIALOG_PATTERN.matcher(modalTag).find()) {
                registerProblem(holder, file, modalMatcher.start(), modalMatcher.end(),
                    "Element appears to be a modal but lacks role='dialog' or role='alertdialog'",
                    new AddDialogRoleFix());
            }
        }
    }
    
    protected int findElementEnd(String content, int start) {
        // Simple approach to find the end of an element
        int depth = 0;
        int i = start;
        boolean inTag = false;
        boolean inQuote = false;
        char quoteChar = ' ';
        
        while (i < content.length()) {
            char c = content.charAt(i);
            
            if (!inQuote && (c == '"' || c == '\'')) {
                inQuote = true;
                quoteChar = c;
            } else if (inQuote && c == quoteChar) {
                inQuote = false;
            } else if (!inQuote) {
                if (c == '<') {
                    inTag = true;
                    if (i + 1 < content.length() && content.charAt(i + 1) == '/') {
                        depth--;
                        if (depth < 0) {
                            return i;
                        }
                    } else {
                        depth++;
                    }
                } else if (c == '>' && inTag) {
                    inTag = false;
                    if (i > 0 && content.charAt(i - 1) == '/') {
                        depth--;
                    }
                    if (depth == 0) {
                        return i + 1;
                    }
                }
            }
            i++;
        }
        
        return content.length();
    }
    
    
    private static class AddAriaModalFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-modal='true' to dialog";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            String text = element.getText();
            if (text.contains("aria-modal")) return;
            String updated = text.replaceFirst(">", " aria-modal=\"true\">");
            if (!updated.equals(text)) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document document = PsiDocumentManager.getInstance(project)
                        .getDocument(element.getContainingFile());
                    if (document != null) {
                        document.replaceString(element.getTextRange().getStartOffset(),
                                element.getTextRange().getEndOffset(), updated);
                    }
                });
            }
        }
    }
    
    private static class FixAriaModalValueFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Change aria-modal to 'true'";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            String text = element.getText();
            String updated = text.replaceAll("(\\baria-modal\\s*=\\s*[\"'])[^\"']+([\"'])", "$1true$2");
            if (!updated.equals(text)) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document document = PsiDocumentManager.getInstance(project)
                        .getDocument(element.getContainingFile());
                    if (document != null) {
                        document.replaceString(element.getTextRange().getStartOffset(),
                                element.getTextRange().getEndOffset(), updated);
                    }
                });
            }
        }
    }
    
    private static class AddDialogLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add aria-label to dialog";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            String text = element.getText();
            if (text.contains("aria-label") || text.contains("aria-labelledby")) return;
            // Prefer a minimal aria-label. If element inner text is simple, reuse it; otherwise use 'Dialog'
            String labelValue = "Dialog";
            String updated = text.replaceFirst(">", " aria-label=\"" + labelValue + "\">");
            if (!updated.equals(text)) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document document = PsiDocumentManager.getInstance(project)
                        .getDocument(element.getContainingFile());
                    if (document != null) {
                        document.replaceString(element.getTextRange().getStartOffset(),
                                element.getTextRange().getEndOffset(), updated);
                    }
                });
            }
        }
    }
    
    private static class AddFocusableElementFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add focusable element to dialog";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Conservative: make container focusable instead of injecting elements
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            String text = element.getText();
            if (text.contains("tabindex=")) return;
            String updated = text.replaceFirst(">", " tabindex=\"-1\">");
            if (!updated.equals(text)) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document document = PsiDocumentManager.getInstance(project)
                        .getDocument(element.getContainingFile());
                    if (document != null) {
                        document.replaceString(element.getTextRange().getStartOffset(),
                                element.getTextRange().getEndOffset(), updated);
                    }
                });
            }
        }
    }
    
    private static class AddCloseButtonFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add close button to dialog";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add a close button
        }
    }
    
    private static class FixDialogTabindexFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Fix dialog tabindex value";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            String text = element.getText();
            String updated = text.replaceAll("(\\btabindex\\s*=\\s*[\"'])[^\"']+([\"'])", "$1-1$2");
            if (!updated.equals(text)) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document document = PsiDocumentManager.getInstance(project)
                        .getDocument(element.getContainingFile());
                    if (document != null) {
                        document.replaceString(element.getTextRange().getStartOffset(),
                                element.getTextRange().getEndOffset(), updated);
                    }
                });
            }
        }
    }
    
    private static class AddDialogRoleFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add role='dialog' to modal element";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            String text = element.getText();
            if (text.contains(" role=")) return;
            String updated = text.replaceFirst(">", " role=\"dialog\">");
            if (!updated.equals(text)) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document document = PsiDocumentManager.getInstance(project)
                        .getDocument(element.getContainingFile());
                    if (document != null) {
                        document.replaceString(element.getTextRange().getStartOffset(),
                                element.getTextRange().getEndOffset(), updated);
                    }
                });
            }
        }
    }

    // New quick-fix: programmatic focus fallback
    private static class AddProgrammaticFocusFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Make dialog programmatically focusable";
        }

        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            PsiElement element = descriptor.getPsiElement();
            if (element == null) return;
            String text = element.getText();
            if (text.contains("tabindex=")) return;
            String updated = text.replaceFirst(">", " tabindex=\"-1\">");
            if (!updated.equals(text)) {
                WriteCommandAction.runWriteCommandAction(project, () -> {
                    Document document = PsiDocumentManager.getInstance(project)
                        .getDocument(element.getContainingFile());
                    if (document != null) {
                        document.replaceString(element.getTextRange().getStartOffset(),
                                element.getTextRange().getEndOffset(), updated);
                    }
                });
            }
        }
    }
    
    /**
     * Validate focus management patterns
     */
    private void checkFocusManagement(String content, PsiFile file, ProblemsHolder holder) {
        Matcher dialogMatcher = DIALOG_PATTERN.matcher(content);
        
        while (dialogMatcher.find()) {
            int dialogStart = dialogMatcher.start();
            int dialogEnd = findElementEnd(content, dialogStart);
            
            if (dialogEnd > dialogStart) {
                String dialogContent = content.substring(dialogStart, Math.min(dialogEnd, content.length()));
                
                // Check for initial focus target
                boolean hasAutoFocus = checkInitialFocus(dialogContent);
                if (!hasAutoFocus) {
                    registerProblem(holder, file, dialogStart, dialogStart + 100,
                        "Modal should have an initial focus target. Add autofocus or programmatic focus",
                        new AddInitialFocusQuickFix());
                }
                
                // Check for focus containment
                boolean hasFocusTrap = FOCUS_TRAP_PATTERN.matcher(content).find() || 
                                      checkFocusContainment(dialogContent);
                if (!hasFocusTrap) {
                    registerProblem(holder, file, dialogStart, dialogStart + 100,
                        "Modal should contain focus within the dialog. Implement focus trapping",
                        new AddFocusTrapQuickFix());
                }
                
                // Check for focus restoration
                boolean hasFocusRestore = FOCUS_RESTORE_PATTERN.matcher(content).find();
                if (!hasFocusRestore) {
                    registerProblem(holder, file, dialogStart, dialogStart + 100,
                        "Modal should restore focus to trigger element when closed",
                        new AddFocusRestorationQuickFix());
                }
            }
        }
    }
    
    private boolean checkInitialFocus(String dialogContent) {
        // Check for autofocus attribute
        if (dialogContent.contains("autofocus")) {
            return true;
        }
        
        // Check for programmatic focus patterns
        Pattern focusPattern = Pattern.compile(
            "(?:focus\\(\\)|requestFocus|setFocus)",
            Pattern.CASE_INSENSITIVE);
        return focusPattern.matcher(dialogContent).find();
    }
    
    private boolean checkFocusContainment(String dialogContent) {
        // Check for Tab key handling
        Pattern tabHandlerPattern = Pattern.compile(
            "(?:keydown|keyup).*?(?:9|Tab).*?(?:preventDefault|stopPropagation)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        
        return tabHandlerPattern.matcher(dialogContent).find() ||
               dialogContent.contains("tabindex=-1") ||
               dialogContent.contains("inert");
    }
    
    /**
     * Check for proper backdrop handling
     */
    private void checkBackdropHandling(String content, PsiFile file, ProblemsHolder holder) {
        Matcher dialogMatcher = DIALOG_PATTERN.matcher(content);
        
        while (dialogMatcher.find()) {
            int dialogStart = dialogMatcher.start();
            
            // Look for backdrop element
            boolean hasBackdrop = BACKDROP_PATTERN.matcher(content).find();
            
            if (hasBackdrop) {
                // Check if backdrop is properly configured
                Matcher backdropMatcher = BACKDROP_PATTERN.matcher(content);
                while (backdropMatcher.find()) {
                    String backdropElement = backdropMatcher.group();
                    
                    // Check if backdrop has click handler for dismissal
                    if (!backdropElement.contains("onclick") && !backdropElement.contains("data-dismiss")) {
                        registerProblem(holder, file, backdropMatcher.start(), backdropMatcher.end(),
                            "Modal backdrop should be clickable to dismiss modal (if appropriate for your use case)",
                            new AddBackdropDismissalQuickFix());
                    }
                    
                    // Check if backdrop prevents interaction with background content
                    if (!backdropElement.contains("z-index") && !backdropElement.contains("position")) {
                        registerProblem(holder, file, backdropMatcher.start(), backdropMatcher.end(),
                            "Modal backdrop should use proper positioning and z-index to prevent background interaction",
                            null);
                    }
                }
                
                // Check for body scroll prevention
                boolean hasScrollLock = SCROLL_LOCK_PATTERN.matcher(content).find();
                if (!hasScrollLock) {
                    registerProblem(holder, file, dialogStart, dialogStart + 100,
                        "Modal should prevent background scrolling when open",
                        new AddScrollLockQuickFix());
                }
            } else {
                registerProblem(holder, file, dialogStart, dialogStart + 100,
                    "Modal should have a backdrop to prevent interaction with background content",
                    new AddBackdropQuickFix());
            }
        }
    }
    
    /**
     * Validate keyboard trap implementation
     */
    private void checkKeyboardTrapImplementation(String content, PsiFile file, ProblemsHolder holder) {
        Matcher dialogMatcher = DIALOG_PATTERN.matcher(content);
        
        while (dialogMatcher.find()) {
            int dialogStart = dialogMatcher.start();
            int dialogEnd = findElementEnd(content, dialogStart);
            
            if (dialogEnd > dialogStart) {
                String dialogContent = content.substring(dialogStart, Math.min(dialogEnd, content.length()));
                
                // Check for Tab key handling
                boolean hasTabHandling = checkTabKeyHandling(dialogContent);
                if (!hasTabHandling) {
                    registerProblem(holder, file, dialogStart, dialogStart + 100,
                        "Modal should handle Tab and Shift+Tab keys to maintain focus within dialog",
                        new AddTabHandlingQuickFix());
                }
                
                // Check for proper focusable element identification
                int focusableCount = countFocusableElements(dialogContent);
                if (focusableCount < 2) {
                    registerProblem(holder, file, dialogStart, dialogStart + 100,
                        "Modal with only one focusable element may not need focus trapping, but should handle Escape key",
                        null);
                }
                
                // Validate focus cycling
                boolean hasFocusCycling = checkFocusCycling(dialogContent);
                if (focusableCount > 1 && !hasFocusCycling) {
                    registerProblem(holder, file, dialogStart, dialogStart + 100,
                        "Modal should implement proper focus cycling between first and last focusable elements",
                        new AddFocusCyclingQuickFix());
                }
            }
        }
    }
    
    private boolean checkTabKeyHandling(String dialogContent) {
        Pattern tabPattern = Pattern.compile(
            "(?:keydown|keyup).*?(?:which|keyCode|key).*?(?:9|Tab)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return tabPattern.matcher(dialogContent).find();
    }
    
    private int countFocusableElements(String dialogContent) {
        int count = 0;
        
        // Count common focusable elements
        count += countOccurrences(dialogContent, "<button");
        count += countOccurrences(dialogContent, "<a ");
        count += countOccurrences(dialogContent, "<input");
        count += countOccurrences(dialogContent, "<select");
        count += countOccurrences(dialogContent, "<textarea");
        
        // Count elements with tabindex
        Pattern tabindexPattern = Pattern.compile("tabindex\\s*=\\s*[\"'][0-9]+[\"']", Pattern.CASE_INSENSITIVE);
        Matcher matcher = tabindexPattern.matcher(dialogContent);
        while (matcher.find()) {
            count++;
        }
        
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
    
    private boolean checkFocusCycling(String dialogContent) {
        // Look for first/last element handling
        return dialogContent.contains("firstFocusable") ||
               dialogContent.contains("lastFocusable") ||
               dialogContent.contains("querySelector") && dialogContent.contains("focusable");
    }
    
    /**
     * Ensure proper modal dismissal methods
     */
    private void checkDismissalMethods(String content, PsiFile file, ProblemsHolder holder) {
        Matcher dialogMatcher = DIALOG_PATTERN.matcher(content);
        
        while (dialogMatcher.find()) {
            int dialogStart = dialogMatcher.start();
            int dialogEnd = findElementEnd(content, dialogStart);
            
            if (dialogEnd > dialogStart) {
                String dialogContent = content.substring(dialogStart, Math.min(dialogEnd, content.length()));
                
                // Check for Escape key handling
                boolean hasEscapeHandling = ESC_HANDLER_PATTERN.matcher(content).find() ||
                                          checkEscapeKeyHandling(dialogContent);
                if (!hasEscapeHandling) {
                    registerProblem(holder, file, dialogStart, dialogStart + 100,
                        "Modal should be dismissible with Escape key",
                        new AddEscapeHandlingQuickFix());
                }
                
                // Check for multiple dismissal methods
                int dismissalMethods = countDismissalMethods(dialogContent);
                if (dismissalMethods < 2) {
                    registerProblem(holder, file, dialogStart, dialogStart + 100,
                        "Modal should provide multiple dismissal methods (close button, Escape key, backdrop click)",
                        new AddDismissalMethodsQuickFix());
                }
                
                // Check for close button accessibility
                checkCloseButtonAccessibility(dialogContent, file, holder, dialogStart);
            }
        }
    }
    
    private boolean checkEscapeKeyHandling(String dialogContent) {
        Pattern escPattern = Pattern.compile(
            "(?:keydown|keyup).*?(?:which|keyCode|key).*?(?:27|Escape|ESC)",
            Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        return escPattern.matcher(dialogContent).find();
    }
    
    private int countDismissalMethods(String dialogContent) {
        int count = 0;
        
        // Count close buttons
        if (CLOSE_BUTTON_PATTERN.matcher(dialogContent).find()) count++;
        
        // Count escape key handlers
        if (ESC_HANDLER_PATTERN.matcher(dialogContent).find()) count++;
        
        // Count backdrop dismissal
        if (dialogContent.contains("backdrop") && dialogContent.contains("click")) count++;
        
        return count;
    }
    
    private void checkCloseButtonAccessibility(String dialogContent, PsiFile file, ProblemsHolder holder, int baseOffset) {
        Matcher closeMatcher = CLOSE_BUTTON_PATTERN.matcher(dialogContent);
        
        while (closeMatcher.find()) {
            String closeButton = closeMatcher.group();
            
            // Check for accessible name
            if (!closeButton.contains("aria-label") && !closeButton.contains(">×</") && !closeButton.contains(">Close</")) {
                registerProblem(holder, file, baseOffset + closeMatcher.start(), baseOffset + closeMatcher.end(),
                    "Close button should have accessible name (aria-label or visible text)",
                    new AddCloseButtonLabelQuickFix());
            }
            
            // Check for proper keyboard access
            if (closeButton.contains("tabindex=\"-1\"")) {
                registerProblem(holder, file, baseOffset + closeMatcher.start(), baseOffset + closeMatcher.end(),
                    "Close button should be keyboard accessible (remove tabindex='-1')",
                    new FixCloseButtonTabindexQuickFix());
            }
        }
    }
    
    // Enhanced Quick Fixes
    private static class AddInitialFocusQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add initial focus to modal";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add autofocus or programmatic focus
        }
    }
    
    private static class AddFocusTrapQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Implement focus trapping";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add focus trap functionality
        }
    }
    
    private static class AddFocusRestorationQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add focus restoration";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add focus restoration code
        }
    }
    
    private static class AddBackdropDismissalQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add backdrop click dismissal";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add backdrop click handler
        }
    }
    
    private static class AddScrollLockQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add background scroll prevention";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add scroll lock functionality
        }
    }
    
    private static class AddBackdropQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add modal backdrop";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add backdrop element
        }
    }
    
    private static class AddTabHandlingQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add Tab key handling for focus trap";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add Tab key event handling
        }
    }
    
    private static class AddFocusCyclingQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Implement focus cycling";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add focus cycling between first/last elements
        }
    }
    
    private static class AddEscapeHandlingQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add Escape key dismissal";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add Escape key handler
        }
    }
    
    private static class AddDismissalMethodsQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add multiple dismissal methods";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest adding various dismissal methods
        }
    }
    
    private static class AddCloseButtonLabelQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add accessible label to close button";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add aria-label to close button
        }
    }
    
    private static class FixCloseButtonTabindexQuickFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getName() { return getFamilyName(); }
        @NotNull
        @Override
        public String getFamilyName() {
            return "Make close button keyboard accessible";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would remove negative tabindex
        }
    }
}
