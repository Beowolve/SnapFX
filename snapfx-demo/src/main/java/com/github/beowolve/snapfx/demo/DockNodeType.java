package com.github.beowolve.snapfx.demo;

/**
 * Enum defining all available DockNode types in the demo application.
 * Each type has a unique ID, icon name, and display name.
 * This centralizes the management of all node properties.
 */
public enum DockNodeType {

    // === Main layout nodes ===
    PROJECT_EXPLORER("projectExplorer", "folder.png", "Project Explorer", true),
    MAIN_EDITOR("mainEditor", "document--pencil.png", "Main.java", true),
    PROPERTIES("properties", "property.png", "Properties", true),
    CONSOLE("console", "terminal.png", "Console", true),
    TASKS("tasks", "clipboard-list.png", "Tasks", true),

    // === Dynamically addable nodes ===
    EDITOR("editor", "document--pencil.png", "Editor", false),
    PROPERTIES_PANEL("propertiesPanel", "property.png", "Properties", false),
    CONSOLE_PANEL("consolePanel", "terminal.png", "Console", false),
    GENERIC_PANEL("genericPanel", "plus.png", "Panel", false);

    private final String id;
    private final String iconName;
    private final String defaultTitle;
    private final boolean singleton;

    /**
     * Constructs a new DockNodeType with the given properties.
     *
     * @param id           unique ID for this node type
     * @param iconName     filename of the icon for this node type
     * @param defaultTitle default display title for this node type
     * @param singleton    whether this node type is a singleton (only one instance allowed)
     */
    DockNodeType(String id, String iconName, String defaultTitle, boolean singleton) {
        this.id = id;
        this.iconName = iconName;
        this.defaultTitle = defaultTitle;
        this.singleton = singleton;
    }

    /**
     * Returns the unique ID for this node type.
     */
    public String getId() {
        return id;
    }

    /**
     * Returns the icon filename for this node type.
     */
    public String getIconName() {
        return iconName;
    }

    /**
     * Returns the default display title for this node type.
     */
    public String getDefaultTitle() {
        return defaultTitle;
    }

    /**
     * Returns whether this node type is a singleton.
     */
    public boolean isSingleton() {
        return singleton;
    }

    /**
     * Finds a DockNodeType by its ID.
     *
     * @param id The ID to search for
     * @return The matching DockNodeType, or null if not found
     */
    public static DockNodeType fromId(String id) {
        for (DockNodeType type : values()) {
            if (type.id.equals(id)) {
                return type;
            }
        }
        return null;
    }
}
