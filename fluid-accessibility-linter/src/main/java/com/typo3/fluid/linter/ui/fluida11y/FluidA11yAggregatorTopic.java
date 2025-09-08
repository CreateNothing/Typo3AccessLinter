package com.typo3.fluid.linter.ui.fluida11y;

import com.intellij.util.messages.Topic;
import com.typo3.fluid.linter.ui.fluida11y.vm.AggregatedSnapshot;
import org.jetbrains.annotations.NotNull;

/**
 * Message bus topic for unified Fluid A11y tool window updates.
 */
public interface FluidA11yAggregatorTopic {
    Topic<FluidA11yAggregatorTopic> TOPIC = Topic.create("Fluid A11y Aggregated Updates", FluidA11yAggregatorTopic.class);
    void updated(@NotNull AggregatedSnapshot snapshot);
}

