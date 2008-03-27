//
// $Id$

package com.threerings.msoy.client {

import mx.controls.Button;
import mx.controls.Label;

import com.threerings.msoy.ui.FloatingPanel;

/**
 * Displays a simple "I'm away" dialog.
 */
public class BackFromAwayDialog extends FloatingPanel
{
    public function BackFromAwayDialog (ctx :MsoyContext, msg :String)
    {
        super(ctx, Msgs.GENERAL.get("t.backFromAway"));

        _msg = msg;
        open(true);
    }

    override public function close () :void
    {
        super.close();
        _ctx.getMsoyController().setAway(false);
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var label :Label = new Label();
        label.text = "\"" + _msg + "\"";
        addChild(label);

        addButtons(OK_BUTTON);
        _buttonBar.setStyle("buttonWidth", 100);
    }

    override protected function createButton (buttonId :int) :Button
    {
        var b :Button = super.createButton(buttonId);
        b.label = Msgs.GENERAL.get("b.Imback");
        return b;
    }

    protected var _msg :String;
}
}
