package com.typo3.fluid.linter.strategy.implementations;

import com.intellij.psi.PsiFile;
import com.typo3.fluid.linter.strategy.ValidationResult;
import com.typo3.fluid.linter.strategy.ValidationStrategy;
import com.typo3.fluid.linter.utils.AccessibilityUtils;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Validation strategy for link text accessibility.
 * Migrated from LinkTextInspection to work with the new strategy pattern.
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
    
    private static final Pattern LINK_PATTERN = Pattern.compile(
        "<(a\\s+[^>]*|f:link(?:\\.[^\\s>]+)?[^>]*)>(.*?)</(a|f:link(?:\\.[^>]*)?)>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern ARIA_LABEL_PATTERN = Pattern.compile(
        "aria-label(?:ledby)?\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HREF_PATTERN = Pattern.compile(
        "href\\s*=\\s*[\"']([^\"']+)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern ICON_PATTERN = Pattern.compile(
        "<(?:i|span)\\s+[^>]*class\\s*=\\s*[\"'][^\"']*(?:icon|fa-|fas|far|fab|glyphicon|material-icons|bi-|ion-)[^\"']*[\"'][^>]*>|" +
        "<img\\s+[^>]*(?:class\\s*=\\s*[\"'][^\"']*icon[^\"']*[\"']|src\\s*=\\s*[\"'][^\"']*icon[^\"']*[\"'])[^>]*>|" +
        "<svg\\s+[^>]*(?:class\\s*=\\s*[\"'][^\"']*icon[^\"']*[\"'])?[^>]*>.*?</svg>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    @Override
    public List<ValidationResult> validate(PsiFile file, String content) {
        List<ValidationResult> results = new ArrayList<>();
        List<LinkInfo> links = collectLinks(content);
        
        for (LinkInfo link : links) {
            // Check for empty links
            if (link.linkText.isEmpty()) {
                if (!link.hasValidAriaLabel()) {
                    if (link.hasIcon) {
                        results.add(new ValidationResult(
                            link.start, link.end,
                            "Icon-only link must have aria-label or meaningful text to be accessible"
                        ));
                    } else {
                        results.add(new ValidationResult(
                            link.start, link.end,
                            "Link has no text content and no accessible label"
                        ));
                    }
                }
            } else {
                // Check for non-descriptive text
                String lowerText = link.linkText.toLowerCase().trim();
                if (NON_DESCRIPTIVE_PHRASES.contains(lowerText)) {
                    if (CONTEXTUAL_PHRASES.contains(lowerText)) {
                        if (!hasDescriptiveContext(content, link.start, lowerText)) {
                            results.add(new ValidationResult(
                                link.start, link.end,
                                String.format("Link text '%s' needs context. Either improve the link text or ensure preceding text describes the destination", link.linkText)
                            ));
                        }
                    } else {
                        results.add(new ValidationResult(
                            link.start, link.end,
                            String.format("Link text '%s' is not descriptive. Links should clearly describe their destination or purpose", link.linkText)
                        ));
                    }
                }
                
                // Check for single character links
                if (link.linkText.matches("^[a-zA-Z]$") && !link.hasValidAriaLabel()) {
                    results.add(new ValidationResult(
                        link.start, link.end,
                        String.format("Single character '%s' as link text is not descriptive. Add aria-label or use descriptive text", link.linkText)
                    ));
                }
                
                // Check for URL as text
                if (isUrlText(link.linkText.toLowerCase())) {
                    results.add(new ValidationResult(
                        link.start, link.end,
                        "URL as link text is not user-friendly. Use descriptive text that explains the link's purpose"
                    ));
                }
                
                // Check for overly long link text
                if (link.linkText.length() > 100) {
                    results.add(new ValidationResult(
                        link.start, link.end,
                        String.format("Link text is too long (%d characters). Consider making it more concise (under 100 characters)", link.linkText.length())
                    ));
                }
            }
        }
        
        // Check for duplicate links with different destinations
        checkDuplicateLinks(links, results);
        
        return results;
    }
    
    private List<LinkInfo> collectLinks(String content) {
        List<LinkInfo> links = new ArrayList<>();
        Matcher matcher = LINK_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String attributes = matcher.group(1) != null ? matcher.group(1) : "";
            String linkContent = matcher.group(2) != null ? matcher.group(2) : "";
            String linkText = AccessibilityUtils.extractTextContent(linkContent).trim();
            
            Matcher ariaLabelMatcher = ARIA_LABEL_PATTERN.matcher(attributes);
            String ariaLabel = ariaLabelMatcher.find() ? ariaLabelMatcher.group(1) : null;
            
            Matcher hrefMatcher = HREF_PATTERN.matcher(attributes);
            String href = hrefMatcher.find() ? hrefMatcher.group(1) : null;
            
            boolean hasIcon = ICON_PATTERN.matcher(linkContent).find();
            
            links.add(new LinkInfo(
                matcher.start(), matcher.end(),
                linkText, href, ariaLabel, hasIcon
            ));
        }
        
        return links;
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
            List<LinkInfo> duplicateLinks = entry.getValue();
            if (duplicateLinks.size() > 1) {
                Set<String> destinations = new HashSet<>();
                for (LinkInfo link : duplicateLinks) {
                    destinations.add(link.href != null ? link.href : "");
                }
                
                if (destinations.size() > 1) {
                    for (LinkInfo link : duplicateLinks) {
                        results.add(new ValidationResult(
                            link.start, link.end,
                            String.format("Multiple links with text '%s' point to different destinations. Make link text more specific", entry.getKey())
                        ));
                    }
                }
            }
        }
    }
    
    private boolean hasDescriptiveContext(String content, int linkStart, String linkText) {
        // Extract context before the link
        int contextStart = Math.max(0, linkStart - 200);
        String context = content.substring(contextStart, linkStart);
        context = AccessibilityUtils.extractTextContent(context).toLowerCase();
        
        // Look for descriptive words that provide context
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
    
    @Override
    public int getPriority() {
        return 100; // High priority for link text validation
    }
    
    @Override
    public boolean shouldApply(PsiFile file) {
        String content = file.getText();
        return content.contains("<a ") || content.contains("<a>") || 
               content.contains("<f:link");
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