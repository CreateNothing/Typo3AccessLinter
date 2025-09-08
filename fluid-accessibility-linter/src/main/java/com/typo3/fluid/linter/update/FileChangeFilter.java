package com.typo3.fluid.linter.update;

import com.intellij.openapi.vfs.VirtualFile;
import org.jetbrains.annotations.NotNull;

public final class FileChangeFilter {

    public enum Reason { FLUID, TYPOSCRIPT, SITE_SETTINGS, OTHER }

    private FileChangeFilter() {}

    public static @NotNull Reason classify(@NotNull VirtualFile file) {
        final String name = file.getName();
        final String path = file.getPath().toLowerCase();
        final String ext = extensionOf(name);

        if ("html".equals(ext)) {
            return Reason.FLUID; // Fluid templates/partials/layouts are HTML files
        }

        // TypoScript: prefer explicit .typoscript; allow .ts only when path hints TypoScript
        if ("typoscript".equals(ext) || ("ts".equals(ext) && looksLikeTypoScriptPath(path))) {
            return Reason.TYPOSCRIPT;
        }

        // Site configuration YAML under config/sites/**.yml/.yaml
        if ((name.endsWith(".yml") || name.endsWith(".yaml")) && path.contains("/config/sites/")) {
            return Reason.SITE_SETTINGS;
        }

        return Reason.OTHER;
    }

    public static boolean isRelevant(@NotNull VirtualFile file) {
        Reason r = classify(file);
        return r == Reason.FLUID || r == Reason.TYPOSCRIPT || r == Reason.SITE_SETTINGS;
    }

    private static boolean looksLikeTypoScriptPath(String lowercasePath) {
        // Common patterns in TYPO3 projects to avoid clashing with TypeScript assets
        return lowercasePath.contains("/typoscript/") ||
               lowercasePath.contains("/config/typoscript/") ||
               lowercasePath.contains("/configuration/typoscript/") ||
               lowercasePath.contains("typoscript");
    }

    public static boolean isRelevantNameInParent(@NotNull String parentPath, @NotNull String name) {
        String ext = extensionOf(name);
        String path = (parentPath + "/" + name).toLowerCase();
        if ("html".equals(ext)) return true;
        if ("typoscript".equals(ext)) return true;
        if ("ts".equals(ext) && looksLikeTypoScriptPath(path)) return true;
        if ((name.endsWith(".yml") || name.endsWith(".yaml")) && path.contains("/config/sites/")) return true;
        return false;
    }

    private static String extensionOf(@NotNull String name) {
        int dot = name.lastIndexOf('.');
        return dot >= 0 ? name.substring(dot + 1).toLowerCase() : "";
    }
}
