//
// $Id$

package com.threerings.msoy.client {

import flash.events.MouseEvent;
import flash.events.TimerEvent;
import flash.display.DisplayObject;
import flash.utils.Timer;

import com.threerings.util.ArrayUtil;
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
//    public const log :Log = Log.getLog(this);

    public function UpsellDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);
    }

    override public function clientDidLogoff (event :ClientEvent) :void
    {
        super.clientDidLogoff(event);

        if (!event.isSwitchingServers()) {
            _timer.stop();
            _locType = 0;
            _shown = {};
            _clicked = {};
        }
    }

    /**
     * Hack. This is called by MsoyController. God dammit.
     */
    public function locationUpdated () :void
    {
        var placeInfo :Array = _mctx.getMsoyController().getPlaceInfo();
        var newLocType :int = (placeInfo[1] == null) ? 0 : (Boolean(placeInfo[0]) ? 1 : 2);
        if (_locType != newLocType) {
            _locType = newLocType;
            _timer.reset();
            if (_locType != 0) {
                _timer.start();
            }
        }
    }

    /**
     * Handle timer expiration.
     */
    protected function handleTimer (event :TimerEvent) :void
    {
        var placeInfo :Array = _mctx.getMsoyController().getPlaceInfo();
        var embed :String = _mctx.getMsoyClient().isEmbedded() ? "embed" : "site";
        var guest :String = MemberObject(_mctx.getClient().getClientObject()).isPermaguest() ?
            "guest" : "mem";
        var place :String = Boolean(placeInfo[0]) ? "game" : "room";
        var mins :String = String(_timer.currentCount);

        for (var ii :int = 0; ii < (1 << 4); ii++) {
            var isNotif :Boolean = ((ii >> 3) % 2 == 0);
            // see the note inside the messagebundle for wtf is up with the key
            var key :String = "x.up_" + (isNotif ? "notif" : "bub") + "_" +
                ((ii % 2 == 0) ? embed : "x") + "_" +
                (((ii >> 1) % 2 == 0) ? guest : "x") + "_" +
                (((ii >> 2) % 2 == 0) ? place : "x") + "_" +
                mins + ".";
            if (Boolean(_shown[key])) {
                continue; // skip it, we've already shown this key during this session
            }
            var actualKey :String = RandomUtil.pickRandom(
                ArrayUtil.keys(Msgs.GENERAL.getAllMapped(key))) as String;
            if (actualKey != null) {
                // we're doing it, we're actually showing something!
                var msg :String = Msgs.GENERAL.get(actualKey,
                    String(placeInfo[1]), String(placeInfo[2]));

                // TODO: someday not track every little piece of spaz?
                var clientAction :String = "2009_03_upsell_" + actualKey;
                var clickedTracker :Function = function () :void {
                    // only track the click once
                    if (!Boolean(_clicked[key])) {
                        _mctx.getMsoyClient().trackClientAction(clientAction + "_clicked", null);
                        _clicked[key] = true;
                    }
                };

                if (isNotif) {
                    _mctx.getNotificationDirector().addGenericNotification(
                        MessageBundle.taint(msg), Notification.SYSTEM, null, clickedTracker);
                } else {
                    var shareBtn :DisplayObject = _mctx.getControlBar().shareBtn;
                    BubblePopup.showHelpBubble(_mctx, shareBtn, msg, -7);
                    if (_shareTracker != null) {
                        // only track one at a time, brah
                        shareBtn.removeEventListener(MouseEvent.CLICK, _shareTracker);
                    }
                    _shareTracker = function (event :MouseEvent) :void {
                        clickedTracker(); // we're just adapting
                    };
                    shareBtn.addEventListener(MouseEvent.CLICK, _shareTracker, false, 1);
                }

                // and record that we've shown it
                _mctx.getMsoyClient().trackClientAction(clientAction + "_shown", null);
                _shown[key] = true; // we've shown the key, plug it up for all variations
                break; // we did it
            }
        }
    }

    protected var _mctx :MsoyContext;

    protected var _timer :Timer = new Timer(60 * 1000);

    /** 0 = none, 1 = game, 2 = scene. Doesn't really matter, really, we just track transitions. */
    protected var _locType :int;

    /** Contains keys that have already been shown. */
    protected var _shown :Object = {};

    protected var _clicked :Object = {};

    protected var _shareTracker :Function;
}
}
