//
// $Id$

package com.threerings.msoy.client {

import flash.external.ExternalInterface;

import com.threerings.util.Log;
import com.threerings.util.LogTarget;

public class LoggingTargets
{
    public static function configureLogging (ctx :MsoyContext) :void
    {
        // for now, everything logs to the FireBug console
        try {
            if (_bugTarget == null && ExternalInterface.available) {
                ExternalInterface.call("console.debug", "Msoy console logging enabled");
                _bugTarget = new FireBugTarget();
                Log.addTarget(_bugTarget);
            }
        } catch (err :Error) {
            // oh well!
        }

        if (null != MsoyParameters.get()["test"]) {
            if (_chatTarget == null) {
                _chatTarget = new ChatTarget(ctx);
                Log.addTarget(_chatTarget);
            }
        } else if (_chatTarget != null) {
            Log.removeTarget(_chatTarget);
            _chatTarget = null;
        }
    }

    protected static var _bugTarget :LogTarget;
    protected static var _chatTarget :LogTarget;
}
}

import flash.external.ExternalInterface;

import com.threerings.util.LogTarget;
import com.threerings.util.MessageBundle;

import com.threerings.msoy.client.MsoyContext;

class ChatTarget
    implements LogTarget
{
    public function ChatTarget (ctx :MsoyContext)
    {
        _ctx = ctx;
    }

    // from LogTarget
    public function log (msg :String) :void
    {
        _ctx.displayInfo(null, MessageBundle.taint(msg));
    }

    protected var _ctx :MsoyContext;
}

/**
 * A logging target that goes to firebug's console.
 */
class FireBugTarget
    implements LogTarget
{
    // from LogTarget
    public function log (msg :String) :void
    {
        try {
            ExternalInterface.call("console.debug", msg);
        } catch (err :Error) {
            // drop it, it's ok.
        }
    }
}
