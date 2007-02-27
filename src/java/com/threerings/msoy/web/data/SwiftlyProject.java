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
    public static byte[] PROJECT_TYPES = { Item.GAME };

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
}
