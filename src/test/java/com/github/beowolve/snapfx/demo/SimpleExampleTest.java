package com.github.beowolve.snapfx.demo;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class SimpleExampleTest {
    @Test
    void testStylesheetResourcePathMatchesExpectedLocation() {
        assertEquals("/snapfx.css", SimpleExample.getStylesheetResourcePath());
    }

    @Test
    void testStylesheetResourceExistsInClasspath() {
        assertNotNull(
            SimpleExample.resolveStylesheetResource(),
            "SimpleExample stylesheet resource should be present in classpath"
        );
    }
}
