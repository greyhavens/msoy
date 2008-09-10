//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;

import com.threerings.presents.client.InvocationService_ConfirmListener;

import com.threerings.msoy.client.MsoyContext;

/**
 * Reports service failure via a chat feedback message.
 */
public class ReportingListener
    implements InvocationService_ConfirmListener
{
    public function ReportingListener (
        ctx :MsoyContext, msgBundle :String = null, errWrap :String = null, success :String = null)
    {
        _ctx = ctx;
        _bundle = msgBundle;
        _errWrap = errWrap;
        _success = success;
    }

    // documentation inherited from ConfirmListener
    public function requestProcessed () :void
    {
        if (_success != null) {
            _ctx.displayFeedback(_bundle, _success);
        }
    }

    // documentation inherited from ConfirmListener
    public function requestFailed (cause :String) :void
    {
        Log.getLog(this).info("Reporting failure [bundle=" + _bundle + ", reason=" + cause + "].");
        if (_errWrap != null) {
            cause = MessageBundle.compose(_errWrap, cause);
        }
        _ctx.displayFeedback(_bundle, cause);
    }

    protected var _ctx :MsoyContext;
    protected var _bundle :String;
    protected var _errWrap :String;
    protected var _success :String;
}
}
