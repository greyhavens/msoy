//
// $Id$

package com.threerings.msoy.swiftly.data;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.swiftly.client.controller.ProjectRoomController;

/**
 * Defines the bits needed for the project room.
 */
public class ProjectRoomConfig extends PlaceConfig
{
    /** Identifier for the project whose room we're entering. */
    public int projectId;

    @Override // from PlaceConfig
    public PlaceController createController ()
    {
        return new ProjectRoomController();
    }

    // from PlaceConfig
    public String getManagerClassName ()
    {
        return "com.threerings.msoy.swiftly.server.ProjectRoomManager";
    }
}
