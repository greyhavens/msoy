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
import com.threerings.msoy.swiftly.data.BuildResult;
import com.threerings.msoy.swiftly.data.ProjectRoomObject;
import com.threerings.msoy.swiftly.data.SwiftlyCodes;
import com.threerings.msoy.swiftly.util.SwiftlyContext;
import com.threerings.presents.client.InvocationService.ConfirmListener;
import com.threerings.util.MessageBundle;

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
        buildStarted();
        _roomObj.service.buildProject(_ctx.getClient(), new ConfirmListener () {
            public void requestProcessed ()
            {
                buildFinished();
            }
            public void requestFailed (String reason) {
                _ctx.showErrorMessage(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, reason));
                buildFinished();
            }
        });

    }

    /**
     * Triggered by {@link #buildExportAction}.
     */
    protected void buildAndExport ()
    {
       buildStarted();
        _roomObj.service.buildAndExportProject(_ctx.getClient(), new ConfirmListener() {
            public void requestProcessed () {
                BuildResult result = _roomObj.findResultForMember(
                    _ctx.getMemberObject().memberName);
                if (result.buildSuccessful()) {
                    _ctx.showInfoMessage(_msgs.get("m.build_export_succeeded"));
                }
                buildFinished();
            }
            public void requestFailed (String reason) {
                _ctx.showErrorMessage(_ctx.xlate(SwiftlyCodes.SWIFTLY_MSGS, reason));
                buildFinished();
            }
        });
    }

    /**
     * Disable the build actions while a build is happening.
     */
    protected void buildStarted ()
    {
        // disable the action on this client
        buildAction.setEnabled(false);
        buildExportAction.setEnabled(false);
        BuildResult result = _roomObj.findResultForMember(_ctx.getMemberObject().memberName);
        if (result != null) {
            _ctx.showProgress((int)result.getBuildTime());
        } else {
            _ctx.showProgress(DEFAULT_BUILD_TIME);
        }
    }

    /**
     * Enable the build actions when a build is finished.
     */
    protected void buildFinished ()
    {
        // enable the action on this client if the user has write access
        if (_roomObj.hasWriteAccess(_ctx.getMemberObject().memberName)) {
            buildAction.setEnabled(true);
            buildExportAction.setEnabled(true);
        }
        _ctx.stopProgress();
    }

    /** The first build will be guessed to be 6 seconds. */
    protected static final int DEFAULT_BUILD_TIME = 6000;

    protected SwiftlyContext _ctx;
    protected MessageBundle _msgs;
    protected ProjectRoomObject _roomObj;
}
