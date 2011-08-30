//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.external.ExternalInterface;
import flash.net.URLVariables;

import com.threerings.util.ParameterUtil;

import com.threerings.msoy.data.UberClientModes;

/**
 * A small utility class for handling parameters in whirled.
 */
public class MsoyParameters
{
    /**
     * Return the parameters in use on this msoy client.
     */
    public static function get () :Object
    {
        return _params;
    }

    /**
     * Initialize.
     */
    public static function init (disp :DisplayObject, thenRun :Function) :void
    {
        if (_params != null) {
            thenRun();
            return;
        }

        var d :DisplayObject = disp;
        while (d != null) {
            try {
                var s :String = Object(d).getWhirledParams();
                set(new URLVariables(s), disp);
                thenRun();
                return;
            } catch (err :Error) {
                // fall through
            }
            try {
                d = d.parent;
            } catch (err :Error) {
                d = null;
            }
        }

        ParameterUtil.getParameters(disp, function (params :Object) :void {
            set(params, disp);
            thenRun();
        });
    }

    /**
     * Set the parameters we're using, and do any required processing.
     */
    protected static function set (params :Object, disp :DisplayObject) :void
    {
        _params = params;

        if (_params["mode"] == UberClientModes.STUB) {
            massageStubParams(disp);
        }
    }

    /**
     * Converts old skool embed parameters to new world order.
     */
    protected static function massageStubParams (disp :DisplayObject) :void
    {
        const gameId :int = int(_params["game"]);
        const roomId :int = int(_params["room"]);
        const avrGameId :int = int(_params["avrgame"]);
        delete _params["game"];
        delete _params["room"];
        delete _params["avrgame"];

        // maybe set up a vector if none already provided
        if (!("vec" in _params)) {
            var stubURL :String = disp.root.loaderInfo.loaderURL;
            var pageURL :String = null;
            try {
                pageURL = ExternalInterface.call("window.location.href.toString");
            } catch (e :Error) {
                // oh well
            }

            const site :String = figureSiteFromUrl(pageURL || stubURL);
            if (gameId != 0) {
                _params["vec"] = "e." + site + ".games." + gameId;
            } else {
                _params["vec"] = "e." + site + ".rooms." + roomId;
            }
        }

        // and our other params
        if (avrGameId != 0) {
            _params["worldGame"] = avrGameId;
            _params["gameRoomId"] = roomId;

        } else if (gameId != 0) {
            _params["gameId"] = gameId;

        } else if (roomId != 0) {
            _params["sceneId"] = roomId;
        }
    }

    protected static function figureSiteFromUrl (url :String) :String
    {
        const URL_REGEXP :RegExp = /^(\w+:\/\/)?\/?([^:\/\s]+)/; // protocol and host
        var result :Object = URL_REGEXP.exec(url);
        if (result == null) {
            return "unknown";
        }

        // TODO?
        // turn www.bullshit.twerk.com into "twerk"
        var host :String = String(result[2]);
        // strip the last part
        var lastdot :int = host.lastIndexOf(".");
        if (lastdot != -1) {
            host = host.substring(0, lastdot);
        }
        // now just keep the last part
        lastdot = host.lastIndexOf(".");
        if (lastdot != -1) {
            host = host.substring(lastdot + 1);
        }
        return massageHost(host);
    }

    // TEMP?
    protected static function massageHost (host :String) :String
    {
        switch (host) {
        case "ungrounded": return "newgrounds";
        default: return host;
        }
    }

    /** The parameters. */
    protected static var _params :Object;
}
}
