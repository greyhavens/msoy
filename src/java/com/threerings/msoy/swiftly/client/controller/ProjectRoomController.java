//
// $Id$

package com.threerings.msoy.swiftly.client.controller;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;
import com.threerings.msoy.swiftly.client.SwiftlyContext;
import com.threerings.msoy.swiftly.client.model.DocumentModel;
import com.threerings.msoy.swiftly.client.model.ProjectModel;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;

/**
 * Wires up all the necessary bits when we enter our project room.
 */
public class ProjectRoomController extends PlaceController
{
    @Override // from PlaceController
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        super.init(ctx, config);
        _ctx = (SwiftlyContext)ctx;
    }

    @Override // from PlaceController
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);

        ProjectRoomObject roomObj = (ProjectRoomObject)plobj;

        // setup our models
        ProjectModel projModel = new ProjectModel(roomObj, _ctx);
        DocumentModel docModel = new DocumentModel(roomObj, _ctx);

        // create the controllers and views
        new ProjectController(projModel, docModel, _ctx);
    }

    private SwiftlyContext _ctx;
}
