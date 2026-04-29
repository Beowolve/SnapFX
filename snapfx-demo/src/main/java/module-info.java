/**
 * SnapFX demo applications and demo-only helpers.
 */
module org.snapfx.demo {
    requires javafx.controls;
    requires javafx.graphics;
    requires javafx.base;
    requires java.desktop;
    requires com.google.gson;
    requires atlantafx.base;

    requires org.snapfx;

    exports org.snapfx.demo;
    opens org.snapfx.demo.i18n to org.snapfx;
}
