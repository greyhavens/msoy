//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.display.Sprite;

import mx.core.UIComponent;

import caurina.transitions.Tweener;

import com.threerings.util.Util;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.TopPanel;

public class TutorialDirector
{
    public function TutorialDirector (ctx :MsoyContext)
    {
        _ctx = ctx;
        _panel = new TutorialPanel(_ctx, onNextTip, onPanelClose);

        // set the width and height for all time
        _panel.width = 600;
        _panel.height = 120;
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
        // TODO: queue etc.
        popup(new Item(null, text, availableFn, buttonText, buttonFn));
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
        // TODO: queue etc.
        popup(new Item(id, text, availableFn, buttonText, buttonFn));
    }

    public function test () :void
    {
        var test :String = "The quick brown fox jumps over the lazy dog. ";
        test = "1... 2... 3... testing 1... 2... 3... " + test + test;
        queueSuggestion(test, null, "Press Here", function () :void {});
    }

    protected function placeBox () :PlaceBox
    {
        return _ctx.getTopPanel().getPlaceContainer();
    }

    protected function onNextTip () :void
    {
    }

    protected function onPanelClose () :void
    {
        Tweener.addTween(_panel, {y :-_panel.height, time: 0.6, transition: "easeinquart",
            onComplete: Util.adapt(placeBox().removeChild, _panel)});
    }

    protected function popup (item :Item) :void
    {
        Tweener.removeTweens(_panel);

        if (_panel.parent == null) {
            placeBox().addOverlay(_panel, PlaceBox.LAYER_TUTORIAL);
            _panel.x = TopPanel.RIGHT_SIDEBAR_WIDTH;
            _panel.y = -_panel.height;
        }

        _panel.setContent(item.text, item.buttonText, item.buttonFn);

        if (_panel.y != 0) {
            Tweener.addTween(_panel, {y :0, time: 0.6, transition: "easeoutquart"});
        }
    }

    protected var _ctx :MsoyContext;
    protected var _panel :TutorialPanel;
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

    public function isAvailable () :Boolean
    {
        return availableFn == null || availableFn();
    }
}
