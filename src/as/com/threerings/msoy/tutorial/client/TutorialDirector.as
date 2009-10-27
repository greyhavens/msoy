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
 * Director for the tutorial popup panel. Manages two kinds of tutorial items: tips and
 * suggestions. Tips are normally added during intialization and shown at various times when
 * requested or after some time where no suggestions are happening. Suggestions are context-
 * dependent and popup immediately if the panel is not showing. Otherwise, the next button flashes
 * and the suggestion is next in line.
 */
public class TutorialDirector
{
    public function TutorialDirector (ctx :MsoyContext)
    {
        _ctx = ctx;
        _timer = new Timer(TIP_DELAY, 1);
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);

        _panel = new TutorialPanel(_ctx, onNextTip, onPanelClose);
    }

    /**
     * Queues a suggestion to popup in the current context. The duration of the context is
     * determined by the given availability function. If the tutorial panel is not showing,
     * pops up immediately. If the suggestion is no longer available when it is due to come up,
     * it is discarded.
     *
     * @param text the text content of the hint, for example "hey, you can publish your room!"
     * @param availableFn the predicate to test if the suggestion should still be available, for
     *        example the room publishing suggestion should not popup if the user has gone into a
     *        game. If null, then the suggestion is always available.
     *        <listing>
     *            function availableFn () :Boolean;
     *        </listing>
     * @param buttonText the text of the button, for example "publish now", or null if
     *        there is no applicable button
     * @param buttonFn the function to call when the button is pressed, or null if there is no
     *        applicable button
     *        <listing>
     *            function buttonFn () :void;
     *        </listing>
     */
    public function queueSuggestion (text :String, availableFn :Function, buttonText :String,
                                     buttonFn :Function) :void
    {
        queue(new Item(null, text, availableFn, buttonText, buttonFn));
    }

    /**
     * Queues a tip to popup later when there has not been a suggestion for a while. Typically, all
     * tips are added during client initialization and lie in wait to be shown in order of
     * addition. We also store a cookie whenever a tip if shown so that the list can be resumed at
     * the first unseen tip when the user logs in again.
     *
     * @param id the id of the tip so that all tips can be cycled through in order
     * @param text the text content of the tip, for example "hey, you can buy more rooms"
     * @param availableFn the predicate to test if the hint should still be available, for example
     *        the room buying may want to suppress if an AVRG is active
     *        <listing>
     *            function availableFn () :Boolean;
     *        </listing>
     * @param buttonText the text content of the button, for example "publish now", or null if
     *        there is no applicable button
     * @param buttonFn the function to call when the button is pressed, or null if there is no
     *        applicable button
     *        <listing>
     *            function buttonFn () :void;
     *        </listing>
     */
    public function queueTip (id :String, text :String, availableFn :Function, buttonText :String,
                              buttonFn :Function) :void
    {
        queue(new Item(id, text, availableFn, buttonText, buttonFn));
    }

    public function test (delayMultiplier :Number) :void
    {
        var gibberish :String = "The quick brown fox jumped over the lazy dog.";
        gibberish = gibberish + " " + gibberish;
        if (_tips.length == 0) {
            queueTip("test1", "This is test tip #1. " + gibberish, null, null, null);
            queueTip("test2", "This is test tip #2. " + gibberish, null, null, null);
            queueTip("test3", "This is test tip #3. " + gibberish, null, null, null);
            queueTip("test4", "This is test tip #4. " + gibberish, null, null, null);
            _ctx.getChatDirector().displayFeedback(null, "Test: added 4 tips.");
        }

        var delay :Number = TIP_DELAY + (Math.random() - .5) * TIP_DELAY * .5;
        delay *= delayMultiplier;
        var id :int = getTimer();
        setTimeout(function () :void {
            queueSuggestion("This is a test suggestion (id " + id + "). " + gibberish, null,
                            "Take me somewhere", function () :void {});
        }, delay);

        _ctx.getChatDirector().displayFeedback(null, "Test: queued suggestion id " + id +
            " for display in " + int(delay / 1000) + " seconds.");
    }

    protected function isShowing () :Boolean
    {
        return _panel.parent != null;
    }

    protected function handleTimer (evt :TimerEvent) :void
    {
        if (!isShowing()) {
            onNextTip();
        }
    }

    protected function placeBox () :PlaceBox
    {
        return _ctx.getTopPanel().getPlaceContainer();
    }

    protected function onNextTip () :void
    {
        if (_suggestions.length > 0) {
            popup(_suggestions.shift());
            if (_suggestions.length > 0) {
                _panel.flashNext();
            }

        } else if (_tips.length > 0) {
            _lastTip = (_lastTip + 1) % _tips.length;
            popup(_tips[_lastTip]);
        }
    }

    protected function onPanelClose () :void
    {
        Tweener.addTween(_panel, {y :-_panel.height, time: 0.6, transition: "easeinquart",
            onComplete: Util.adapt(placeBox().removeChild, _panel)});

        _timer.start();
    }

    protected function queue (item :Item) :void
    {
        if (item.isSuggestion()) {
            if (isShowing()) {
                _suggestions.push(item);
                _panel.flashNext();
            } else {
                popup(item);
            }
        } else {
            _tips.push(item);
            if (!isShowing() && !_timer.running) {
                _timer.start();
            }
        }
    }

    protected function popup (item :Item) :void
    {
        Tweener.removeTweens(_panel);

        if (!isShowing()) {
            placeBox().addOverlay(_panel, PlaceBox.LAYER_TUTORIAL);
            _panel.x = TopPanel.RIGHT_SIDEBAR_WIDTH;
            _panel.y = -_panel.height;
        }

        _panel.setContent(item.text, item.buttonText, item.buttonFn);

        if (_panel.y != 0) {
            Tweener.addTween(_panel, {y :0, time: 0.6, transition: "easeoutquart"});
        }

        _timer.reset();
    }

    protected var _ctx :MsoyContext;
    protected var _panel :TutorialPanel;
    protected var _timer :Timer;
    protected var _suggestions :Array = [];
    protected var _tips :Array = [];
    protected var _lastTip :int = -1;

    protected var TIP_DELAY :Number = 60 * 1000;
}
}

/**
 * Encapsulates the data associated with a tutorial item.
 */
class Item
{
    public var id :String;
    public var text :String;
    public var availableFn :Function;
    public var buttonText :String;
    public var buttonFn :Function;

    public function Item (id :String, text :String, availableFn :Function, buttonText :String,
                          buttonFn :Function)
    {
        this.id = id;
        this.text = text;
        this.availableFn = availableFn;
        this.buttonText = buttonText;
        this.buttonFn = buttonFn;
    }

    public function isSuggestion () :Boolean
    {
        return id == null;
    }

    public function isAvailable () :Boolean
    {
        return availableFn == null || availableFn();
    }
}
