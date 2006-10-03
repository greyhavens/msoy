package {

import flash.display.Graphics;
import flash.display.Sprite;

import flash.events.KeyboardEvent;
import flash.events.MouseEvent;

import flash.ui.Keyboard;

import com.threerings.ezgame.Game;
import com.threerings.ezgame.EZGame;
import com.threerings.ezgame.PropertyChangedEvent;
import com.threerings.ezgame.PropertyChangedListener;
import com.threerings.ezgame.StateChangedEvent;
import com.threerings.ezgame.StateChangedListener;
import com.threerings.ezgame.MessageReceivedEvent;
import com.threerings.ezgame.MessageReceivedListener;

[SWF(width="400", height="400")]
public class SubAttack extends Sprite
    implements Game, PropertyChangedListener, MessageReceivedListener
{
    /** The size of a tile. */
    public static const TILE_SIZE :int = 24;

    public function SubAttack ()
    {
        graphics.beginFill(0x009999);
        graphics.drawRect(0, 0, 400, 400);
    }

    // from Game
    public function setGameObject (gameObj :EZGame) :void
    {
        _gameObj = gameObj;
        _board = new Board(gameObj);
        _myIndex = _gameObj.getMyIndex();

        if (_myIndex != -1) {
            stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        }

        // create a submarine for each player
        var names :Array = gameObj.getPlayerNames();
        var xx :int;
        var yy :int;
        for (var ii :int = 0; ii < names.length; ii++) {
            switch (ii) {
            default:
                trace("Cannot yet handle " + (ii + 1) + " player games!");
                // fall through to 0

            case 0:
                xx = 0;
                yy = 0;
                break;

            case 1:
                xx = Board.SIZE - 1;
                yy = Board.SIZE - 1;
                break;
            }

            var sub :Submarine = new Submarine(
                ii, String(names[ii]), xx, yy, _board);
            addChild(sub);
            _subs[ii] = sub;
        }
    }

    // from PropertyChangedListener
    public function propertyChanged (event :PropertyChangedEvent) :void
    {
        var name :String = event.name;
        var index :int = event.index;
        if (name == "board") {
            if (index == -1) {
                // the board is first populated
                for (var yy :int = 0; yy < Board.SIZE; yy++) {
                    for (var xx :int = 0; xx < Board.SIZE; xx++) {
                        if (!_board.isTraversable(xx, yy)) {
                            var seaweed :Seaweed;
                            seaweed = new Seaweed();
                            seaweed.x = xx * TILE_SIZE;
                            seaweed.y = yy * TILE_SIZE;
                            addChild(seaweed);
                            _seaweed[_board.coordsToIdx(xx, yy)] = seaweed;
                        }
                    }
                }

            } else {
                // TODO: modify a board element
            }
        }
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
        var name :String = event.name;
        if (name == "tick") {
            // clear out the move counts
            for each (var sub :Submarine in _subs) {
                sub.resetMoveCounter();
            }

        } else if (name.indexOf("sub") == 0) {
            var subIndex :int = int(name.substring(3));
            var moveResult :Boolean = Submarine(_subs[subIndex]).performAction(
                int(event.value));
            if (!moveResult) {
                trace("Dropped action: " + name);
            }
        }
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        switch (event.keyCode) {
        case Keyboard.DOWN:
        case Keyboard.UP:
        case Keyboard.RIGHT:
        case Keyboard.LEFT:
        case Keyboard.SPACE:
            _gameObj.sendMessage("sub" + _myIndex, event.keyCode);
            break;

        default:
            return;
        }
    }

    /** The game object. */
    protected var _gameObj :EZGame;

    /** Represents our board. */
    protected var _board :Board;

    protected var _myIndex :int;

    protected var _seaweed :Array = [];

    protected var _subs :Array = [];

    /** How many tiles in any direction can we see? */
    protected static const VISION_DISTANCE :int = 5;
}
}
