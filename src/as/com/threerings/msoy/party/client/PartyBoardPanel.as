//
// $Id$

package com.threerings.msoy.party.client {

import mx.core.ClassFactory;
import mx.controls.Label;
import mx.controls.List;

import com.threerings.presents.client.ResultAdapter;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

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
        setButtonWidth(0);

        var cf :ClassFactory = new ClassFactory(PartyInfoRenderer);
        cf.properties =  { mctx: _ctx };
        _partyList = new List();
        _partyList.itemRenderer = cf;

        getPartyBoard();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(_loading);
        addChild(_partyList);

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
    protected function gotPartyBoard (result :Array) :void
    {
        removeChild(_loading);
        _partyList.dataProvider = result;
    }

    protected var _partyList :List;

    protected var _loading :Label = FlexUtil.createLabel(Msgs.PARTY.get("m.loading"), null);
}
}
