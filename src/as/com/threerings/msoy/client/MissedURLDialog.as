//
// $Id$

package com.threerings.msoy.client {

import flash.system.System;

import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.CopyableText;
import com.threerings.msoy.ui.FloatingPanel;

public class MissedURLDialog extends FloatingPanel
{
    public function MissedURLDialog (ctx :MsoyContext, url :String)
    {
        super(ctx, Msgs.GENERAL.get("t.missedUrl"));
        showCloseButton = true;

        addChild(FlexUtil.createText(Msgs.GENERAL.get("m.missedUrl"),
            Math.min(400, ctx.getTopPanel().width - 100)));

        addChild(new CopyableText(url));

        //setButtonWidth(0); // free-size
        addButtons(OK_BUTTON);
        open(true);
    }
}
}
