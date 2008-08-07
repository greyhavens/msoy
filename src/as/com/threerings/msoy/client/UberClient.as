//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;

import mx.core.Application;

import com.threerings.msoy.data.UberClientModes;
import com.threerings.msoy.utils.UberClientLoader;

import com.threerings.msoy.room.client.WorldClient;
import com.threerings.msoy.room.client.Viewer;

import com.threerings.msoy.room.client.StudioClient;
import com.threerings.msoy.room.client.RoomStudioView;

/**
 * Assists in the usage of the UberClient.
 * This could just be part of world.mxml, but I don't like having a bunch of code
 * inside a CDATA block.
 */
public class UberClient
{
    /**
     * Convenience method: Are we running in a regular damn client?
     */
    public static function isRegularClient () :Boolean
    {
        return (UberClientModes.CLIENT == getMode());
    }

    /**
     * Convenience method: Are we showing the featured places?
     */
    public static function isFeaturedPlaceView () :Boolean
    {
        return (UberClientModes.FEATURED_PLACE == getMode());
    }

    /**
     * Get the client mode. Only valid after initialization.
     */
    public static function getMode () :int
    {
        return _mode;
    }

    // NOTE: The mode constants are defined in UberClientModes, so that users of that
    // class do not also need to include this class, which will drag in all the world client
    // classes.

    public static function init (app :Application) :void
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
        } else if ("featuredPlace" in params) {
            mode = UberClientModes.FEATURED_PLACE;
        } else if ("avatar" in params) {
            mode = UberClientModes.AVATAR_VIEWER;
        } else if ("media" in params) {
            mode = UberClientModes.GENERIC_VIEWER;
        } else {
            mode = UberClientModes.CLIENT;
        }
        setMode(app, mode, params);
    }

    /**
     * Effects the setting of the mode and final setup of the client.
     */
    protected static function setMode (app :Application, mode :int, params :Object = null) :void
    {
        _mode = mode;

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
        case UberClientModes.PET_VIEWER:
        case UberClientModes.DECOR_VIEWER:
        case UberClientModes.FURNI_VIEWER:
        case UberClientModes.TOY_VIEWER:
            var sc :StudioClient = new StudioClient(app.stage, params);
            var rsv :RoomStudioView = sc.getPlaceView();
            rsv.initForViewing(params, mode);
            Object(app).setViewer(rsv);
            break;

        case UberClientModes.GENERIC_VIEWER:
            Object(app).setViewerObject(new Viewer(params));
            break;
        }
    }

    /** The mode, once we've figured it out. */
    protected static var _mode :int;
}
}
