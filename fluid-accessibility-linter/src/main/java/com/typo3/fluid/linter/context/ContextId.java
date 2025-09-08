package com.typo3.fluid.linter.context;

import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public final class ContextId {
    private final String id;

    public static final ContextId DEFAULT = new ContextId("DEFAULT");

    public ContextId(@NotNull String id) { this.id = id; }
    public @NotNull String id() { return id; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ContextId)) return false;
        ContextId that = (ContextId) o;
        return id.equals(that.id);
    }

    @Override public int hashCode() { return Objects.hash(id); }
    @Override public String toString() { return id; }
}

