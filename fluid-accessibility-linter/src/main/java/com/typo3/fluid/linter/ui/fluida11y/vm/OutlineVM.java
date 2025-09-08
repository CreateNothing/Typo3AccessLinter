package com.typo3.fluid.linter.ui.fluida11y.vm;

import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Immutable view model for the Outline tab.
 * This is intentionally minimal for the initial scaffold.
 */
public final class OutlineVM {
    public final boolean indexing;
    public final boolean unsavedPreview;
    public final long lastUpdatedMillis;
    public final @NotNull List<HeadingItem> headings;

    public OutlineVM(boolean indexing, boolean unsavedPreview, long lastUpdatedMillis, @NotNull List<HeadingItem> headings) {
        this.indexing = indexing;
        this.unsavedPreview = unsavedPreview;
        this.lastUpdatedMillis = lastUpdatedMillis;
        this.headings = Collections.unmodifiableList(headings);
    }

    public static final class HeadingItem {
        public final int level; public final String text; public final int startOffset;
        public HeadingItem(int level, String text, int startOffset) {
            this.level = level; this.text = text; this.startOffset = startOffset;
        }
    }
}

