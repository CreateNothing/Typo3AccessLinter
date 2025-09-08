package com.typo3.fluid.linter.context;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class FluidContextManagerTest {

    @Test
    public void parseTypoScript_lineAndBlockForms_preserveOrderAndKinds() {
        String ts = "" +
                "templateRootPaths.10 = path/site/Templates\n" +
                "layoutRootPaths.20 = path/site/Layouts\n" +
                "partialRootPaths.30 = path/site/Partials\n\n" +
                "templateRootPaths {\n 40 = path/vendor/ext/Templates\n 50 = path/fallback/Templates\n}\n" +
                "layoutRootPaths {\n 60 = path/vendor/ext/Layouts\n}\n" +
                "partialRootPaths {\n 70 = path/vendor/ext/Partials\n 80 = path/fallback/Partials\n}\n";

        RootPathSet set = FluidContextManager.parseTypoScript(ts);
        assertEquals(List.of("path/site/Templates", "path/vendor/ext/Templates", "path/fallback/Templates"), set.templates());
        assertEquals(List.of("path/site/Layouts", "path/vendor/ext/Layouts"), set.layouts());
        assertEquals(List.of("path/site/Partials", "path/vendor/ext/Partials", "path/fallback/Partials"), set.partials());
    }

    @Test
    public void parseYaml_mappingAndInlineList_supported() {
        String yaml = "" +
                "templateRootPaths:\n" +
                "  10: path/site/Templates\n" +
                "  20: path/vendor/Templates\n" +
                "layoutRootPaths: [path/site/Layouts, path/vendor/Layouts]\n" +
                "partialRootPaths:\n" +
                "  5: path/first/Partials\n" +
                "  15: path/second/Partials\n";

        RootPathSet set = FluidContextManager.parseYaml(yaml);
        assertEquals(List.of("path/site/Templates", "path/vendor/Templates"), set.templates());
        assertEquals(List.of("path/site/Layouts", "path/vendor/Layouts"), set.layouts());
        assertEquals(List.of("path/first/Partials", "path/second/Partials"), set.partials());
    }
}

