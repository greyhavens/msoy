//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.presents.client.ConfirmAdapter;

import com.threerings.msoy.client.MsoyContext;

/**
 * A confirm listener that reports success and failure via a chat feedback message.
 */
public class ReportingListener extends ConfirmAdapter
{
    public function ReportingListener (
        ctx :MsoyContext, msgBundle :String = null, errWrap :String = null, success :String = null)
    {
        super(ctx.chatErrHandler(msgBundle, errWrap),
            (success == null) ? null :
            function () :void {
                ctx.displayFeedback(msgBundle, success);
            });
    }
}
}
