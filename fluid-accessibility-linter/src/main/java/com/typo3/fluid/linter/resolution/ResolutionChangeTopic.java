package com.typo3.fluid.linter.resolution;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface ResolutionChangeTopic {
    Topic<ResolutionChangeTopic> TOPIC = Topic.create("Fluid Effective Resolution Changes", ResolutionChangeTopic.class);

    void resolutionsChanged(@NotNull ResolutionDiff diff);
}

