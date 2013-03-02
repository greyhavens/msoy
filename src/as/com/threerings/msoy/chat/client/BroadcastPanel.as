//
// $Id$

package com.threerings.msoy.chat.client {

import mx.containers.HBox;
import mx.containers.VBox;
import mx.controls.Label;
import mx.controls.RadioButton;
import mx.controls.RadioButtonGroup;
import mx.controls.Text;

import com.threerings.util.StringUtil;

import com.threerings.presents.client.ResultAdapter;

import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.FlexUtil;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.MsoyService;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.PlaceInfo;
import com.threerings.msoy.money.client.BuyPanel;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;
import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Panel to be popped up when a user requests to broadcast a paid announcement. Retrieves the quote
 * from the server and if the user agrees, the broadcast is sent and the money deducted.
 */
public class BroadcastPanel extends FloatingPanel
{
    public function BroadcastPanel (ctx :MsoyContext, msg :String)
    {
        super(ctx, Msgs.CHAT.get("t.broadcast"));
        _msg = msg;
        open();

        var msoySvc :MsoyService = _ctx.getMsoyClient().requireService(MsoyService) as MsoyService;
        msoySvc.secureBroadcastQuote(new ResultAdapter(gotQuote, handleNoPermission));
    }

    protected function handleNoPermission (reason :String) :void
    {
        _ctx.displayFeedback(MsoyCodes.GENERAL_MSGS, reason);
        close();
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        styleName = "broadcastPanel";

        addChild(FlexUtil.createText("\" " + _msg + " \"", 350));

        _instructions = FlexUtil.createText(
            Msgs.CHAT.get("m.broadcast_instructions_initial", "..."), 350);
        addChild(_instructions);

        var hbox :HBox = new HBox();
        hbox.addChild(new CommandCheckBox("", setAgreeTOS));
        var tos :Label = new Label();
        tos.selectable = true;
        tos.htmlText = Msgs.CHAT.get("l.broadcast_tos");
        hbox.addChild(tos);
        addChild(hbox);

        addChild(_buyPanel = new BuyPanel(_ctx, processPurchase));
        _buyPanel.enabled = false;

        var vbox :VBox = new VBox();
        vbox.setStyle("horizontalAlignment", "left");
        vbox.addChild(makeLinkOption("m.br_link_none", true, true));
        var placeInfo :PlaceInfo = _ctx.getMsoyController().getPlaceInfo();
        vbox.addChild(makeLinkOption("m.br_link_room",
            placeInfo.sceneId != 0, false,
            Msgs.CHAT.get("m.visit_room", placeInfo.sceneId)));
        vbox.addChild(makeLinkOption("m.br_link_party",
            WorldContext(_ctx).getPartyDirector().isInParty(), false,
            Msgs.CHAT.get("m.view_party", WorldContext(_ctx).getPartyDirector().getPartyId())));
        addChild(vbox);

        addButtons(CANCEL_BUTTON);
    }

    protected function makeLinkOption (
        labelKey :String, enabled :Boolean, selected :Boolean, link :String = "") :RadioButton
    {
        var rb :RadioButton = new RadioButton();
        rb.enabled = enabled;
        rb.selected = selected;
        rb.group = _linkGroup;
        rb.label = Msgs.CHAT.get(labelKey);
        rb.value = link;
        return rb;
    }

    protected function gotQuote (quote :PriceQuote, first :Boolean = true) :void
    {
        _buyPanel.setPriceQuote(quote);
        _instructions.text = Msgs.CHAT.get(
            "m.broadcast_instructions_" + (first ? "initial" : "price_change"), quote.getCoins());
    }

    protected function broadcastSent (result :PriceQuote) :void
    {
        if (result != null) {
            // oops, the price went up, inform the user and keep the dialog open
            // TODO: do something more exciting here
            gotQuote(result, false);

        } else {
            // otherwise, close. The user should see the broadcast as feedback
            close();
        }
    }

    protected function setAgreeTOS (agree :Boolean) :void
    {
        _buyPanel.enabled = agree;
    }

    protected function processPurchase (currency :Currency, authedAmount :int) :void
    {
        var finalMsg :String = _msg;
        if (!StringUtil.isBlank(_linkGroup.selectedValue as String)) {
            finalMsg += " " + _linkGroup.selectedValue;
        }

        var msoySvc :MsoyService = _ctx.getMsoyClient().requireService(MsoyService) as MsoyService;
        msoySvc.purchaseAndSendBroadcast(authedAmount, finalMsg,
            _ctx.resultListener(broadcastSent, MsoyCodes.GENERAL_MSGS, null, _buyPanel));
    }

    protected var _msg :String;
    protected var _instructions :Text;
    protected var _buyPanel :BuyPanel;
    protected var _linkGroup :RadioButtonGroup = new RadioButtonGroup();
}
}
