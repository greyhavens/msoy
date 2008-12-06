//
// $Id$

package com.threerings.msoy.party.client {

import mx.core.ClassFactory;
import mx.controls.List;

import com.threerings.presents.client.ResultAdapter;

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

        var cf :ClassFactory = new ClassFactory(PartyRenderer);
        cf.properties =  { mctx: _ctx };
        _partyList = new List();
        _partyList.itemRenderer = cf;

        getPartyBoard();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addButtons(new CommandButton(Msgs.PARTY.get("b.create"), 
            FloatingPanel.createPopper(function () :FloatingPanel {
                return new CreatePartyPanel(_ctx);
            })));

        addChild(_partyList);
    }

    protected function getPartyBoard (query :String = null) :void
    {
        _ctx.getPartyDirector().getPartyBoard(gotPartyBoard, query);
    }

    /**
     * Called with the result of a getPartyBoard request.
     */
    protected function gotPartyBoard (result :Array) :void
    {
        _partyList.dataProvider = result;
    }

    protected var _partyList :List;
}
}
