package com.typo3.fluid.linter.context;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

public final class RootPathSet {
    private final List<String> templates;
    private final List<String> layouts;
    private final List<String> partials;

    public RootPathSet(@NotNull List<String> templates, @NotNull List<String> layouts, @NotNull List<String> partials) {
        this.templates = Collections.unmodifiableList(new ArrayList<>(templates));
        this.layouts = Collections.unmodifiableList(new ArrayList<>(layouts));
        this.partials = Collections.unmodifiableList(new ArrayList<>(partials));
    }

    public @NotNull List<String> templates() { return templates; }
    public @NotNull List<String> layouts() { return layouts; }
    public @NotNull List<String> partials() { return partials; }

    @Override public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RootPathSet)) return false;
        RootPathSet that = (RootPathSet) o;
        return templates.equals(that.templates) && layouts.equals(that.layouts) && partials.equals(that.partials);
    }

    @Override public int hashCode() { return Objects.hash(templates, layouts, partials); }

    @Override public String toString() {
        return "RootPathSet{" +
                "templates=" + templates +
                ", layouts=" + layouts +
                ", partials=" + partials +
                '}';
    }
}

