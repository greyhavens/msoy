//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import javax.swing.ImageIcon;

/**
 * Store meta data associated with an EditorAction.
 */
public enum ActionResource
{
    ADD_FILE ("m.action.add_file", "m.tooltip.add_file", "new.gif"),
    UPLOAD_FILE ("m.action.upload_file", "m.tooltip.upload_file", "upload.gif"),
    DELETE_FILE ("m.action.delete_file", "m.tooltip.delete_file", "delete.gif"),
    RENAME_FILE ("m.action.rename_file", "m.tooltip.rename_file", "rename.gif"),
    SHOW_CONSOLE ("m.action.show_console", "m.tooltip.show_console", "console.png"),
    CLOSE_CURRENT_TAB ("m.action.close_tab", "m.tooltip.close_tab", "close.png"),
    BUILD ("m.action.build", "m.tooltip.build", "build.png"),
    BUILD_EXPORT ("m.action.build_export", "m.tooltip.build_export", "build_export.png");

    public static final String BASE_PATH = "/rsrc/icons/swiftly/";

    public final String name;
    public final String description;
    public final ImageIcon icon;

    ActionResource (String name, String description, String iconPath)
    {
        this.name = name;
        this.description = description;
        this.icon = new ImageIcon(getClass().getResource(BASE_PATH + iconPath));
    }
}
