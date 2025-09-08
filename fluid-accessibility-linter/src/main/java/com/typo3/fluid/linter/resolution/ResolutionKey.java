package com.typo3.fluid.linter.resolution;

import com.typo3.fluid.linter.catalog.ImplementationKind;
import com.typo3.fluid.linter.context.ContextId;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ResolutionKey {
    public final @NotNull ContextId ctx;
    public final @NotNull ImplementationKind kind;
    public final @NotNull String logicalName;

    ResolutionKey(@NotNull ContextId ctx, @NotNull ImplementationKind kind, @NotNull String logicalName) {
        this.ctx = ctx; this.kind = kind; this.logicalName = logicalName;
    }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResolutionKey)) return false;
        ResolutionKey that = (ResolutionKey) o;
        return ctx.equals(that.ctx) && kind == that.kind && logicalName.equals(that.logicalName);
    }

    @Override public int hashCode() { return Objects.hash(ctx, kind, logicalName); }
}
