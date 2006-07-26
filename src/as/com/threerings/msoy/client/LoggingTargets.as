package com.threerings.msoy.client {

import flash.external.ExternalInterface;

import mx.logging.Log;

import com.threerings.msoy.data.MemberObject;

// TODO: stop listening at the end?
public class LoggingTargets
{
    public static function configureLogging (ctx :MsoyContext) :void
    {
        var userObj :MemberObject = ctx.getClientObject();

        // for now, everything logs to the FireBug console
        try {
            if (ExternalInterface.available) {
                ExternalInterface.call("console.debug",
                        "Msoy console logging enabled");
                mx.logging.Log.addTarget(new FireBugTarget());
            }
        } catch (err :Error) {
            // oh well!
        }

        // admins log to the chatbox
        if (userObj.getTokens().isAdmin()) {
            mx.logging.Log.addTarget(new ChatTarget(ctx));
        }
    }
}
}

import flash.external.ExternalInterface;

import mx.logging.LogEventLevel;
import mx.logging.targets.LineFormattedTarget;

import mx.core.mx_internal;

use namespace mx_internal;

import com.threerings.util.MessageBundle;

import com.threerings.msoy.client.MsoyContext;

// TODO: stop listening at the end?
class ChatTarget extends LineFormattedTarget
{
    public function ChatTarget (ctx :MsoyContext)
    {
        _ctx = ctx;
        super();

        includeCategory = includeTime = includeLevel = true;
        filters = ["*"];
        level = LogEventLevel.DEBUG;
    }

    override mx_internal function internalLog (msg :String) :void
    {
        _ctx.displayInfo(null, MessageBundle.taint(msg));
    }

    protected var _ctx :MsoyContext;
}

/**
 * A logging target that goes to firebug's console.
 */
class FireBugTarget extends LineFormattedTarget
{
    public function FireBugTarget ()
    {
        super();

        includeCategory = includeTime = includeLevel = true;
        filters = ["*"];
        level = LogEventLevel.DEBUG;
    }

    override mx_internal function internalLog (msg :String) :void
    {
        // TEMP: needed because of bug in ExternalInterface
        // (long lines are unterminated)
        var max_length :int = 79;
        while (msg.length > max_length) {
            ExternalInterface.call("console.debug",
                msg.substring(0, max_length));
            msg = msg.substring(max_length);
        }
        // END: temp

        ExternalInterface.call("console.debug", msg);
    }
}
