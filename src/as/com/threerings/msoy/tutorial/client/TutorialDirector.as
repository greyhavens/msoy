//
// $Id$

package com.threerings.msoy.tutorial.client {

import flash.events.Event;
import flash.events.TimerEvent;
import flash.utils.Timer;

import mx.managers.PopUpManager;

import com.threerings.util.Arrays;
import com.threerings.util.Map;
import com.threerings.util.Maps;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.UIState;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Director for the tutorial. Notionally manages a collection of tutorial items, their display and
 * heuristics.
 */
public class TutorialDirector
{
    /**
     * Whether this is the player's first time on the DJ site.
     */
    public var djTutorial :Boolean = false;

    /**
     * Creates a new director.
     */
    public function TutorialDirector (ctx :WorldContext)
    {
        _ctx = ctx;
        _timer = new Timer(TIP_DELAY, 1);
        _timer.addEventListener(TimerEvent.TIMER, handleTimer);
        _ctx.getUIState().addEventListener(UIState.STATE_CHANGE, handleUIStateChange);
        _panel = new TutorialPanel(onPanelClose);
        _ctx.getChatDirector().registerCommandHandler(Msgs.CHAT, "tut", new TutorialHandler());
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
     * Creates a new builder for a tutorial sequence. Once a sequence is activated, all of its
     * items must be viewed before the "ambient" tutorial proceeds. If any item in the sequence is
     * not available the tutorial will wait and check periodically until it becomes available. The
     * progress within a sequence is stored in <code>Prefs</code> so that a sequence will pick up
     * where it left off in the last session when activated.
     */
    public function newSequence (id :String) :TutorialSequenceBuilder
    {
        return new TutorialSequenceBuilder(id, this);
    }

    /**
     * Queues a previously created item to popup.
     */
    public function queueItem (item :TutorialItem) :void
    {
        var ignored :Boolean = isIgnored(item);
        if (isImmediate(item.kind)) {
            // either show this item now or just ignore it
            if (!ignored && item.isAvailable() && !item.equals(_current) &&
                Arrays.indexOf(_suggestions, item) == -1) {
                _suggestions.push(item);
                update();
            }
        } else {
            // add to pool for later display, mark as seen if it was previously ignored
            // TODO: unfudge: "ignored" is not really the same as "seen"
            var seen :Boolean = ignored;
            _pool.put(item, seen);
            update();
        }
    }

    /**
     * Activates a sequence.
     */
    public function activateSequence (seq :TutorialSequence, dismiss :Boolean) :Boolean
    {
        if (!seq.isAvailable() ||
            (seq.isPersisted() && Prefs.getTutorialProgress(seq.id) >= seq.size())) {
            return false;
        }

        if (_sequence != null) {
            if (dismiss) {
                _panel.handleClose();
                _sequence = null;

            } else {
                return false;
            }
        }

        _sequence = new ActiveSequence(seq);
        update();
        return true;
    }

    /**
     * Gets the level of the logged in member, or -1 if the member is not logged in.
     */
    public function getMemberLevel () :int
    {
        var memObj :MemberObject = _ctx.getMemberObject();
        return memObj == null ? -1 : memObj.level;
    }

    /**
     * Pops up a previosuly queued tip. Displays feedback if the tip could not be popped up, or if
     * some other condition that would normally prevent popping up the tip is being ignored.
     */
    public function testTip (id :String) :void
    {
        var item :TutorialItem = null;
        var seen :Boolean;
        _pool.forEach(function (key :TutorialItem, value :Boolean) :Boolean {
            if (key.id == id) {
                item = key;
                seen = value;
                return true;
            }
            return false;
        });
        function feedback (msg :String) :void {
            _ctx.getChatDirector().displayFeedback(MsoyCodes.NPC_MSGS, msg);
        }
        if (item == null) {
            feedback("m.testtip_not_found");
            return;
        }
        if (isShowing()) {
            feedback("m.testtip_panel_open");
            return;
        }
        if (seen == true) {
            feedback("m.testtip_seen");
        }
        if (isIgnored(item)) {
            feedback("m.testtip_ignored");
        }
        if (!item.isAvailable()) {
            feedback("m.testtip_unavailable");
        }
        popup(item);
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
        return _current != null;
    }

    protected function handleUIStateChange (evt :Event) :void
    {
        if (isShowing() && inGame()) {
            _panel.handleClose();
        } else {
            update();
        }
    }

    protected function handleTimer (evt :TimerEvent) :void
    {
        var item :TutorialItem; // for use in multiple scopes
        if (!isShowing()) {
            if (inGame()) {
                update();

            } else if (_sequence != null) {
                item = _sequence.item;
                if (item == null || !item.isAvailable()) {
                    // degenerate case, the sequence has changed since the cookie was last set
                    // TODO: any special behavior when a sequence item is not available?
                    _sequence = null;
                    update();

                } else {
                    popup(item);
                }

            } else if (_suggestions.length > 0) {
                popup(_suggestions.shift());

            } else {
                // compute the candidate list of unseen tips and shuffle
                var unseen :Array = Maps.filter(_pool, isUnseen, Maps.selectKey);
                Arrays.shuffle(unseen);

                // start from the end and find one that is not ignored and is available
                var changed :Boolean = false;
                while (unseen.length > 0) {
                    item = unseen.pop();
                    if (isIgnored(item)) {
                        // TODO: unfudge: "ignored" is not really the same as "seen"
                        _pool.put(item, true);
                        changed = true;
                    } else if (!item.isAvailable()) {
                        // skip it, it might become available later
                    } else {
                        // show it and bail
                        popup(item);
                        break;
                    }
                }

                // update if any tip changed state
                if (changed) {
                    update();
                }
            }
        }
    }

    protected function onEnterFrame (event :Event) :void
    {
        // Continuously poll isAvailable() if the tutorial should continue to the next step
        if (_current != null && !_current.isAvailable()) {
            event.target.removeEventListener(Event.ENTER_FRAME, onEnterFrame);
            _panel.handleClose();
        }
    }

    protected function update () :void
    {
        var showing :Boolean = isShowing();
        if (showing) {
            if (_current.checkAvailable != null) {
                _ctx.getTopPanel().addEventListener(Event.ENTER_FRAME, onEnterFrame);
            }
            _timer.reset();

        } else {
            var delay :Number = inGame() ? GAME_DELAY :
                ((_suggestions.length > 0 || _sequence != null) ? SUGGESTION_DELAY : TIP_DELAY);
            if (delay != _timer.delay) {
                _timer.delay = delay;
            }
            if (!_timer.running && (
                Maps.some(_pool, isUnseen) || _suggestions.length > 0 || _sequence != null)) {
                _timer.start();
            }
        }

        if (showing) {
            if (_suggestions.length > 0 || (_sequence != null && !_sequence.hasItem(_current))) {
                _panel.flashCloseButton();
            }
        }
    }

    protected function inGame () :Boolean
    {
        return _ctx.getUIState().inGame || _ctx.getUIState().inAVRGame;
    }

    internal function get topPanel () :TopPanel
    {
        return _ctx.getTopPanel();
    }

    internal function get worldCtx () :WorldContext
    {
        return _ctx;
    }

    protected function onPanelClose () :void
    {
        // mark as seen
        if (_sequence != null && _sequence.hasItem(_current)) {
            if (!_sequence.advance()) {
                _sequence = null;
            }

        } else if (!isImmediate(_current.kind)) {
            _pool.put(_current, true);
        }

        _current = null;
        update();
    }

    protected function popup (item :TutorialItem) :void
    {
        if (_panel.parent == null) {
            PopUpManager.addPopUp(_panel, topPanel);
            topPanel.setTutorialPanel(_panel);
        }

        PopUpManager.bringToFront(_panel);

        _panel.setTopMargin(topPanel.getHeaderBarHeight());
        _panel.setContent(_current = item);
        update();
    }

    protected static function isIgnored (item :TutorialItem) :Boolean
    {
        return item.ignorable && Prefs.isTutorialIgnored(item.id);
    }

    protected static function isImmediate (kind :Kind) :Boolean
    {
        return kind == Kind.SUGGESTION;
    }

    protected static function isUnseen (item :TutorialItem, val :Boolean) :Boolean
    {
        return !val;
    }

    protected var _ctx :WorldContext;
    protected var _panel :TutorialPanel;
    protected var _timer :Timer;
    protected var _suggestions :Array = [];
    protected var _pool :Map = Maps.newMapOf(TutorialItem); // to boolean: seen
    protected var _sequence :ActiveSequence;
    protected var _current :TutorialItem;

    protected var TIP_DELAY :Number = (DeploymentConfig.devDeployment ? 1 : 5) * 60 * 1000;
    protected var SUGGESTION_DELAY :Number = (TutorialPanel.ROLL_TIME + .25) * 1000;
    protected var GAME_DELAY :Number = 10 * 60 * 1000;
}
}
