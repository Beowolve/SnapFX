/**
 * SnapFX - A lightweight JavaFX docking framework
 * This module provides a complete docking system with drag &amp; drop support,
 * layout persistence, and a clean separation between model and view.
 */
module org.snapfx {
    // JavaFX dependencies
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires java.desktop;

    // JSON serialization
    requires com.google.gson;

    // Export public API packages
    exports org.snapfx;
    exports org.snapfx.close;
    exports org.snapfx.model;
    exports org.snapfx.view;
    exports org.snapfx.floating;
    exports org.snapfx.dnd;
    exports org.snapfx.persistence;
    exports org.snapfx.debug;
    exports org.snapfx.sidebar;

    // Open packages for reflection (needed by Gson for serialization)
    opens org.snapfx.model to com.google.gson;
    opens org.snapfx.persistence to com.google.gson;
}

