//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains information on a Swiftly Project
 */
public class SwiftlyProject
    implements IsSerializable
{
    /** The id of the project. */
    public int projectId;
    
    /** The id of the project owner. */
    public int ownerId;

    /** The project type id. */
    public int projectTypeId;

    /** The project name. */
    public String projectName;

}
