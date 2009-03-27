//
// $Id$

package com.threerings.msoy.chat.client {

import mx.controls.Text;

import com.threerings.util.Log;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.flex.FlexUtil;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyService;

import com.threerings.msoy.money.client.BuyButton;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.msoy.money.data.all.PriceQuote;

import com.threerings.msoy.chat.client.MsoyChatDirector;

/**
 * Panel to be popped up when a user requests to broadcast a paid announcement. Retrieves the quote
 * from the server and if the user agrees, the broadcast is sent and the money deducted.
 */
public class BroadcastPanel extends FloatingPanel
{
    public static var log :Log = Log.getLog(BroadcastPanel);

    public function BroadcastPanel (ctx :MsoyContext, msg :String)
    {
        super(ctx, Msgs.CHAT.get("t.broadcast"));
        _msg = msg;
        open();
    }

    override protected function createChildren () :void
    {
        super.createChildren();
        styleName = "broadcastPanel";

        addChild(FlexUtil.createLabel(_msg));

        _instructions = new Text();
        _instructions.text = Msgs.CHAT.get("m.broadcast_instructions_initial", "...");
        _instructions.width = 350;
        addChild(_instructions);

        addChild(_barButton = new BuyButton(Currency.BARS, processPurchase));
        _barButton.enabled = false;

        showCloseButton = true;
    }

    /**
     * Asks the server for the going rate for a broadcast.
     */
    override protected function didOpen () :void
    {
        super.didOpen();

        var client :MsoyClient = _ctx.getMsoyClient();
        var msoySvc :MsoyService = client.requireService(MsoyService) as MsoyService;
        msoySvc.secureBroadcastQuote(client, new ResultAdapter(gotFirstQuote, getQuoteFailed));
    }

    protected function gotFirstQuote (result :PriceQuote) :void
    {
        gotQuote(result, true);
    }

    protected function gotQuote (result :PriceQuote, first :Boolean) :void
    {
        log.info("Got quote", "result", result);
        _quote = result.getBars();

        if (first) {
            _instructions.text = Msgs.CHAT.get("m.broadcast_instructions_initial", _quote);
        } else {
            _instructions.text = Msgs.CHAT.get("m.broadcast_instructions_price_change", _quote);
        }
        _barButton.setValue(_quote);
        _barButton.enabled = true;
    }

    protected function getQuoteFailed (cause :String) :void
    {
        log.info("Get quote failed", "cause", cause);
        _ctx.displayFeedback(MsoyCodes.GENERAL_MSGS, cause);
    }

    protected function broadcastSent (result :PriceQuote) :void
    {
        log.info("Broadcast sent", "result", result);
        if (result != null) {
            // oops, the price went up, inform the user and keep the dialog open
            // TODO: do something more exciting here
            gotQuote(result, false);

        } else {
            // otherwise, close. The user should see the broadcast as feedback
            close();
        }
    }

    protected function broadcastFailed (cause :String) :void
    {
        log.info("Broadcast failed", "cause", cause);
        _ctx.displayFeedback(MsoyCodes.GENERAL_MSGS, cause);
    }

    protected function processPurchase () :void
    {
        var client :MsoyClient = _ctx.getMsoyClient();
        var msoySvc :MsoyService = client.requireService(MsoyService) as MsoyService;
        msoySvc.purchaseAndSendBroadcast(client, _quote, _msg,
            new ResultAdapter(broadcastSent, broadcastFailed));
    }

    protected var _msg :String;
    protected var _instructions :Text;
    protected var _quote :int;
    protected var _barButton :BuyButton;
}
}
