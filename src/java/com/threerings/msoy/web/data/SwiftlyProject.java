//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.all.Item;

/**
 * Contains information on a Swiftly Project
 */
public class SwiftlyProject
    implements IsSerializable, Streamable
{
    public SwiftlyProject ()
    {
    }

    /** The valid project types. */
    public static byte[] PROJECT_TYPES = { Item.GAME, Item.AVATAR, Item.FURNITURE, Item.PET };

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

    /**
     * A version of clone() that will work in GWT land and Java land.
     */
    public SwiftlyProject klone ()
    {
        SwiftlyProject project = new SwiftlyProject();
        project.projectId = this.projectId;
        project.ownerId = this.ownerId;
        project.projectType = this.projectType;
        project.remixable = this.remixable;
        return project;
    }

    /**
     * Returns the source file name for the project's type.
     * TODO: Store this in the database upon project creation. Until that's
     * implemented changing these will break existing projects, so don't.
     */
    public String getTemplateSourceName () {
        // We can't use a switch statement because the type finals are not actually constants
        if (projectType == Item.GAME) {
            return "SwiftlyGame.as";
        } else if (projectType == Item.AVATAR) {
            return "SwiftlyAvatar.as";
        } else if (projectType == Item.FURNITURE) {
            return "SwiftlyFurni.as";
        } else if (projectType == Item.PET) {
            return "SwiftlyPet.as";
        } else {
            return null;
        }
    }

    /**
     * Returns the output file name for the project's type.
     * TODO: Store this in the database upon project creation. Until that's
     * implemented changing these will break existing projects, so don't.
     */
    public String getOutputFileName ()
    {
        // We can't use a switch statement because the type finals are not actually constants
        if (projectType == Item.GAME) {
            return "SwiftlyGame.swf";
        } else if (projectType == Item.AVATAR) {
            return "SwiftlyAvatar.swf";
        } else if (projectType == Item.FURNITURE) {
            return "SwiftlyFurni.swf";
        } else if (projectType == Item.PET) {
            return "SwiftlyPet.swf";
        } else {
            return null;
        }
    }
}
