//
// $Id$

package com.threerings.msoy.client {

import flash.events.TimerEvent;
import flash.utils.Timer;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.notify.data.Notification;

public class UpsellDirector extends BasicDirector
{
    public const log :Log = Log.getLog(this);

    public function UpsellDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);
    }

    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        _timer.stop();
    }

    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);

        _timer.reset();
        _timer.start();
        _shownBubble = false;
    }

    /**
     * Handle timer expiration.
     */
    protected function handleTimer (event :TimerEvent) :void
    {
        var embedded :Boolean = _mctx.getMsoyClient().isEmbedded();
        var isGuest :Boolean = MemberObject(_mctx.getClient().getClientObject()).isPermaguest();
        var inGame :Boolean = Boolean(_mctx.getMsoyController().getPlaceInfo()[0]);

        if (showNotification(embedded, isGuest, inGame)) {
            return;
        }

        // else: do other nutty upsell stuff.
        if ((_timer.currentCount > 4) && /*embedded && isGuest &&*/ inGame && !_shownBubble) {
            var msg :String =
                RandomUtil.pickRandom(Msgs.GENERAL.getAll("x.up_gamebubble")) as String;
            if (msg != null) {
                BubblePopup.showHelpBubble(_mctx, _mctx.getControlBar().shareBtn, msg, -7);
                _shownBubble = true;
            }
        }
    }

    /**
     * Show a standard upsell notification.
     */
    protected function showNotification (
        embedded :Boolean, isGuest :Boolean, inGame :Boolean) :Boolean
    {
        var embed :String = embedded ? "embed" : "site";
        var guest :String = isGuest ?  "guest" : "mem";
        var place :String = inGame ? "game" : "room";
        var mins :String = String(_timer.currentCount);

        for (var ii :int = 0; ii < (1 << 3); ii++) {
            // see the note inside the messagebundle for wtf is up with the key
            var key :String = "x.up_notif" +
                ((ii % 2 == 0) ? embed : "x") + "_" +
                (((ii >> 1) % 2 == 0) ? guest : "x") + "_" +
                (((ii >> 2) % 2 == 0) ? place : "x") + "_" +
                mins;
            if (Msgs.GENERAL.exists(key)) {
                var placeInfo :Array = _mctx.getMsoyController().getPlaceInfo();
                var msg :String = Msgs.GENERAL.get(key, String(placeInfo[1]), String(placeInfo[2]));
                _mctx.getNotificationDirector().addGenericNotification(
                    MessageBundle.taint(msg), Notification.SYSTEM);
                return true;
            }
        }

        return false;
    }

    protected var _mctx :MsoyContext;

    protected var _timer :Timer = new Timer(6 * 1000);

    protected var _shownBubble :Boolean;
}
}
