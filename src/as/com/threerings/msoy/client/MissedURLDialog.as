//
// $Id$

package com.threerings.msoy.client {

import flash.system.System;

import mx.controls.Text;

import com.threerings.msoy.ui.CopyableText;
import com.threerings.msoy.ui.FloatingPanel;

public class MissedURLDialog extends FloatingPanel
{
    public function MissedURLDialog (ctx :MsoyContext, url :String)
    {
        super(ctx, Msgs.GENERAL.get("t.missedUrl"));
        showCloseButton = true;

        var text :Text = new Text();
        text.text = Msgs.GENERAL.get("m.missedUrl");
        text.width = Math.min(400, ctx.getTopPanel().width - 100);
        addChild(text);

        addChild(new CopyableText(url));

        //setButtonWidth(0); // free-size
        addButtons(OK_BUTTON);
        open(true);
    }
}
}
