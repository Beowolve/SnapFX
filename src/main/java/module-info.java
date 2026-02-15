/**
 * SnapFX - A lightweight JavaFX docking framework
 * This module provides a complete docking system with drag & drop support,
 * layout persistence, and a clean separation between model and view.
 */
module com.github.beowolve.snapfx {
    // JavaFX dependencies
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires java.desktop;

    // JSON serialization
    requires com.google.gson;

    // Export public API packages
    exports com.github.beowolve.snapfx;
    exports com.github.beowolve.snapfx.close;
    exports com.github.beowolve.snapfx.model;
    exports com.github.beowolve.snapfx.view;
    exports com.github.beowolve.snapfx.floating;
    exports com.github.beowolve.snapfx.dnd;
    exports com.github.beowolve.snapfx.persistence;
    exports com.github.beowolve.snapfx.debug;

    // Demo package is exported for running the demo application
    exports com.github.beowolve.snapfx.demo;

    // Open packages for reflection (needed by Gson for serialization)
    opens com.github.beowolve.snapfx.model to com.google.gson;
    opens com.github.beowolve.snapfx.persistence to com.google.gson;
}

