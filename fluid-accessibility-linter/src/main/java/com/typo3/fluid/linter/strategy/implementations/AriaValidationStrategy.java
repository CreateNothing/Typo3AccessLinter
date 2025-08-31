package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.utils.AccessibilityUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * PSI-based ARIA role validation with basic semantics:
 * - Invalid/abstract roles
 * - Redundant roles on semantic elements (e.g., <button role="button">)
 * - Conflicting roles (e.g., role="presentation" on interactive elements)
 * - Multiple roles specified
 */
public class AriaValidationStrategy extends BaseValidationStrategy {

    private static final Set<String> INTERACTIVE_TAGS = new HashSet<>(Arrays.asList(
            "a", "button", "input", "select", "textarea"
    ));

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();

        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String roleAttr = tag.getAttributeValue("role");
                    if (roleAttr != null) {
                        int start = tag.getTextRange().getStartOffset();
                        int end = tag.getTextRange().getEndOffset();

                        // Multiple roles specified
                        String[] roles = roleAttr.trim().split("\\s+");
                        if (roles.length > 1) {
                            results.add(new ValidationResult(start, end,
                                    "Multiple ARIA roles specified; use only one definitive role"));
                        }
                        String role = roles[0].trim();

                        // Invalid or abstract role
                        if (!AccessibilityUtils.isValidARIARole(role)) {
                            if (AccessibilityUtils.isAbstractARIARole(role)) {
                                results.add(new ValidationResult(start, end,
                                        "Abstract ARIA role should not be used: " + role));
                            } else {
                                results.add(new ValidationResult(start, end,
                                        "Invalid ARIA role: " + role));
                            }
                        }

                        // Redundant roles on semantic elements
                        String implicit = inferImplicitRole(tag);
                        if (implicit != null && implicit.equalsIgnoreCase(role)) {
                            results.add(new ValidationResult(start, end,
                                    "Redundant role on semantic element: role='" + role + "'"));
                        }

                        // Conflicting roles: presentation/none on interactive elements
                        if (("presentation".equalsIgnoreCase(role) || "none".equalsIgnoreCase(role)) && isInteractive(tag)) {
                            results.add(new ValidationResult(start, end,
                                    "Interactive element should not have role='" + role + "'"));
                        }

                        // Required ARIA attributes for certain roles
                        for (String required : requiredAttributesForRole(role)) {
                            if (required.contains("|")) {
                                // one-of requirement, e.g., aria-label|aria-labelledby
                                String[] options = required.split("\\|");
                                if (!hasAny(tag, options)) {
                                    results.add(new ValidationResult(start, end,
                                            "Role '" + role + "' requires one of: " + String.join(", ", options)));
                                }
                            } else if (tag.getAttributeValue(required) == null) {
                                results.add(new ValidationResult(start, end,
                                        "Role '" + role + "' requires attribute '" + required + "'"));
                            }
                        }
                    }
                }
                super.visitElement(element);
            }
        });

        return results;
    }

    private boolean isInteractive(XmlTag tag) {
        String name = tag.getName().toLowerCase();
        if (INTERACTIVE_TAGS.contains(name)) {
            if (name.equals("a")) {
                String href = tag.getAttributeValue("href");
                return href != null && !href.trim().isEmpty();
            }
            if (name.equals("input")) {
                String type = tag.getAttributeValue("type");
                if (type == null) return true;
                String lower = type.toLowerCase();
                return !lower.equals("hidden");
            }
            return true;
        }
        return false;
    }

    private String inferImplicitRole(XmlTag tag) {
        String name = tag.getName().toLowerCase();
        switch (name) {
            case "a":
                return tag.getAttributeValue("href") != null ? "link" : null;
            case "button":
                return "button";
            case "nav":
                return "navigation";
            case "ul":
                return "list";
            case "ol":
                return "list";
            case "li":
                return "listitem";
            case "img":
                return "img";
            case "table":
                return "table";
            case "header":
                return "banner";
            case "footer":
                return "contentinfo";
            case "main":
                return "main";
            case "form":
                return "form";
            case "aside":
                return "complementary";
            case "section":
                return "region";
        }
        return null;
    }

    private List<String> requiredAttributesForRole(String role) {
        String r = role.toLowerCase();
        switch (r) {
            case "slider":
                return Arrays.asList("aria-valuemin", "aria-valuemax", "aria-valuenow");
            case "scrollbar":
                return Arrays.asList("aria-valuemin", "aria-valuemax", "aria-valuenow");
            case "spinbutton":
                return Arrays.asList("aria-valuenow");
            case "progressbar":
                return Arrays.asList("aria-valuenow", "aria-valuemin", "aria-valuemax");
            case "checkbox":
            case "radio":
            case "switch":
            case "menuitemcheckbox":
            case "menuitemradio":
                return Arrays.asList("aria-checked");
            case "tab":
                return Arrays.asList("aria-selected");
            case "combobox":
                return Arrays.asList("aria-expanded");
            case "tabpanel":
                return Arrays.asList("aria-label|aria-labelledby");
            default:
                return java.util.Collections.emptyList();
        }
    }

    private boolean hasAny(XmlTag tag, String[] attrs) {
        for (String a : attrs) {
            if (tag.getAttributeValue(a) != null) return true;
        }
        return false;
    }

    @Override
    public int getPriority() {
        return 80;
    }
}
