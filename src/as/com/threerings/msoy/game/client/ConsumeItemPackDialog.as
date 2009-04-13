//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.flex.FlexUtil;
import com.threerings.util.StringUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.ui.FloatingPanel;

/**
 * Displays a request for the user to confirm the consumption of an item pack.
 */
public class ConsumeItemPackDialog extends FloatingPanel
{
    public function ConsumeItemPackDialog (
        ctx :MsoyContext, name :String, msg :String, onConfirm :Function)
    {
        super(ctx, Msgs.GAME.get("t.consume_item_pack"));
        _onConfirm = onConfirm;
        if (!StringUtil.isBlank(msg)) {
            addChild(FlexUtil.createText(msg, 300));
        }
        addChild(FlexUtil.createText(Msgs.GAME.get("m.consume_item_pack", name), 300));
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        switch (buttonId) {
        case CANCEL_BUTTON: return Msgs.GAME.get("b.no");
        case OK_BUTTON: return Msgs.GAME.get("b.yes");
        default: return super.getButtonLabel(buttonId)
        }
    }

    override protected function okButtonClicked () :void
    {
        _onConfirm();
    }

    protected var _onConfirm :Function;
}
}
