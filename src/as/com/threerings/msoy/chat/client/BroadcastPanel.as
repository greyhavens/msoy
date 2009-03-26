//
// $Id$

package com.threerings.msoy.chat.client {

import mx.controls.Label;

import com.threerings.util.Integer;
import com.threerings.presents.client.ResultAdapter;
import com.threerings.presents.client.ConfirmAdapter;

import com.threerings.msoy.ui.FloatingPanel;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyService;

import com.threerings.msoy.chat.client.MsoyChatDirector;

/**
 * Panel to be popped up when a user requests to broadcast a paid announcement. Retrieves the quote
 * from the server and if the user agrees, the broadcast is sent and the money deducted.
 */
public class BroadcastPanel extends FloatingPanel
{
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

    protected function gotQuote (result :int) :void
    {
        _quote = result;
        if (!isOpen()) {
            // if we weren't shown before, let createChildren take care of setting the bar value
            open();

        } else {
            // otherwise, update it
            _barCost.text = "" + result;
        }
    }

    protected function getQuoteFailed (cause :String) :void
    {
        _ctx.displayFeedback(MsoyCodes.GENERAL_MSGS, cause);
    }

    protected function broadcastSent () :void
    {
        // nothing to do here (probably)
    }

    protected function broadcastFailed (cause :String) :void
    {
        _ctx.displayFeedback(MsoyCodes.GENERAL_MSGS, cause);

        // TODO: this is just a stop-gap until a custom listener is added
        requestQuote();
    }

    override protected function okButtonClicked () :void
    {
        // TODO: use a custom listener here so the new quote can be returned if the price changes
        var client :MsoyClient = _ctx.getMsoyClient();
        var msoySvc :MsoyService = client.requireService(MsoyService) as MsoyService;
        msoySvc.purchaseAndSendBroadcast(client, _quote, _msg,
            new ConfirmAdapter(broadcastSent, broadcastFailed));
    }

    protected var _msg :String;
    protected var _barCost :Label;
    protected var _quote :int;
}
}
