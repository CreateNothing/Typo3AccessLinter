package com.typo3.fluid.linter.ui.fluida11y.vm;

import com.typo3.fluid.linter.catalog.ImplementationKind;
import com.typo3.fluid.linter.context.ContextId;
import org.jetbrains.annotations.NotNull;

import java.util.Collections;
import java.util.List;

/**
 * Immutable view model for the Parents & Overrides tab.
 */
public final class OverridesVM {
    public final boolean indexing;
    public final long lastUpdatedMillis;
    public final @NotNull List<Row> rows;

    public OverridesVM(boolean indexing, long lastUpdatedMillis, @NotNull List<Row> rows) {
        this.indexing = indexing;
        this.lastUpdatedMillis = lastUpdatedMillis;
        this.rows = Collections.unmodifiableList(rows);
    }

    public static final class Row {
        public final ContextId ctx; public final ImplementationKind kind; public final String logicalName;
        public final String badge; public final String effectivePath;
        public Row(ContextId ctx, ImplementationKind kind, String logicalName, String badge, String effectivePath) {
            this.ctx = ctx; this.kind = kind; this.logicalName = logicalName; this.badge = badge; this.effectivePath = effectivePath;
        }
    }
}

