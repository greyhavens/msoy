//
// $Id$

package com.threerings.msoy.party.client {

import mx.core.ClassFactory;
import mx.core.ScrollPolicy;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.List;
import mx.controls.Text;

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
        _content.width = 400;
        _content.height = 380;
        _content.addChild(loading);

        getPartyBoard();
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var top :VBox = new VBox();
        top.percentWidth = 100;
        top.styleName = "panelBottom";
        addChild(top);

        var sep :VBox = new VBox();
        sep.percentWidth = 100;
        sep.height = 1;
        sep.styleName = "panelBottomSeparator";
        addChild(sep);

        var text :Text = FlexUtil.createWideText(null);
        text.htmlText = Msgs.PARTY.get("m.about");
        top.addChild(text);

        addChild(_content);

        addButtons(new CommandButton(Msgs.PARTY.get("b.create"),
            FloatingPanel.createPopper(function () :FloatingPanel {
                return new CreatePartyPanel(_wctx);
            })));
        _buttonBar.styleName = "buttonPadding"; // pad out the buttons since we have no border
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

        if (result.length > 0) {
            _content.addChild(_partyList);
            _partyList.dataProvider = result;
        } else {
            var none :Label = FlexUtil.createLabel(Msgs.PARTY.get("m.no_parties"), null);
            none.percentWidth = 100;
            none.percentHeight = 100;
            _content.addChild(none);
        }
    }

    protected var _wctx :WorldContext;

    protected var _partyList :List;

    /** Contains either the loading Label or party List. */
    protected var _content :VBox;
}
}
