//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.display.Sprite;
import flash.events.TimerEvent;
import flash.utils.Timer;

import mx.core.UIComponent;

import caurina.transitions.Tweener;

import com.threerings.util.Util;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.TopPanel;

import flash.utils.setTimeout; // function import
import flash.utils.getTimer; // function import

/**
 * Director for the tutorial. Notionally manages a collection of tutorial items, their display and
 * heuristics.
 */
public class TutorialDirector
{
    /**
     * Creates a new director.
     */
    public function TutorialDirector (ctx :MsoyContext)
    {
        _ctx = ctx;
        _timer = new Timer(TIP_DELAY, 1);
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);

        _panel = new TutorialPanel(_ctx, onIgnore, onPanelClose);
    }

    /**
     * Creates a new builder for a suggestion with the given id and text. When queued, the
     * suggestion is shown immediately unless it is not appropriate to do so.
     */
    public function newSuggestion (id :String, text :String) :TutorialItemBuilder
    {
        return newItem(Kind.SUGGESTION, id, text);
    }

    /**
     * Creates a new builder for a tip with the given id and text. When queued, the tip is added to
     * the end of the tip list. It gets shown later once all preceding tips have been shown unless
     * it is not appropriate to do so at that time.
     */
    public function newTip (id :String, text :String) :TutorialItemBuilder
    {
        return newItem(Kind.TIP, id, text);
    }

    /**
     * Creates a new builder for a promption item with the given id and text. Currently promotions
     * are treated like tips. The plan is to make them non-ignorable. They may also get shown at a
     * different frequency.
     */
    public function newPromotion (id :String, text :String) :TutorialItemBuilder
    {
        return newItem(Kind.PROMOTION, id, text);
    }

    /**
     * Queues a previously created item to popup.
     */
    public function queueItem (item :TutorialItem) :void
    {
        queue(item);
    }

    public function test (delayMultiplier :Number) :void
    {
        var gibberish :String = "The quick brown fox jumped over the lazy dog.";
        gibberish = gibberish + " " + gibberish;
        if (_tips.length == 0) {
            newTip("test1", "This is test tip #1. " + gibberish).queue();
            newTip("test2", "This is test tip #2. " + gibberish).queue();
            newTip("test3", "This is test tip #3. " + gibberish).queue();
            newTip("test4", "This is test tip #4. " + gibberish).queue();
            _ctx.getChatDirector().displayFeedback(null, "Test: added 4 tips.");
        }

        var delay :Number = TIP_DELAY + (Math.random() - .5) * TIP_DELAY * .5;
        delay *= delayMultiplier;
        var id :int = getTimer();
        setTimeout(function () :void {
            newSuggestion("test" + id, "This is a test suggestion (id " + id + "). " + gibberish).
                button("Take me somewhere", function () :void {}).queue();
        }, delay);

        _ctx.getChatDirector().displayFeedback(null, "Test: queued suggestion id " + id +
            " for display in " + int(delay / 1000) + " seconds.");
    }

    /**
     * Creates a new builder for an item of the given kind with the given id and text.
     */
    protected function newItem (kind :Kind, id :String, text :String) :TutorialItemBuilder
    {
        return new TutorialItemBuilder(new TutorialItem(kind, id, text), this);
    }

    protected function isShowing () :Boolean
    {
        return _panel.parent != null;
    }

    protected function handleTimer (evt :TimerEvent) :void
    {
        if (!isShowing()) {
            if (_suggestions.length > 0) {
                popup(_suggestions.shift());

            } else if (_tips.length > 0) {
                _lastTip = (_lastTip + 1) % _tips.length;
                popup(_tips[_lastTip]);
            }
        }
    }

    protected function update () :void
    {
        var showing :Boolean = isShowing();
        if (showing) {
            _timer.reset();

        } else {
            _timer.delay = _suggestions.length > 0 ? SUGGESTION_DELAY : TIP_DELAY;
            if (!_timer.running && (_tips.length > 0 || _suggestions.length > 0)) {
                _timer.start();
            }
        }

        if (isShowing() && _suggestions.length > 0) {
            _panel.flashCloseButton();
        }
    }

    protected function placeBox () :PlaceBox
    {
        return _ctx.getTopPanel().getPlaceContainer();
    }

    protected function onPanelClose () :void
    {
        Tweener.addTween(_panel, {y :-_panel.height, time: ROLL_TIME, transition: "easeinquart",
            onComplete: Util.sequence(
                Util.adapt(placeBox().removeChild, _panel),
                Util.adapt(update))});
    }

    protected function onIgnore () :void
    {
    }

    protected function queue (item :TutorialItem) :void
    {
        if (item.kind == Kind.SUGGESTION) {
            _suggestions.push(item);
            update();

        } else {
            _tips.push(item);
            update();
        }
    }

    protected function popup (item :TutorialItem) :void
    {
        Tweener.removeTweens(_panel);

        if (!isShowing()) {
            placeBox().addOverlay(_panel, PlaceBox.LAYER_TUTORIAL);
            _panel.y = -_panel.height;
        }

        _panel.setContent(item.text, item.buttonText, item.onClick);

        if (_panel.y != 0) {
            Tweener.addTween(_panel, {y :0, time: ROLL_TIME, transition: "easeoutquart"});
        }

        update();
    }

    protected var _ctx :MsoyContext;
    protected var _panel :TutorialPanel;
    protected var _timer :Timer;
    protected var _suggestions :Array = [];
    protected var _tips :Array = [];
    protected var _lastTip :int = -1;

    protected var ROLL_TIME :Number = 0.6;
    protected var TIP_DELAY :Number = 60 * 1000;
    protected var SUGGESTION_DELAY :Number = (ROLL_TIME + .25) * 1000;
}
}
