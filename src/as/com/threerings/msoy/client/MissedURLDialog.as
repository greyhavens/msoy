//
// $Id$

package com.threerings.msoy.client {

import flash.system.System;

import mx.controls.Label;

import com.threerings.flex.CommandButton;

import com.threerings.msoy.ui.FloatingPanel;

public class MissedURLDialog extends FloatingPanel
{
    public function MissedURLDialog (ctx :MsoyContext, url :String)
    {
        super(ctx, Msgs.GENERAL.get("t.missedUrl"));
        showCloseButton = true;

        var label :Label = new Label();
        label.text = url;
        label.selectable = true;
        label.setStyle("fontSize", 14);
        addChild(label);

        addButtons(
            new CommandButton(Msgs.GENERAL.get("b.copy_to_clipboard"), System.setClipboard, url),
            OK_BUTTON);
        setButtonWidth(0); // free-size
        open(true);
    }
}
}
