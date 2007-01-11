//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.util.List;
import java.util.Map;

/**
 * Project Management XML-RPC Methods
 */
public interface SwiftlyProjectRpc
{
    /**
     * Provides a list of the user's projects.
     */
    public List<Map<String,Object>> getProjects (String authtoken);

    /**
     * Create a project for the user.
     */
    public boolean createProject (String authtoken, String projectName);

    // Java implements XML-RPC structs as hashmaps, which is a bummer. So, we provide keys into those dictionaries.
    /** Project struct, name string element. */
    public static final String PROJECT_NAME = "PROJECT_NAME";

    /** Project struct, id integer element */
    public static final String PROJECT_ID = "PROJECT_ID";
}
