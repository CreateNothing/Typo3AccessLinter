package com.typo3.fluid.linter.ui.fluida11y;

import com.intellij.util.messages.Topic;

public interface UiStateTopic {
    Topic<UiStateTopic> TOPIC = Topic.create("Fluid A11y UI State", UiStateTopic.class);
    void stateChanged(A11yUiState state);
}

