//
// $Id$

package com.threerings.msoy.party.client {

import mx.core.ClassFactory;
import mx.core.ScrollPolicy;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.List;

import com.threerings.flex.CommandButton;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.world.client.WorldContext;

public class PartyBoardPanel extends FloatingPanel
{
    public function PartyBoardPanel (ctx :WorldContext)
    {
        super(ctx, Msgs.PARTY.get("t.board"));
        showCloseButton = true;
        setButtonWidth(0);
        _wctx = ctx;

        var cf :ClassFactory = new ClassFactory(PartyBoardInfoRenderer);
        cf.properties = { wctx: _wctx };
        _partyList = new List();
        _partyList.selectable = false;
        _partyList.itemRenderer = cf;
        _partyList.verticalScrollPolicy = ScrollPolicy.ON;
        _partyList.percentWidth = 100;
        _partyList.percentHeight = 100;

        var loading :Label = FlexUtil.createLabel(Msgs.PARTY.get("m.loading"), null);
        loading.percentWidth = 100;
        loading.percentHeight = 100;

        _content = new VBox();
        _content.width = 400; // TODO: tweak
        _content.height = 400; // TODO: tweak
        _content.addChild(loading);

        getPartyBoard();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        addChild(_content);

        addButtons(new CommandButton(Msgs.PARTY.get("b.create"),
            FloatingPanel.createPopper(function () :FloatingPanel {
                return new CreatePartyPanel(_wctx);
            })));
    }

    protected function getPartyBoard (query :String = null) :void
    {
        _wctx.getPartyDirector().getPartyBoard(gotPartyBoard, query);
    }

    /**
     * Called with the result of a getPartyBoard request.
     */
    protected function gotPartyBoard (result :Array) :void
    {
        _content.removeAllChildren();
        _content.addChild(_partyList);
        _partyList.dataProvider = result;
    }

    protected var _wctx :WorldContext;

    protected var _partyList :List;

    /** Contains either the loading Label or party List. */
    protected var _content :VBox;
}
}
