package {

import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.text.TextField;

import flash.ui.Keyboard;

import flash.utils.getTimer; // function import

import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;

import com.whirled.WhirledGameControl;

[SWF(width="416", height="416")]
public class SubAttack extends Sprite
{
    /** How many tiles does our vision extend past our tile? */
    public static const VISION_TILES :int = 6;

    /** How many total tiles are in one direction in the view? */
    public static const VIEW_TILES :int = (VISION_TILES * 2) + 1;

    public function SubAttack ()
    {
        addChild(_seaDisplay = new SeaDisplay());

        var maskSize :int = VIEW_TILES * SeaDisplay.TILE_SIZE;
        var masker :Shape = new Shape();
        masker.graphics.beginFill(0xFFFFFF);
        masker.graphics.drawRect(0, 0, maskSize, maskSize);
        masker.graphics.endFill();
        this.mask = masker;
        addChild(masker); // the mask must be added to the display

        _gameCtrl = new WhirledGameControl(this);
        if (!_gameCtrl.isConnected()) {
            // just show a demo-mode display
            _seaDisplay.setupSea(VIEW_TILES, VIEW_TILES);
            _seaDisplay.setStatus(
                "<P align=\"center\"><font size=\"+2\">Truckyard Shootout</font>" +
                "<br>A fast-action maze-building and shooting game for " +
                "2-8 players.<br>Must be played inside Whirled.</P>");
            var sub :Submarine = new Submarine(0, "Player 1", 3, 3, null);
            _seaDisplay.addChild(sub);
            return;
        }

        _board = new Board(_gameCtrl, _seaDisplay);
        _myIndex = _gameCtrl.seating.getMyPosition();

        if (_myIndex != -1) {
            _gameCtrl.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);

            addEventListener(Event.ENTER_FRAME, enterFrame);
        }
    }

    /**
     * Handles KEY_DOWN.
     */
    protected function keyEvent (event :KeyboardEvent) :void
    {
        var action :int = getActionForKey(event.keyCode);
        switch (action) {
        case Action.NONE:
            break;

        default:
            if (_queued != null) {
                _queued.push(action);

            } else {
                var now :int = getTimer();
                if ((now - _lastSent) < SEND_THROTTLE) {
                    _queued = [ action ];

                } else {
                    _gameCtrl.sendMessage("sub" + _myIndex, [ action ]);
                    _lastSent = now;
                }
            }
            break;
        }
    }

    protected function enterFrame (event :Event) :void
    {
        if (_queued != null) {
            var now :int = getTimer();
            if ((now - _lastSent) >= SEND_THROTTLE) {
                _gameCtrl.sendMessage("sub" + _myIndex, _queued);
                _lastSent = now;
                _queued = null;
            }
        }
    }

    /**
     * Get the action that corresponds to the specified key.
     */
    protected function getActionForKey (keyCode :int) :int
    {
        switch (keyCode) {
        case Keyboard.DOWN:
            return Action.DOWN;

        case Keyboard.UP:
            return Action.UP;

        case Keyboard.RIGHT:
            return Action.RIGHT;

        case Keyboard.LEFT:
            return Action.LEFT;

        case Keyboard.SPACE:
            return Action.SHOOT;

        case Keyboard.ENTER:
            return Action.RESPAWN

        default:
            return Action.NONE;
        }
    }

    /** The game control. */
    protected var _gameCtrl :WhirledGameControl;

    /** Represents our board. */
    protected var _board :Board;

    /** The visual display of the game. */
    protected var _seaDisplay :SeaDisplay;

    /** Our player index, or -1 if we're not a player. */
    protected var _myIndex :int;

    /** The time at which we last sent our actions. */
    protected var _lastSent :int = 0;

    /** The actions we have queued to be sent. */
    protected var _queued :Array;

    protected static const SEND_THROTTLE :int = 105;
}
}
