//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import mx.core.Application;

import com.threerings.msoy.data.UberClientModes;
import com.threerings.msoy.utils.UberClientLoader;

import com.threerings.msoy.world.client.AvatarViewerComp;
import com.threerings.msoy.world.client.WorldClient;
import com.threerings.msoy.world.client.Viewer;

import com.threerings.msoy.world.client.StudioClient;
import com.threerings.msoy.world.client.RoomStudioView;

/**
 * Assists in the usage of the UberClient.
 * This could just be part of world.mxml, but I don't like having a bunch of code
 * inside a CDATA block.
 */
public class UberClient
{
    // NOTE: The mode constants are defined in UberClientModes, so that users of that
    // class do not also need to include this class, which will drag in all the world client
    // classes.

    public function UberClient (app :Application)
    {
        var mode :int;
        var params :Object = MsoyParameters.get();

        // determine how this app should be configured!
        var d :DisplayObject = app;
        while (d != null) {
            if (d is UberClientLoader) {
                var ucl :UberClientLoader = d as UberClientLoader;

                mode = ucl.getMode();

                // stash the width/height in our real params
                params.width = ucl.width;
                params.height = ucl.height;

                setMode(app, mode, params);
                return;
            }
            try {
                d = d.parent;
            } catch (err :SecurityError) {
                d = null;
            }
        }

        if ("mode" in params) {
            // if a mode is specified, that overrides all
            mode = parseInt(params["mode"]);
        } else if ("avatar" in params) {
            mode = UberClientModes.AVATAR_VIEWER;
        } else if ("media" in params) {
            mode = UberClientModes.GENERIC_VIEWER;
        } else {
            mode = UberClientModes.CLIENT;
        }
        setMode(app, mode, params);
    }

    protected function setMode (app :Application, mode :int, params :Object = null) :void
    {
        // Stash the mode back into the real parameters, in case we figured it out
        // somehow else.
        if (params != null) {
            params.mode = mode;
        }

        switch (mode) {
        default:
            new WorldClient(app.stage);
            break;

        case UberClientModes.AVATAR_VIEWER:
        if (true) {
            // ye olde avatar viewer
            Object(app).setViewerObject(new AvatarViewerComp(params));
        } else {
            var sc :StudioClient = new StudioClient(app.stage, params);
            var rsv :RoomStudioView = sc.getPlaceView();
            rsv.initForViewing(params, mode);
            Object(app).setViewer(rsv);
        }
            break;

        case UberClientModes.PET_VIEWER:
        case UberClientModes.FURNI_VIEWER:
        case UberClientModes.TOY_VIEWER:
        case UberClientModes.DECOR_VIEWER:
        case UberClientModes.GENERIC_VIEWER:
            Object(app).setViewerObject(new Viewer(params));
            break;
        }
    }
}
}
