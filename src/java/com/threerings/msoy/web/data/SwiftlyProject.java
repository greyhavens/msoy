//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.web.Item;

/**
 * Contains information on a Swiftly Project
 */
public class SwiftlyProject
    implements IsSerializable
{
    /** The valid project types. */
    public static byte[] PROJECT_TYPES = { Item.GAME, Item.AVATAR };

    /** Returns true if the given project type is supported by Swiftly. */
    public static boolean isValidProjectType (byte projectType)
    {
        for (int i = 0; i < PROJECT_TYPES.length; i++) {
            if (PROJECT_TYPES[i] == projectType) {
                return true;
            }
        }

        return false;
    }

    /** The id of the project. */
    public int projectId;

    /** The id of the project owner. */
    public int ownerId;

    /** The project type. */
    public int projectType;

    /** The project name. */
    public String projectName;

    /** Whether the project is remixable. */
    public boolean remixable;

    /** Returns the source file name for the project's type. */
    public String getTemplateSourceName () {
        // We can't use a switch statement because the type finals are not actually constants
        if (projectType == Item.GAME) {
            return "Game.as";
        } else if (projectType == Item.AVATAR) {
            return "Avatar.as";
        } else {
            return null;
        }
    }
}
