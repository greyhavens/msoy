//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

/**
 * A dialog used to configure a new party for creation.
 */
public class CreatePartyPanel extends FloatingPanel
{
    public function CreatePartyPanel (ctx :MsoyContext)
    {
        super(ctx, Msgs.PARTY.get("t.create"));
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addButtons(OK_BUTTON, CANCEL_BUTTON);
    }

    override protected function getButtonLabel (buttonId :int) :String
    {
        switch (buttonId) {
        case OK_BUTTON: return Msgs.PARTY.get("b.create");
        default: return super.getButtonLabel(buttonId);
        }
    }

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON) {
            _ctx.getPartyDirector().createParty("My party TODO", 0);
        }

        super.buttonClicked(buttonId);
    }
}
}
