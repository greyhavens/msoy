//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import mx.core.Application;

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
    // NOTE: The mode constants are defined in UberClientLoader, so that users of that
    // class do not also need to include this class, which will drag in all the world client
    // classes.

    public function UberClient (app :Application)
    {
        var mode :int;

        // determine how this app should be configured!
        var d :DisplayObject = app;
        while (d != null) {
            if (d is UberClientLoader) {
                mode = (d as UberClientLoader).getMode();
                MsoyParameters.get().mode = mode; // stash the mode in our real params
                setMode(app, mode);
                return;
            }
            try {
                d = d.parent;
            } catch (err :SecurityError) {
                d = null;
            }
        }

        var params :Object = MsoyParameters.get();
        if ("mode" in params) {
            // if a mode is specified, that overrides all
            mode = parseInt(params["mode"]);
        } else if ("avatar" in params) {
            mode = UberClientLoader.AVATAR_VIEWER;
        } else if ("media" in params) {
            mode = UberClientLoader.GENERIC_VIEWER;
        } else {
            mode = UberClientLoader.CLIENT;
        }
        setMode(app, mode, params);
    }

    protected function setMode (app :Application, mode :int, params :Object = null) :void
    {
        switch (mode) {
        default:
            new WorldClient(app.stage);
            break;

        case UberClientLoader.AVATAR_VIEWER:
        if (true) {
            // ye olde avatar viewer
            Object(app).setViewerObject(new AvatarViewerComp(params));
        } else {
            var sc :StudioClient = new StudioClient(app.stage);
            var rsv :RoomStudioView = sc.getPlaceView();
            rsv.initForViewing(params, mode);
            Object(app).setViewer(rsv);
        }
            break;

        case UberClientLoader.PET_VIEWER:
        case UberClientLoader.FURNI_VIEWER:
        case UberClientLoader.TOY_VIEWER:
        case UberClientLoader.DECOR_VIEWER:
        case UberClientLoader.GENERIC_VIEWER:
            Object(app).setViewerObject(new Viewer(params));
            break;
        }
    }
}
}
