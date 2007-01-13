//
// $Id$

package com.threerings.msoy.swiftly.client;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.swiftly.util.SwiftlyContext;

/**
 * Wires up all the necessary bits when we enter our project room.
 */
public class ProjectRoomController extends PlaceController
{
    @Override // from PlaceController
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return new SwiftlyEditor((SwiftlyContext)ctx);
    }
}
