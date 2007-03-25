//
// $Id$

package com.threerings.msoy.swiftly.client;

import java.awt.event.ActionEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;

/**
 * Wires up all the necessary bits when we enter our project room.
 */
public class ProjectRoomController extends PlaceController
{
    /** An action that requests to build the project. */
    public Action buildAction;

    @Override // from PlaceController
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        // we need to create our actions before we call super because that will call
        // createPlaceView which will create the whole UI which will want to wire up our actions
        _ctx = (SwiftlyContext)ctx;

        buildAction = new AbstractAction(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, "m.action.build")) {
            public void actionPerformed (ActionEvent e) {
                buildProject();
            }
        };

        super.init(ctx, config);
    }

    @Override // from PlaceController
    public void willEnterPlace (PlaceObject plobj)
    {
        super.willEnterPlace(plobj);
        _roomObj = (ProjectRoomObject)plobj;
    }

    @Override // from PlaceController
    protected PlaceView createPlaceView (CrowdContext ctx)
    {
        return (_editor = new SwiftlyEditor(this, (SwiftlyContext)ctx));
    }

    /**
     * Triggered by {@link #buildAction}.
     */
    protected void buildProject ()
    {
        // report to the user that we're starting the build
        _editor.buildStarted();
        // disable the action on this client
        buildAction.setEnabled(false);
        // the results of this request will be communicated via _roomObj.console
        _roomObj.service.buildProject(_ctx.getClient());
    }

    protected SwiftlyContext _ctx;
    protected ProjectRoomObject _roomObj;
    protected SwiftlyEditor _editor;
}
