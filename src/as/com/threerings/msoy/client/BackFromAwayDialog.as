//
// $Id$

package com.threerings.msoy.client {

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

        setButtonWidth(0);
        addButtons(OK_BUTTON);
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        return Msgs.GENERAL.get("b.Imback"); // we only have one button
    }

    protected var _msg :String;
}
}
