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

import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.util.MessageBundle;

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

    /**
     *  An action that requests to build the project and export the results to the users
     *  Whirled inventory.
     */
    public Action buildExportAction;

    @Override // from PlaceController
    public void init (CrowdContext ctx, PlaceConfig config)
    {
        // we need to create our actions before we call super because that will call
        // createPlaceView which will create the whole UI which will want to wire up our actions
        _ctx = (SwiftlyContext)ctx;
        _msgs = _ctx.getMessageManager().getBundle(SwiftlyCodes.SWIFTLY_MSGS);

        buildAction = new AbstractAction(_msgs.get("m.action.build")) {
            public void actionPerformed (ActionEvent e) {
                buildProject();
            }
        };

        buildExportAction = new AbstractAction(_msgs.get("m.action.build_export")) {
            public void actionPerformed (ActionEvent e) {
                buildAndExport();
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
        return new SwiftlyEditor(this, (SwiftlyContext)ctx);
    }

    /**
     * Triggered by {@link #buildAction}.
     */
    protected void buildProject ()
    {
        disableBuild();
        _roomObj.service.buildProject(_ctx.getClient(), new ConfirmListener () {
            public void requestProcessed ()
            {
                // Success will be handled with the room object result field.
            }
            public void requestFailed (String reason) {
                _ctx.showErrorMessage(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, reason));
            }
        });

    }

    /**
     * Triggered by {@link #buildExportAction}.
     */
    protected void buildAndExport ()
    {
       disableBuild();
        _roomObj.service.buildAndExportProject(_ctx.getClient(), new ConfirmListener() {
            public void requestProcessed () {
                // inform just this user that the build result was exported
                _ctx.showInfoMessage(_msgs.get("m.build_export_succeeded"));
            }
            public void requestFailed (String reason) {
                _ctx.showErrorMessage(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, reason));
            }
        });
    }

    /**
     * Disable the build actions while a build is happening. Enabling will be handled by watching
     * the _roomObj.building field in SwiftlyEditor.
     */
    protected void disableBuild ()
    {
        // disable the action on this client
        buildAction.setEnabled(false);
        buildExportAction.setEnabled(false);
    }

    protected SwiftlyContext _ctx;
    protected MessageBundle _msgs;
    protected ProjectRoomObject _roomObj;
}
