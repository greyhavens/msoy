//
// $Id$

package com.threerings.msoy.client {

import flash.events.TimerEvent;
import flash.utils.Timer;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.RandomUtil;
import com.threerings.util.Util;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.ClientEvent;

import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.data.Embedding;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.PlaceInfo;
import com.threerings.msoy.ui.BubblePopup;

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
        }
    }

    /**
     * Hack. This is called by MsoyController. God dammit.
     */
    public function locationUpdated () :void
    {
        var placeInfo :PlaceInfo = _mctx.getMsoyController().getPlaceInfo();
        var newLocType :int = (placeInfo.gameId != 0) ? 1 : ((placeInfo.sceneId != 0) ? 2 : 0);
        if (_locType != newLocType) {
            _locType = newLocType;
            _timer.reset();
            if (_locType != 0) {
                _timer.start();
            }
        }
    }

    /**
     * Called when a single-player game ends.
     */
    public function noteGameOver () :void
    {
        handleTimer(null);
    }

    /**
     * Handle timer expiration.
     */
    protected function handleTimer (event :TimerEvent) :void
    {
        // disable for facebook
        // TODO: consider some upsells that might make sense for facebook mode
        if (_mctx.getMsoyClient().getEmbedding() == Embedding.FACEBOOK) {
            return;
        }

        if (_mctx.getMemberObject() == null) {
            return;
        }

        var placeInfo :PlaceInfo = _mctx.getMsoyController().getPlaceInfo();
        var embed :String = _mctx.getMsoyClient().isEmbedded() ? "embed" : "site";
        var guest :String = (_mctx.getMemberObject()).isPermaguest() ? "guest" : "mem";
        var place :String = placeInfo.inGame ? "game" : "room";
        var mins :String = (event == null) ? "-1" : String(_timer.currentCount);

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
                Util.keys(Msgs.GENERAL.getAllMapped(key))) as String;
            if (actualKey != null) {
                // we're doing it, we're actually showing something!
                var msg :String = Msgs.GENERAL.get(
                    actualKey, String(placeInfo.name), String(placeInfo.id));
                if (isNotif) {
                    _mctx.getNotificationDirector().addGenericNotification(
                        MessageBundle.taint(msg), Notification.SYSTEM, null, null);
                } else {
                    BubblePopup.showHelpBubble(_mctx, _mctx.getControlBar().shareBtn, msg, -7);
                }
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
}
}
