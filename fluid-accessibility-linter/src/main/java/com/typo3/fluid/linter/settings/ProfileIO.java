package com.typo3.fluid.linter.settings;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.intellij.openapi.project.Project;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;

/**
 * Import/export of rule profiles as JSON using Gson (bundled with IDE).
 */
public final class ProfileIO {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private ProfileIO() {}

    public static void exportProfile(Project project, File file) throws Exception {
        RuleSettingsState state = RuleSettingsState.getInstance(project);
        try (BufferedWriter w = new BufferedWriter(new FileWriter(file, StandardCharsets.UTF_8))) {
            w.write(GSON.toJson(state.getState()));
        }
    }

    public static void importProfile(Project project, File file) throws Exception {
        RuleSettingsState stateSvc = RuleSettingsState.getInstance(project);
        try (BufferedReader r = new BufferedReader(new FileReader(file, StandardCharsets.UTF_8))) {
            RuleSettingsState.State in = GSON.fromJson(r, RuleSettingsState.State.class);
            if (in != null) {
                stateSvc.loadState(in);
            }
        }
    }
}

