//
// $Id$

package com.threerings.msoy.swiftly.server.persist;

import com.samskivert.jdbc.depot.annotation.*; // for Depot annotations

/**
 * Contains the configuration of a particular member's person page.
 */
@Entity public class SwiftlyProjectRecord
{
    /** The id of the project. */
    @Id public int projectId;
    
    /** The id of the project owner. */
    // @NotNull
    public int ownerId;

    /** The project name. */
    // @NotNull
    public String projectName;

    /** A brief project description. */
    // @NotNull
    public String projectDescription;

    /** Project subversion URL. */
    // @NotNull
    public String projectSVNURL;
}
