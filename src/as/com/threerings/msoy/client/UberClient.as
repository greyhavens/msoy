//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import mx.core.Application;

import com.threerings.util.ParameterUtil;

import com.threerings.msoy.utils.UberClientLoader;

import com.threerings.msoy.world.client.AvatarViewerComp;
import com.threerings.msoy.world.client.WorldClient;
import com.threerings.msoy.world.client.Viewer;

/**
 * Assists in the usage of the UberClient.
 * This could just be part of world.mxml, but I don't like having a bunch of code
 * inside a CDATA block.
 */
public class UberClient
{
    /** Mode constants. */
    public static const CLIENT :int = 0;
    public static const AVATAR_VIEWER :int = 100;
    public static const GENERIC_VIEWER :int = 199;

    public function UberClient (app :Application)
    {
        // determine how this app should be configured!
        var d :DisplayObject = app;
        while (d != null) {
            if (d is UberClientLoader) {
                setMode(app, (d as UberClientLoader).getMode());
                return;
            }
            try {
                d = d.parent;
            } catch (err :SecurityError) {
                d = null;
            }
        }

        // if we never found our mode here, try to determine it from our parameters
        ParameterUtil.getParameters(app, function (params :Object) :void {
            var mode :int = CLIENT;

            if ("avatar" in params) {
                mode = AVATAR_VIEWER;
            } else if ("media" in params) {
                mode = GENERIC_VIEWER;
            }

            setMode(app, mode, params);
        });
    }

    protected function setMode (app :Application, mode :int, params :Object = null) :void
    {
        switch (mode) {
        default:
            new WorldClient(app.stage);
            break;

        case AVATAR_VIEWER:
            Object(app).setViewer(new AvatarViewerComp(params));
            break;

        case GENERIC_VIEWER:
            Object(app).setViewer(new Viewer(params));
            break;
        }
    }
}
}
