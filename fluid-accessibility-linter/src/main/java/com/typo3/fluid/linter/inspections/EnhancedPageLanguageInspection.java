package com.typo3.fluid.linter.inspections;

import com.intellij.codeInspection.LocalQuickFix;
import com.intellij.codeInspection.ProblemDescriptor;
import com.intellij.codeInspection.ProblemHighlightType;
import com.intellij.codeInspection.ProblemsHolder;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.PsiFileFactory;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class EnhancedPageLanguageInspection extends PageLanguageInspection {
    
    // Enhanced patterns for multilingual content analysis
    private static final Pattern LANG_SWITCH_PATTERN = Pattern.compile(
        "<[^>]*(?:class\\s*=\\s*[\"'][^\"']*(?:lang-switch|language|i18n)[^\"']*[\"']|" +
        "data-lang\\s*=\\s*[\"'][^\"']+[\"'])[^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern CONTENT_LANG_PATTERN = Pattern.compile(
        "<[^>]*\\blang\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>(.*?)</[^>]+>",
        Pattern.CASE_INSENSITIVE | Pattern.DOTALL
    );
    
    private static final Pattern META_LANG_PATTERN = Pattern.compile(
        "<meta[^>]*(?:name\\s*=\\s*[\"'](?:language|locale)[\"']|" +
        "property\\s*=\\s*[\"']og:locale[\"'])[^>]*content\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern HREFLANG_PATTERN = Pattern.compile(
        "<link[^>]*hreflang\\s*=\\s*[\"']([^\"']+)[\"'][^>]*>",
        Pattern.CASE_INSENSITIVE
    );
    
    private static final Pattern DIRECTION_PATTERN = Pattern.compile(
        "\\bdir\\s*=\\s*[\"'](ltr|rtl)[\"']",
        Pattern.CASE_INSENSITIVE
    );
    
    // Common language detection patterns
    private static final Map<String, Pattern> LANGUAGE_PATTERNS = new HashMap<>();
    static {
        // Arabic script detection
        LANGUAGE_PATTERNS.put("ar", Pattern.compile("[\\u0600-\\u06FF\\u0750-\\u077F\\u08A0-\\u08FF\\uFB50-\\uFDFF\\uFE70-\\uFEFF]"));
        // Hebrew script detection  
        LANGUAGE_PATTERNS.put("he", Pattern.compile("[\\u0590-\\u05FF\\uFB1D-\\uFB4F]"));
        // Chinese characters
        LANGUAGE_PATTERNS.put("zh", Pattern.compile("[\\u4E00-\\u9FFF\\u3400-\\u4DBF]"));
        // Japanese characters
        LANGUAGE_PATTERNS.put("ja", Pattern.compile("[\\u3040-\\u309F\\u30A0-\\u30FF\\u4E00-\\u9FAF]"));
        // Korean characters
        LANGUAGE_PATTERNS.put("ko", Pattern.compile("[\\uAC00-\\uD7AF\\u1100-\\u11FF\\u3130-\\u318F]"));
        // Cyrillic script (Russian, etc.)
        LANGUAGE_PATTERNS.put("ru", Pattern.compile("[\\u0400-\\u04FF\\u0500-\\u052F]"));
        // Greek script
        LANGUAGE_PATTERNS.put("el", Pattern.compile("[\\u0370-\\u03FF\\u1F00-\\u1FFF]"));
    }
    
    private static final Set<String> RTL_LANGUAGES = new HashSet<>(Arrays.asList(
        "ar", "he", "fa", "ur", "yi", "ji", "iw", "arc", "bcc", "bqi", "ckb", "dv", "fa", "glk", "ku", "mzn", "pnb", "ps", "sd", "ug", "uz"
    ));

    @NotNull
    @Override
    public String getDisplayName() {
        return "Enhanced page language attributes and multilingual support";
    }

    @NotNull
    @Override
    public String getShortName() {
        return "EnhancedPageLanguage";
    }

    @Override
    protected void inspectFile(@NotNull PsiFile file, @NotNull ProblemsHolder holder) {
        String content = file.getText();
        
        // Skip lang attribute checks for Fluid Layout/Partial files
        if (isFluidLayoutOrPartial(file, content)) {
            return;
        }
        
        // Call parent implementation first
        super.inspectFile(file, holder);
        
        // Enhanced language validations
        analyzeMultilingualContent(content, file, holder);
        validateLanguageConsistency(content, file, holder);
        checkLanguageSwitchingInterface(content, file, holder);
        validateDirectionalitySupport(content, file, holder);
        checkSEOLanguageOptimization(content, file, holder);
        analyzeContentLanguageDetection(content, file, holder);
    }
    
    /**
     * Determines if this is a Fluid Layout or Partial file that shouldn't require lang attributes
     */
    private boolean isFluidLayoutOrPartial(PsiFile file, String content) {
        String filePath = file.getVirtualFile() != null ? file.getVirtualFile().getPath() : "";
        
        // Check if file is in Layouts/ or Partials/ directory
        if (filePath.contains("/Layouts/") || filePath.contains("/Partials/")) {
            return true;
        }
        
        // Check for Fluid-specific patterns that indicate this is a fragment, not a full HTML document
        // Look for f:render or f:section tags which are typical in Layouts/Partials
        if (content.contains("<f:render") || content.contains("<f:section")) {
            // Check if html tag exists and has xmlns:f attribute (namespace declaration only)
            Pattern htmlWithNamespace = Pattern.compile("<html[^>]*xmlns:f=[^>]*>", Pattern.CASE_INSENSITIVE);
            if (htmlWithNamespace.matcher(content).find()) {
                // If html tag only has Fluid namespace and no DOCTYPE, it's likely a Layout/Partial
                if (!content.contains("<!DOCTYPE") && !content.contains("<head>") && !content.contains("<body>")) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private void analyzeMultilingualContent(String content, PsiFile file, ProblemsHolder holder) {
        // Find all language declarations
        Map<String, List<LanguageDeclaration>> languageDeclarations = new HashMap<>();
        
        // Collect HTML lang attributes
        Matcher langMatcher = CONTENT_LANG_PATTERN.matcher(content);
        while (langMatcher.find()) {
            String langCode = langMatcher.group(1);
            String langContent = langMatcher.group(2);
            int offset = langMatcher.start();
            
            languageDeclarations.computeIfAbsent(langCode, k -> new ArrayList<>())
                .add(new LanguageDeclaration(langCode, langContent, offset, "content"));
        }
        
        // Collect meta language declarations
        Matcher metaMatcher = META_LANG_PATTERN.matcher(content);
        while (metaMatcher.find()) {
            String langCode = metaMatcher.group(1);
            int offset = metaMatcher.start();
            
            languageDeclarations.computeIfAbsent(langCode, k -> new ArrayList<>())
                .add(new LanguageDeclaration(langCode, "", offset, "meta"));
        }
        
        // Collect hreflang declarations
        Matcher hreflangMatcher = HREFLANG_PATTERN.matcher(content);
        while (hreflangMatcher.find()) {
            String langCode = hreflangMatcher.group(1);
            int offset = hreflangMatcher.start();
            
            languageDeclarations.computeIfAbsent(langCode, k -> new ArrayList<>())
                .add(new LanguageDeclaration(langCode, "", offset, "hreflang"));
        }
        
        // Analyze multilingual patterns
        if (languageDeclarations.size() > 1) {
            analyzeMultilingualImplementation(languageDeclarations, content, file, holder);
        }
        
        // Check for undeclared language content
        checkUndeclaredLanguageContent(content, languageDeclarations, file, holder);
    }
    
    private void validateLanguageConsistency(String content, PsiFile file, ProblemsHolder holder) {
        // Find the primary document language
        String primaryLang = extractPrimaryLanguage(content);
        
        if (primaryLang != null) {
            // Check meta tags consistency
            Matcher metaMatcher = META_LANG_PATTERN.matcher(content);
            while (metaMatcher.find()) {
                String metaLang = metaMatcher.group(1);
                
                if (!isLanguageEquivalent(primaryLang, metaLang)) {
                    registerProblem(holder, file, metaMatcher.start(), metaMatcher.end(),
                        String.format("Meta language '%s' doesn't match document language '%s'", metaLang, primaryLang),
                        ProblemHighlightType.WARNING,
                        new SynchronizeLanguageDeclarationsFix(primaryLang));
                }
            }
            
            // Check hreflang consistency
            Matcher hreflangMatcher = HREFLANG_PATTERN.matcher(content);
            boolean hasOwnHreflang = false;
            
            while (hreflangMatcher.find()) {
                String hreflangCode = hreflangMatcher.group(1);
                
                if (isLanguageEquivalent(primaryLang, hreflangCode)) {
                    hasOwnHreflang = true;
                }
            }
            
            // For multilingual sites, should include self-referencing hreflang
            if (hreflangMatcher.reset().find() && !hasOwnHreflang && !primaryLang.equals("x-default")) {
                registerProblem(holder, file, 0, 100,
                    String.format("Multilingual site should include hreflang='%s' for the current page", primaryLang),
                    ProblemHighlightType.WARNING,
                    new AddSelfReferencingHreflangFix(primaryLang));
            }
        }
    }
    
    private void checkLanguageSwitchingInterface(String content, PsiFile file, ProblemsHolder holder) {
        // Find language switching elements
        Matcher langSwitchMatcher = LANG_SWITCH_PATTERN.matcher(content);
        
        while (langSwitchMatcher.find()) {
            int switcherStart = langSwitchMatcher.start();
            int switcherEnd = findElementEnd(content, switcherStart);
            String switcherContent = content.substring(switcherStart, Math.min(switcherEnd, content.length()));
            
            // Check for accessibility features
            validateLanguageSwitcherAccessibility(switcherContent, file, holder, switcherStart);
            
            // Check for proper language indication
            validateLanguageSwitcherLanguageIndication(switcherContent, file, holder, switcherStart);
            
            // Check for current language indication
            boolean hasCurrentIndicator = switcherContent.contains("aria-current") ||
                                         switcherContent.contains("current") ||
                                         switcherContent.contains("active");
            
            if (!hasCurrentIndicator) {
                registerProblem(holder, file, switcherStart, switcherStart + 100,
                    "Language switcher should indicate the current language",
                    ProblemHighlightType.WARNING,
                    new AddCurrentLanguageIndicatorFix());
            }
        }
        
        // Check for missing language switcher in multilingual sites
        if (hasMultipleLanguages(content) && !langSwitchMatcher.reset().find()) {
            registerProblem(holder, file, 0, 100,
                "Multilingual site should provide a language switcher for user accessibility",
                ProblemHighlightType.WARNING,
                new AddLanguageSwitcherFix());
        }
    }
    
    private void validateDirectionalitySupport(String content, PsiFile file, ProblemsHolder holder) {
        // Find all language declarations and check for RTL language support
        Set<String> detectedLanguages = new HashSet<>();
        
        // Extract languages from lang attributes
        Matcher langMatcher = LANG_ATTR_PATTERN.matcher(content);
        while (langMatcher.find()) {
            String langCode = langMatcher.group(2);
            String baseLang = langCode.split("[-_]")[0].toLowerCase();
            detectedLanguages.add(baseLang);
        }
        
        // Check content for RTL script patterns
        for (Map.Entry<String, Pattern> entry : LANGUAGE_PATTERNS.entrySet()) {
            if (RTL_LANGUAGES.contains(entry.getKey()) && entry.getValue().matcher(content).find()) {
                detectedLanguages.add(entry.getKey());
            }
        }
        
        // Validate RTL language support
        for (String lang : detectedLanguages) {
            if (RTL_LANGUAGES.contains(lang)) {
                validateRTLLanguageSupport(lang, content, file, holder);
            }
        }
        
        // Check for mixed directionality content
        checkMixedDirectionalityContent(content, file, holder);
    }
    
    private void checkSEOLanguageOptimization(String content, PsiFile file, ProblemsHolder holder) {
        String primaryLang = extractPrimaryLanguage(content);
        
        if (primaryLang != null) {
            // Check for Open Graph locale
            boolean hasOGLocale = content.contains("property=\"og:locale\"");
            
            if (!hasOGLocale) {
                registerProblem(holder, file, 0, 100,
                    "Add Open Graph locale meta tag for better social media sharing",
                    ProblemHighlightType.INFORMATION,
                    new AddOpenGraphLocaleFix(primaryLang));
            }
            
            // Check for alternate language versions
            boolean hasHreflang = HREFLANG_PATTERN.matcher(content).find();
            boolean isMultilingual = hasMultipleLanguages(content);
            
            if (isMultilingual && !hasHreflang) {
                registerProblem(holder, file, 0, 100,
                    "Multilingual site should include hreflang links for SEO",
                    ProblemHighlightType.INFORMATION,
                    new AddHreflangLinksFix());
            }
            
            // Check for canonical URL with language parameter
            boolean hasCanonical = content.contains("rel=\"canonical\"");
            boolean hasLanguageInURL = content.contains("lang=") || content.contains("/en/") || 
                                      content.contains("/de/") || content.contains("/fr/");
            
            if (isMultilingual && hasLanguageInURL && !hasCanonical) {
                registerProblem(holder, file, 0, 100,
                    "Multilingual pages with language in URL should have canonical links",
                    ProblemHighlightType.INFORMATION,
                    new AddCanonicalLinkFix());
            }
        }
    }
    
    private void analyzeContentLanguageDetection(String content, PsiFile file, ProblemsHolder holder) {
        // Analyze text content to detect potential language mismatches
        String primaryLang = extractPrimaryLanguage(content);
        
        if (primaryLang != null) {
            // Extract meaningful text content (excluding HTML tags, scripts, styles)
            String textContent = extractTextContent(content);
            
            // Check for potential language mismatches
            Map<String, Integer> detectedLanguageScripts = new HashMap<>();
            
            for (Map.Entry<String, Pattern> entry : LANGUAGE_PATTERNS.entrySet()) {
                Matcher matcher = entry.getValue().matcher(textContent);
                int count = 0;
                while (matcher.find()) {
                    count++;
                }
                if (count > 0) {
                    detectedLanguageScripts.put(entry.getKey(), count);
                }
            }
            
            // Analyze detected scripts
            for (Map.Entry<String, Integer> entry : detectedLanguageScripts.entrySet()) {
                String detectedLang = entry.getKey();
                int occurrences = entry.getValue();
                
                if (!isLanguageEquivalent(primaryLang, detectedLang) && occurrences > 10) {
                    // Significant amount of content in different script
                    registerProblem(holder, file, 0, 100,
                        String.format("Content contains significant %s text but document language is '%s'. Consider adding lang attributes to specific sections",
                            getLanguageName(detectedLang), primaryLang),
                        ProblemHighlightType.INFORMATION,
                        new AddSectionLanguageAttributesFix(detectedLang));
                }
            }
        }
        
        // Check for untranslated interface text
        checkUntranslatedInterfaceText(content, primaryLang, file, holder);
    }
    
    // Helper methods
    private static class LanguageDeclaration {
        final String langCode;
        final String content;
        final int offset;
        final String type;
        
        LanguageDeclaration(String langCode, String content, int offset, String type) {
            this.langCode = langCode;
            this.content = content;
            this.offset = offset;
            this.type = type;
        }
    }
    
    private void analyzeMultilingualImplementation(Map<String, List<LanguageDeclaration>> languageDeclarations,
                                                  String content, PsiFile file, ProblemsHolder holder) {
        
        // Check for consistent multilingual implementation
        Set<String> languages = languageDeclarations.keySet();
        
        if (languages.size() > 2) {
            registerProblem(holder, file, 0, 100,
                String.format("Document contains %d languages. Ensure proper content organization and user interface for multilingual content",
                    languages.size()),
                ProblemHighlightType.INFORMATION,
                new OptimizeMultilingualStructureFix(languages.size()));
        }
        
        // Check for language-specific content sections
        for (String lang : languages) {
            List<LanguageDeclaration> declarations = languageDeclarations.get(lang);
            
            for (LanguageDeclaration declaration : declarations) {
                if ("content".equals(declaration.type) && declaration.content.length() > 100) {
                    // Check if this language section has appropriate structure
                    validateLanguageSectionStructure(declaration, content, file, holder);
                }
            }
        }
    }
    
    private void checkUndeclaredLanguageContent(String content, Map<String, List<LanguageDeclaration>> declared,
                                               PsiFile file, ProblemsHolder holder) {
        
        // Extract text content without declared language sections
        String undeclaredContent = content;
        
        for (List<LanguageDeclaration> declarations : declared.values()) {
            for (LanguageDeclaration declaration : declarations) {
                if ("content".equals(declaration.type)) {
                    // Remove declared content from analysis
                    undeclaredContent = undeclaredContent.replace(declaration.content, "");
                }
            }
        }
        
        String textContent = extractTextContent(undeclaredContent);
        
        // Check for significant non-English content without language declaration
        for (Map.Entry<String, Pattern> entry : LANGUAGE_PATTERNS.entrySet()) {
            String detectedLang = entry.getKey();
            Matcher matcher = entry.getValue().matcher(textContent);
            
            int count = 0;
            while (matcher.find()) {
                count++;
            }
            
            if (count > 5) { // Threshold for significant content
                registerProblem(holder, file, 0, 100,
                    String.format("Detected significant %s content without language declaration. Add lang='%s' to appropriate elements",
                        getLanguageName(detectedLang), detectedLang),
                    ProblemHighlightType.WARNING,
                    new AddMissingLanguageDeclarationFix(detectedLang));
            }
        }
    }
    
    private String extractPrimaryLanguage(String content) {
        Matcher htmlMatcher = HTML_TAG_PATTERN.matcher(content);
        if (htmlMatcher.find()) {
            String htmlTag = htmlMatcher.group();
            return getAttributeValue(htmlTag, "lang");
        }
        return null;
    }
    
    private boolean isLanguageEquivalent(String lang1, String lang2) {
        if (lang1 == null || lang2 == null) return false;
        
        // Compare base language codes (before hyphen/underscore)
        String base1 = lang1.split("[-_]")[0].toLowerCase();
        String base2 = lang2.split("[-_]")[0].toLowerCase();
        
        return base1.equals(base2);
    }
    
    private boolean hasMultipleLanguages(String content) {
        Set<String> languages = new HashSet<>();
        
        Matcher langMatcher = LANG_ATTR_PATTERN.matcher(content);
        while (langMatcher.find()) {
            String langCode = langMatcher.group(2);
            String baseLang = langCode.split("[-_]")[0].toLowerCase();
            languages.add(baseLang);
        }
        
        return languages.size() > 1;
    }
    
    private void validateLanguageSwitcherAccessibility(String switcherContent, PsiFile file,
                                                     ProblemsHolder holder, int offset) {
        
        // Check for proper labeling
        boolean hasAccessibleLabel = switcherContent.contains("aria-label") ||
                                    switcherContent.contains("aria-labelledby") ||
                                    switcherContent.contains("title");
        
        if (!hasAccessibleLabel) {
            registerProblem(holder, file, offset, offset + 100,
                "Language switcher should have accessible labeling for screen readers",
                ProblemHighlightType.WARNING,
                new AddLanguageSwitcherLabelFix());
        }
        
        // Check for keyboard accessibility
        boolean hasKeyboardAccess = switcherContent.contains("tabindex") ||
                                   switcherContent.contains("<button") ||
                                   switcherContent.contains("<a") ||
                                   switcherContent.contains("role=\"button\"");
        
        if (!hasKeyboardAccess) {
            registerProblem(holder, file, offset, offset + 100,
                "Language switcher should be keyboard accessible",
                ProblemHighlightType.WARNING,
                new MakeLanguageSwitcherKeyboardAccessibleFix());
        }
    }
    
    private void validateLanguageSwitcherLanguageIndication(String switcherContent, PsiFile file,
                                                          ProblemsHolder holder, int offset) {
        
        // Check if language options have their own lang attributes
        Matcher linkMatcher = Pattern.compile("<a[^>]*>.*?</a>", Pattern.CASE_INSENSITIVE | Pattern.DOTALL)
            .matcher(switcherContent);
        
        while (linkMatcher.find()) {
            String link = linkMatcher.group();
            
            if (!link.contains("lang=") && !link.contains("hreflang=")) {
                registerProblem(holder, file, offset, offset + 100,
                    "Language switcher links should specify their target language with lang or hreflang attributes",
                    ProblemHighlightType.WARNING,
                    new AddLanguageToSwitcherLinksFix());
                break; // Report once per switcher
            }
        }
    }
    
    private void validateRTLLanguageSupport(String language, String content, PsiFile file, ProblemsHolder holder) {
        // Check for dir attribute
        boolean hasDirectionAttribute = DIRECTION_PATTERN.matcher(content).find();
        
        if (!hasDirectionAttribute) {
            registerProblem(holder, file, 0, 100,
                String.format("Content includes RTL language (%s) but missing dir='rtl' attribute", language),
                ProblemHighlightType.ERROR,
                new AddDirectionalityAttributeFix("rtl"));
        }
        
        // Check for RTL CSS support indicators
        boolean hasRTLCSS = content.toLowerCase().contains("rtl") ||
                           content.contains("text-align: right") ||
                           content.contains("direction: rtl");
        
        if (!hasRTLCSS) {
            registerProblem(holder, file, 0, 100,
                String.format("RTL language (%s) content should include appropriate CSS for right-to-left layout", language),
                ProblemHighlightType.WARNING,
                new AddRTLCSSSupportFix(language));
        }
    }
    
    private void checkMixedDirectionalityContent(String content, PsiFile file, ProblemsHolder holder) {
        // Find elements with mixed LTR/RTL content
        Matcher langMatcher = CONTENT_LANG_PATTERN.matcher(content);
        
        while (langMatcher.find()) {
            String langCode = langMatcher.group(1);
            String langContent = langMatcher.group(2);
            String baseLang = langCode.split("[-_]")[0].toLowerCase();
            
            boolean isRTLLang = RTL_LANGUAGES.contains(baseLang);
            boolean hasRTLScript = false;
            boolean hasLTRScript = false;
            
            // Check for RTL scripts in content
            for (Map.Entry<String, Pattern> entry : LANGUAGE_PATTERNS.entrySet()) {
                if (RTL_LANGUAGES.contains(entry.getKey()) && entry.getValue().matcher(langContent).find()) {
                    hasRTLScript = true;
                }
                if (!RTL_LANGUAGES.contains(entry.getKey()) && entry.getValue().matcher(langContent).find()) {
                    hasLTRScript = true;
                }
            }
            
            if (hasRTLScript && hasLTRScript) {
                registerProblem(holder, file, langMatcher.start(), langMatcher.start() + 100,
                    "Content contains mixed RTL and LTR scripts. Consider using dir attribute for proper text direction",
                    ProblemHighlightType.WARNING,
                    new HandleMixedDirectionalityFix());
            }
        }
    }
    
    private String extractTextContent(String html) {
        // Remove script and style content
        String text = html.replaceAll("<script[^>]*>.*?</script>", "");
        text = text.replaceAll("<style[^>]*>.*?</style>", "");
        
        // Remove HTML tags
        text = text.replaceAll("<[^>]+>", " ");
        
        // Clean up whitespace
        text = text.replaceAll("\\s+", " ").trim();
        
        return text;
    }
    
    private void checkUntranslatedInterfaceText(String content, String primaryLang, PsiFile file, ProblemsHolder holder) {
        if (primaryLang == null || primaryLang.startsWith("en")) return;
        
        // Common English interface terms that might not be translated
        String[] englishTerms = {
            "home", "about", "contact", "login", "register", "search", "menu", "more",
            "click here", "read more", "learn more", "get started", "sign up", "log in",
            "privacy policy", "terms of service", "cookie policy"
        };
        
        String lowerContent = content.toLowerCase();
        
        for (String term : englishTerms) {
            if (lowerContent.contains(">" + term + "<") || lowerContent.contains(" " + term + " ")) {
                registerProblem(holder, file, 0, 100,
                    String.format("Document language is '%s' but contains English interface text ('%s'). Consider translating for consistency",
                        primaryLang, term),
                    ProblemHighlightType.INFORMATION,
                    new TranslateInterfaceTextFix(term, primaryLang));
                break; // Report once per file
            }
        }
    }
    
    private void validateLanguageSectionStructure(LanguageDeclaration declaration, String content,
                                                 PsiFile file, ProblemsHolder holder) {
        
        // Check if language section has proper heading structure
        String sectionContent = declaration.content;
        
        boolean hasHeading = sectionContent.contains("<h1") || sectionContent.contains("<h2") ||
                           sectionContent.contains("<h3") || sectionContent.contains("<h4") ||
                           sectionContent.contains("<h5") || sectionContent.contains("<h6");
        
        if (sectionContent.length() > 500 && !hasHeading) {
            registerProblem(holder, file, declaration.offset, declaration.offset + 100,
                String.format("Large content section in %s should include proper heading structure", 
                    getLanguageName(declaration.langCode)),
                ProblemHighlightType.INFORMATION,
                new AddLanguageSectionHeadingFix(declaration.langCode));
        }
        
        // Check for language-specific formatting needs
        if (RTL_LANGUAGES.contains(declaration.langCode.split("[-_]")[0].toLowerCase())) {
            boolean hasDirectionality = sectionContent.contains("dir=");
            
            if (!hasDirectionality) {
                registerProblem(holder, file, declaration.offset, declaration.offset + 100,
                    String.format("RTL language section (%s) should specify text direction with dir attribute",
                        declaration.langCode),
                    ProblemHighlightType.WARNING,
                    new AddSectionDirectionalityFix(declaration.langCode));
            }
        }
    }
    
    private String getLanguageName(String langCode) {
        Map<String, String> languageNames = new HashMap<>();
        languageNames.put("ar", "Arabic");
        languageNames.put("he", "Hebrew");
        languageNames.put("zh", "Chinese");
        languageNames.put("ja", "Japanese");
        languageNames.put("ko", "Korean");
        languageNames.put("ru", "Russian");
        languageNames.put("el", "Greek");
        languageNames.put("de", "German");
        languageNames.put("fr", "French");
        languageNames.put("es", "Spanish");
        languageNames.put("it", "Italian");
        
        return languageNames.getOrDefault(langCode, langCode.toUpperCase());
    }
    
    
    // Enhanced Quick Fixes
    private static class SynchronizeLanguageDeclarationsFix implements LocalQuickFix {
        private final String primaryLang;
        
        SynchronizeLanguageDeclarationsFix(String primaryLang) {
            this.primaryLang = primaryLang;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Synchronize language declarations with document language";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would synchronize language declarations
        }
    }
    
    private static class AddSelfReferencingHreflangFix implements LocalQuickFix {
        private final String language;
        
        AddSelfReferencingHreflangFix(String language) {
            this.language = language;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add self-referencing hreflang for " + language;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add self-referencing hreflang
        }
    }
    
    private static class AddCurrentLanguageIndicatorFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add current language indicator to switcher";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add current language indicator
        }
    }
    
    private static class AddLanguageSwitcherFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add language switcher interface";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add language switcher
        }
    }
    
    private static class AddLanguageSwitcherLabelFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add accessible label to language switcher";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add accessible label
        }
    }
    
    private static class MakeLanguageSwitcherKeyboardAccessibleFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Make language switcher keyboard accessible";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add keyboard accessibility
        }
    }
    
    private static class AddLanguageToSwitcherLinksFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add language attributes to switcher links";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add language attributes to links
        }
    }
    
    private static class AddDirectionalityAttributeFix implements LocalQuickFix {
        private final String direction;
        
        AddDirectionalityAttributeFix(String direction) {
            this.direction = direction;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add dir='" + direction + "' attribute";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add directionality attribute
        }
    }
    
    private static class AddRTLCSSSupportFix implements LocalQuickFix {
        private final String language;
        
        AddRTLCSSSupportFix(String language) {
            this.language = language;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add RTL CSS support for " + language;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add RTL CSS support
        }
    }
    
    private static class HandleMixedDirectionalityFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Handle mixed directional content with proper dir attributes";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would handle mixed directionality
        }
    }
    
    private static class AddOpenGraphLocaleFix implements LocalQuickFix {
        private final String language;
        
        AddOpenGraphLocaleFix(String language) {
            this.language = language;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add Open Graph locale meta tag";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add OG locale
        }
    }
    
    private static class AddHreflangLinksFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add hreflang links for multilingual SEO";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add hreflang links
        }
    }
    
    private static class AddCanonicalLinkFix implements LocalQuickFix {
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add canonical link for multilingual page";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add canonical link
        }
    }
    
    private static class AddSectionLanguageAttributesFix implements LocalQuickFix {
        private final String detectedLanguage;
        
        AddSectionLanguageAttributesFix(String detectedLanguage) {
            this.detectedLanguage = detectedLanguage;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add lang='" + detectedLanguage + "' to relevant sections";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add section language attributes
        }
    }
    
    private static class OptimizeMultilingualStructureFix implements LocalQuickFix {
        private final int languageCount;
        
        OptimizeMultilingualStructureFix(int languageCount) {
            this.languageCount = languageCount;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Optimize structure for " + languageCount + " languages";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would optimize multilingual structure
        }
    }
    
    private static class AddMissingLanguageDeclarationFix implements LocalQuickFix {
        private final String language;
        
        AddMissingLanguageDeclarationFix(String language) {
            this.language = language;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add lang='" + language + "' declaration to undeclared content";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add missing language declaration
        }
    }
    
    private static class TranslateInterfaceTextFix implements LocalQuickFix {
        private final String term;
        private final String targetLanguage;
        
        TranslateInterfaceTextFix(String term, String targetLanguage) {
            this.term = term;
            this.targetLanguage = targetLanguage;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Consider translating '" + term + "' to " + targetLanguage;
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would suggest translation
        }
    }
    
    private static class AddLanguageSectionHeadingFix implements LocalQuickFix {
        private final String language;
        
        AddLanguageSectionHeadingFix(String language) {
            this.language = language;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add heading structure to " + language + " content section";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add heading structure
        }
    }
    
    private static class AddSectionDirectionalityFix implements LocalQuickFix {
        private final String language;
        
        AddSectionDirectionalityFix(String language) {
            this.language = language;
        }
        
        @NotNull
        @Override
        public String getFamilyName() {
            return "Add dir attribute to " + language + " section";
        }
        
        @Override
        public void applyFix(@NotNull Project project, @NotNull ProblemDescriptor descriptor) {
            // Implementation would add section directionality
        }
    }
}