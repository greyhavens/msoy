//
// $Id$

package com.threerings.msoy.party.client {

import com.threerings.flex.CommandButton;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.party.data.PartyInfo;

public class PartyBoardPanel extends FloatingPanel
{
    public function PartyBoardPanel (ctx :MsoyContext)
    {
        super(ctx, Msgs.PARTY.get("t.board"));
        showCloseButton = true;

        getPartyBoard();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addButtons(new CommandButton(Msgs.PARTY.get("b.create"), 
            FloatingPanel.createPopper(function () :FloatingPanel {
                return new CreatePartyPanel(_ctx);
            })));
    }

    protected function getPartyBoard (query :String = null) :void
    {
        _ctx.getPartyDirector().getPartyBoard(gotPartyBoard, query);
    }

    /**
     * Called with the result of a getPartyBoard request.
     */
    protected function gotPartyBoard (result :Object) :void
    {
        // TODO (result is an Array of PartyInfo objects, or should be.)

        var infos :Array = result as Array;
        trace("We got infos: " + infos.length);
        for each (var info :PartyInfo in infos) {
            trace("Party id " + info.id);
        }
    }
}
}
