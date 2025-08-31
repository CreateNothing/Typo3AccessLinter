package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiRecursiveElementVisitor;
import com.intellij.psi.xml.XmlTag;
import com.intellij.psi.xml.XmlText;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;
import com.typo3.fluid.linter.utils.AccessibilityUtils;

import java.util.*;

/**
 * PSI-first validation strategy for link text accessibility.
 */
public class LinkTextValidationStrategy implements ValidationStrategy {

    private static final Set<String> NON_DESCRIPTIVE_PHRASES = new HashSet<>(Arrays.asList(
            "click here", "here", "read more", "learn more", "more", "click", "download",
            "link", "this link", "this page", "more info", "info", "details", "view",
            "see more", "continue", "go", "start", "submit", "follow this link",
            "click this link", "visit", "check out", "click to", "go to", "tap here",
            "press here", "follow", "open", "view details", "more details",
            "further information", "additional information", "click for more",
            "find out", "discover", "explore"
    ));

    private static final Set<String> CONTEXTUAL_PHRASES = new HashSet<>(Arrays.asList(
            "read more", "learn more", "see more", "view more", "details",
            "more info", "continue reading", "find out more"
    ));

    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        List<LinkInfo> links = collectLinksPsi(file);

        for (LinkInfo link : links) {
            if (link.linkText.isEmpty()) {
                if (!link.hasValidAriaLabel()) {
                    // Offer to add aria-label
                    com.typo3.fluid.linter.fixes.FixContext ctx = new com.typo3.fluid.linter.fixes.FixContext("missing-attribute");
                    ctx.setAttribute("attributeName", "aria-label");
                    com.intellij.codeInspection.LocalQuickFix[] fixes = com.typo3.fluid.linter.fixes.FixRegistry.getInstance()
                            .getFixes(file, link.start, link.end, ctx);
                            results.add(new ValidationResult(
                                    link.start, link.end,
                                    link.hasIcon ?
                                            "Icon-only link should have aria-label or meaningful text to be accessible" :
                                            "Link has no text content and no accessible label",
                                    fixes
                            ));
                }
            } else {
                String lowerText = link.linkText.toLowerCase().trim();
                if (NON_DESCRIPTIVE_PHRASES.contains(lowerText)) {
                    if (CONTEXTUAL_PHRASES.contains(lowerText)) {
                        if (!hasDescriptiveContext(content, link.start)) {
                            // Offer to add aria-label for better accessible name
                            com.typo3.fluid.linter.fixes.FixContext ctx = new com.typo3.fluid.linter.fixes.FixContext("missing-attribute");
                            ctx.setAttribute("attributeName", "aria-label");
                            com.intellij.codeInspection.LocalQuickFix[] fixes = com.typo3.fluid.linter.fixes.FixRegistry.getInstance()
                                    .getFixes(file, link.start, link.end, ctx);
                            results.add(new ValidationResult(
                                    link.start, link.end,
                                    String.format("Link text '%s' needs context. Either improve the link text or ensure preceding text describes the destination", link.linkText),
                                    fixes
                            ));
                        }
                    } else {
                        com.typo3.fluid.linter.fixes.FixContext ctx = new com.typo3.fluid.linter.fixes.FixContext("missing-attribute");
                        ctx.setAttribute("attributeName", "aria-label");
                        com.intellij.codeInspection.LocalQuickFix[] fixes = com.typo3.fluid.linter.fixes.FixRegistry.getInstance()
                                .getFixes(file, link.start, link.end, ctx);
                        results.add(new ValidationResult(
                                link.start, link.end,
                                String.format("Link text '%s' is not descriptive. Links should clearly describe their destination or purpose", link.linkText),
                                fixes
                        ));
                    }
                }

                if (link.linkText.matches("^[a-zA-Z]$") && !link.hasValidAriaLabel()) {
                    com.typo3.fluid.linter.fixes.FixContext ctx = new com.typo3.fluid.linter.fixes.FixContext("missing-attribute");
                    ctx.setAttribute("attributeName", "aria-label");
                    com.intellij.codeInspection.LocalQuickFix[] fixes = com.typo3.fluid.linter.fixes.FixRegistry.getInstance()
                            .getFixes(file, link.start, link.end, ctx);
                    results.add(new ValidationResult(
                            link.start, link.end,
                            String.format("Single character '%s' as link text is not descriptive. Add aria-label or use descriptive text", link.linkText),
                            fixes
                    ));
                }

                if (isUrlText(lowerText)) {
                    com.typo3.fluid.linter.fixes.FixContext ctx = new com.typo3.fluid.linter.fixes.FixContext("missing-attribute");
                    ctx.setAttribute("attributeName", "aria-label");
                    com.intellij.codeInspection.LocalQuickFix[] fixes = com.typo3.fluid.linter.fixes.FixRegistry.getInstance()
                            .getFixes(file, link.start, link.end, ctx);
                    results.add(new ValidationResult(
                            link.start, link.end,
                            "URL as link text is not user-friendly. Use descriptive text that explains the link's purpose",
                            fixes
                    ));
                }

                if (link.linkText.length() > 100) {
                    results.add(new ValidationResult(
                            link.start, link.end,
                            String.format("Link text is too long (%d characters). Consider making it more concise (under 100 characters)", link.linkText.length())
                    ));
                }
            }
        }

        checkDuplicateLinks(links, results);
        return results;
    }

    private List<LinkInfo> collectLinksPsi(PsiFile file) {
        List<LinkInfo> links = new ArrayList<>();
        file.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (element instanceof XmlTag) {
                    XmlTag tag = (XmlTag) element;
                    String name = tag.getName();
                    if (isAnchorTag(name) || isFluidLinkTag(name)) {
                        links.add(extractLinkInfo(tag));
                    }
                }
                super.visitElement(element);
            }
        });
        return links;
    }

    private boolean isAnchorTag(String name) {
        return "a".equalsIgnoreCase(name);
    }

    private boolean isFluidLinkTag(String name) {
        String lower = name.toLowerCase();
        return lower.equals("f:link") || lower.startsWith("f:link.") || lower.endsWith(":link") || lower.contains(":link.");
    }

    private LinkInfo extractLinkInfo(XmlTag tag) {
        String inner = tag.getValue().getText();
        String linkText = AccessibilityUtils.extractTextContent(inner).trim();
        String ariaLabel = firstNonEmpty(tag.getAttributeValue("aria-label"), tag.getAttributeValue("aria-labelledby"));
        String href = tag.getAttributeValue("href");
        boolean hasIcon = hasIconDescendant(tag);
        int start = tag.getTextRange().getStartOffset();
        int end = tag.getTextRange().getEndOffset();
        return new LinkInfo(start, end, linkText, href, ariaLabel, hasIcon);
    }

    private boolean hasIconDescendant(XmlTag tag) {
        final boolean[] found = {false};
        tag.accept(new PsiRecursiveElementVisitor() {
            @Override
            public void visitElement(@org.jetbrains.annotations.NotNull PsiElement element) {
                if (found[0]) return;
                if (element instanceof XmlTag) {
                    XmlTag t = (XmlTag) element;
                    String n = t.getName().toLowerCase();
                    if (n.equals("svg")) { found[0] = true; return; }
                    if (n.equals("img")) {
                        String cls = t.getAttributeValue("class");
                        String src = t.getAttributeValue("src");
                        if ((cls != null && cls.toLowerCase().contains("icon")) || (src != null && src.toLowerCase().contains("icon"))) {
                            found[0] = true; return;
                        }
                    }
                    if (n.equals("i") || n.equals("span")) {
                        String cls = t.getAttributeValue("class");
                        if (cls != null) {
                            String l = cls.toLowerCase();
                            if (l.contains("icon") || l.contains("fa-") || l.contains("glyphicon") || l.contains("material-icons") || l.contains("bi-") || l.contains("ion-")) {
                                found[0] = true; return;
                            }
                        }
                    }
                }
                super.visitElement(element);
            }
        });
        return found[0];
    }

    private void checkDuplicateLinks(List<LinkInfo> links, List<ValidationResult> results) {
        Map<String, List<LinkInfo>> linksByText = new HashMap<>();
        for (LinkInfo link : links) {
            if (!link.linkText.isEmpty()) {
                String key = link.linkText.toLowerCase().trim();
                linksByText.computeIfAbsent(key, k -> new ArrayList<>()).add(link);
            }
        }
        for (Map.Entry<String, List<LinkInfo>> entry : linksByText.entrySet()) {
            List<LinkInfo> dups = entry.getValue();
            if (dups.size() > 1) {
                Set<String> destinations = new HashSet<>();
                for (LinkInfo link : dups) destinations.add(link.href != null ? link.href : "");
                if (destinations.size() > 1) {
                    for (LinkInfo link : dups) {
                        results.add(new ValidationResult(
                                link.start, link.end,
                                String.format("Multiple links with text '%s' point to different destinations. Make link text more specific", entry.getKey())
                        ));
                    }
                }
            }
        }
    }

    private boolean hasDescriptiveContext(String content, int linkStart) {
        int contextStart = Math.max(0, linkStart - 200);
        String context = content.substring(contextStart, linkStart);
        context = AccessibilityUtils.extractTextContent(context).toLowerCase();
        return context.contains("article") || context.contains("blog") ||
                context.contains("story") || context.contains("about") ||
                context.contains("guide") || context.contains("tutorial") ||
                context.contains("news") || context.contains("product") ||
                context.contains("service") || context.contains("feature");
    }

    private boolean isUrlText(String text) {
        return text.matches("^(https?://|ftp://|www\\.).*") ||
                text.contains("http://") || text.contains("https://") ||
                text.matches(".*\\.[a-z]{2,4}/.*");
    }

    private String firstNonEmpty(String a, String b) {
        if (a != null && !a.trim().isEmpty()) return a;
        if (b != null && !b.trim().isEmpty()) return b;
        return null;
    }

    @Override
    public int getPriority() {
        return 95;
    }

    @Override
    public boolean shouldApply(PsiFile file) {
        String content = file.getText();
        return content.contains("<a ") || content.contains("<a>") || content.contains("<f:link");
    }

    private static class LinkInfo {
        final int start;
        final int end;
        final String linkText;
        final String href;
        final String ariaLabel;
        final boolean hasIcon;

        LinkInfo(int start, int end, String linkText, String href, String ariaLabel, boolean hasIcon) {
            this.start = start;
            this.end = end;
            this.linkText = linkText;
            this.href = href;
            this.ariaLabel = ariaLabel;
            this.hasIcon = hasIcon;
        }

        boolean hasValidAriaLabel() {
            return ariaLabel != null && !ariaLabel.trim().isEmpty() &&
                    !NON_DESCRIPTIVE_PHRASES.contains(ariaLabel.toLowerCase().trim());
        }
    }
}
