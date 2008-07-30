package com.threerings.msoy.ui {

import caurina.transitions.Tweener;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.MultiLoader;

import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.MovieClip;
import flash.display.Sprite;
import flash.events.Event;
import flash.text.TextField;

public class AwardPanel
{
    public static const log :Log = Log.getLog(AwardPanel);

    public function AwardPanel (wctx :WorldContext, chatDirector :ChatDirector = null)
    {
        _wctx = wctx;
        _chatDirector = chatDirector;
    }

    public function close () :void
    {
        if (_panel.parent != null) {
            _wctx.getTopPanel().getPlaceContainer().removeOverlay(_panel);
        }
    }

    public function displayAward (award :Object) :void
    {
        _pendingAwards.push(award);
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
        log.info("showNextAward");

        var msg :String, name :String, title :String;
        var media :MediaDesc;
        if (award is Trophy) {
            var trophy :Trophy = (award as Trophy);
            msg = MessageBundle.tcompose("m.trophy_earned", trophy.name);
            name = trophy.name;
            title = "m.trophy_title";
            media = trophy.trophyMedia;

        } else if (award is Item) {
            var item :Item = (award as Item);
            msg = MessageBundle.tcompose("m.prize_earned", item.name);
            name = item.name;
            title = "m.prize_title";
            media = item.getThumbnailMedia();

        } else {
            log.warning("Requested to display unknown award " + award + ".");
            checkPendingAwards();
            return;
        }

        // display a chat message reporting their award
        if (_chatDirector != null) {
            _chatDirector.displayInfo(MsoyCodes.GAME_MSGS, msg);
        }

        // configure the award display panel with the award info
        (_panel.getChildByName("statement") as TextField).text =
            _wctx.xlate(MsoyCodes.GAME_MSGS, title);
        (_panel.getChildByName("trophy_name") as TextField).text = name;
        var clip :MovieClip = (_panel.getChildByName("trophy") as MovieClip);
        while (clip.numChildren > 0) { // remove any old trophy image or the sample
            clip.removeChildAt(0);
        }
        var image :MsoyMediaContainer = new MsoyMediaContainer(media);
        clip.addChild(image);

        // add ourselves to the stage now so that the logic that checks if it's used will
        // be able to detect its state properly
        _panel.x = 250;
        _panel.y = -_panel.height;
        var container :PlaceBox = _wctx.getTopPanel().getPlaceContainer();
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

    protected var _chatDirector :ChatDirector;

    /** The award display movie. */
    protected var _panel :DisplayObjectContainer;

    /** Awards waiting to be displayed. Either Trophy or Item. */
    protected var _pendingAwards :Array = [];

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
