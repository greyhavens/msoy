//
// $Id$

package com.threerings.msoy.chat.client {

import mx.controls.Label;

import com.threerings.util.Integer;
import com.threerings.util.Log;
import com.threerings.presents.client.ResultAdapter;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyService;

import com.threerings.msoy.money.data.all.PriceQuote;

import com.threerings.msoy.chat.client.MsoyChatDirector;

/**
 * Panel to be popped up when a user requests to broadcast a paid announcement. Retrieves the quote
 * from the server and if the user agrees, the broadcast is sent and the money deducted.
 */
public class BroadcastPanel extends FloatingPanel
{
    public static var log :Log = Log.getLog(BroadcastPanel);

    /**
     * Shows the panel and if everything is confirmed broadcasts the given message for a fee.
     */
    public static function show (ctx :MsoyContext, msg :String) :void
    {
        var panel :BroadcastPanel = new BroadcastPanel(ctx, msg);
        panel.requestQuote();
    }

    public function BroadcastPanel (ctx :MsoyContext, msg :String)
    {
        super(ctx);
        _msg = msg;
    }

    override protected function createChildren () :void
    {
        // TODO: add text that explains what's going on
        // TODO: big orange buy button with bar graphic and big orange "get bars" button
        super.createChildren();
        _barCost = new Label();
        _barCost.text = "" + _quote;
        addChild(_barCost);
        addButtons(OK_BUTTON);
        showCloseButton = true;
    }

    /**
     * Asks the server for the going rate for a broadcast.
     */
    protected function requestQuote () :void
    {
        var client :MsoyClient = _ctx.getMsoyClient();
        var msoySvc :MsoyService = client.requireService(MsoyService) as MsoyService;
        msoySvc.secureBroadcastQuote(client, new ResultAdapter(gotQuote, getQuoteFailed));
    }

    protected function gotQuote (result :PriceQuote) :void
    {
        log.info("Got quote", "result", result);
        _quote = result.getBars();
        if (!isOpen()) {
            // if we weren't shown before, let createChildren take care of setting the bar value
            open();

        } else {
            // otherwise, update it
            _barCost.text = "" + _quote;
        }
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
            gotQuote(result);

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

    override protected function buttonClicked (buttonId :int) :void
    {
        if (buttonId == OK_BUTTON) {
            var client :MsoyClient = _ctx.getMsoyClient();
            var msoySvc :MsoyService = client.requireService(MsoyService) as MsoyService;
            msoySvc.purchaseAndSendBroadcast(client, _quote, _msg,
                new ResultAdapter(broadcastSent, broadcastFailed));
        } else {
            super.buttonClicked(buttonId);
        }
    }

    protected var _msg :String;
    protected var _barCost :Label;
    protected var _quote :int;
}
}
