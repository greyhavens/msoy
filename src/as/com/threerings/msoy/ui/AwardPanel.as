//
// $Id$

package com.threerings.msoy.ui {

import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.MovieClip;
import flash.display.Sprite;

import flash.events.Event;

import flash.text.TextField;

import caurina.transitions.Tweener;

import com.threerings.flash.MediaContainer;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.MultiLoader;

import com.threerings.msoy.badge.data.all.Badge;

import com.threerings.msoy.client.PlaceBox;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MediaDesc;

import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.world.client.WorldContext;

public class AwardPanel
{
    public function AwardPanel (wctx :WorldContext)
    {
        _wctx = wctx;
    }

    /**
     * Removes the AwardPanel from the screen, if it's being displayed.
     */
    public function close () :void
    {
        if (_panel.parent != null) {
            _wctx.getTopPanel().getPlaceContainer().removeOverlay(_panel);
        }
    }

    /**
     * Displays
     * @param award a Trophy, Item, or Badge
     */
    public function displayAward (award :Object) :void
    {
        _pendingAwards.unshift(award);
        checkPendingAwards();
    }

    protected function checkPendingAwards () :void
    {
        // if we haven't yet loaded our trophy panel, do that
        if (_panel == null) {
            _panel = LOADING;
            MultiLoader.getContents(AWARD_PANEL, function (result :DisplayObjectContainer) :void {
                _panel = result;
                checkPendingAwards();
            });

        } else if (_panel == LOADING || _panel.stage != null || _pendingAwards.length == 0) {
            // we're loading the award panel or it's being used or we're done

        } else {
            // otherwise pop the next award from the list and display it
            showNextAward(_pendingAwards.pop());
        }
    }

    protected function showNextAward (award :Object) :void
    {
        var msg :String, name :String, title :String;
        var messageBundle :String;
        var mediaDesc :MediaDesc;
        var mediaUrl :String;
        if (award is Trophy) {
            var trophy :Trophy = (award as Trophy);
            msg = MessageBundle.tcompose("m.trophy_earned", trophy.name);
            name = trophy.name;
            title = _wctx.xlate(MsoyCodes.GAME_MSGS, "m.trophy_title");
            mediaDesc = trophy.trophyMedia;
            messageBundle = MsoyCodes.GAME_MSGS;

        } else if (award is Item) {
            var item :Item = (award as Item);
            msg = MessageBundle.tcompose("m.prize_earned", item.name);
            name = item.name;
            title = _wctx.xlate(MsoyCodes.GAME_MSGS, "m.prize_title");
            mediaDesc = item.getThumbnailMedia();
            messageBundle = MsoyCodes.GAME_MSGS;

        } else if (award is Badge) {
            var badge :Badge = (award as Badge);
            var level :String = 
                _wctx.xlate(MsoyCodes.PASSPORT_MSGS, "m.badge_level_" + badge.level);
            name = _wctx.xlate(MsoyCodes.PASSPORT_MSGS, badge.nameProp, level);
            msg = MessageBundle.tcompose("m.badge_awarded", name, badge.coinValue);
            title = _wctx.xlate(MsoyCodes.PASSPORT_MSGS, "t.badge_awarded");
            mediaUrl = badge.imageUrl;
            messageBundle = MsoyCodes.PASSPORT_MSGS;

        } else {
            log.warning("Requested to display unknown award " + award + ".");
            checkPendingAwards();
            return;
        }

        // display a chat message reporting their award
        _wctx.getChatDirector().displayInfo(messageBundle, msg);

        // configure the award display panel with the award info
        (_panel.getChildByName("statement") as TextField).text = title;
        (_panel.getChildByName("trophy_name") as TextField).text = name;
        var clip :MovieClip = (_panel.getChildByName("trophy") as MovieClip);
        while (clip.numChildren > 0) { // remove any old trophy image or the sample
            clip.removeChildAt(0);
        }
        // load the appropriate image from either a MediaDesc or a URL
        var image :MediaContainer = (mediaDesc != null ? new MsoyMediaContainer(mediaDesc) :
            new MediaContainer(mediaUrl));
        clip.addChild(image);

        // add ourselves to the stage now so that the logic that checks if it's used will
        // be able to detect its state properly
        var container :PlaceBox = _wctx.getTopPanel().getPlaceContainer();
        if (container.width < _panel.width * 0.9) {
            // if the place box is too small to show at least 90% of the award panel, don't
            // show it all.  Also, none of the other pending awards will get shown, so we
            // can clear them out too.
            _pendingAwards = [];
            return;
        }
        _panel.x = (container.width - _panel.width) / 2;
        _panel.y = -_panel.height;
        container.addOverlay(_panel, PlaceBox.LAYER_TRANSIENT);

        // wait for the award image to load
        var linfo :LoaderInfo = (image.getMedia() as Loader).contentLoaderInfo;
        linfo.addEventListener(Event.COMPLETE, function (event :Event) :void {
            // center the award image
            image.x -= image.getContentWidth() * 0.5;
            image.y -= image.getContentHeight() * 0.5;
            Tweener.addTween(_panel, {y: 0, time: 0.75, transition: EASING_OUT});
            Tweener.addTween(_panel,
                {y: -_panel.height, time: 0.5, delay: 3, transition: EASING_IN,
                    onComplete: function () :void {
                        container.removeOverlay(_panel);
                        checkPendingAwards();
                    }
                });
        });
    }

    /** Provides access to main client services. */
    protected var _wctx :WorldContext;

    /** The award display movie. */
    protected var _panel :DisplayObjectContainer;

    /** Awards waiting to be displayed. Either Trophy or Item. */
    protected var _pendingAwards :Array = [];

    protected static const log :Log = Log.getLog(AwardPanel);

    /** Used to note that we're loading an embedded SWF. */
    protected static const LOADING :Sprite = new Sprite();

    /** The Tweener easing functions used for our award and guest coin displays */
    protected static const EASING_OUT :String = "easeoutbounce";
    protected static const EASING_IN :String = "easeoutcubic";

    [Embed(source="../../../../../../rsrc/media/award_panel.swf",
           mimeType="application/octet-stream")]
    protected static const AWARD_PANEL :Class;
}

}
