package com.typo3.fluid.linter.ui.fluida11y.vm;

import org.jetbrains.annotations.NotNull;

/**
 * Bundles the individual tab view models into a single immutable snapshot
 * to reduce UI churn and keep updates atomic.
 */
public final class AggregatedSnapshot {
    public final @NotNull OutlineVM outline;
    public final @NotNull OverridesVM overrides;
    public final @NotNull DiagnosticsVM diagnostics;

    public AggregatedSnapshot(@NotNull OutlineVM outline, @NotNull OverridesVM overrides, @NotNull DiagnosticsVM diagnostics) {
        this.outline = outline;
        this.overrides = overrides;
        this.diagnostics = diagnostics;
    }
}

