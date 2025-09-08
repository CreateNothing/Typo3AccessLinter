package com.typo3.fluid.linter.context;

import com.intellij.util.messages.Topic;
import org.jetbrains.annotations.NotNull;

public interface ContextChangeTopic {
    Topic<ContextChangeTopic> TOPIC = Topic.create("Fluid Context Changes", ContextChangeTopic.class);

    void contextsChanged(@NotNull ContextDiff diff);
}

