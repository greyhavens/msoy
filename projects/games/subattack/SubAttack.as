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

        } else if (name == "subs") {
            var pos :int;
            var submarine :Submarine;
            if (index == -1) {
                var subs :Array = (_gameObj.get("subs") as Array);
                for (var ii :int = 0; ii < subs.length; ii++) {
                    submarine = new Submarine(ii);
                    pos = int(subs[ii]);
                    submarine.x = _board.idxToX(pos) * TILE_SIZE;
                    submarine.y = _board.idxToY(pos) * TILE_SIZE;
                    addChild(submarine);
                    _subs[ii] = submarine;
                }

            } else {
                // update the sub to its new location
                pos = int(event.newValue);
                submarine = (_subs[index] as Submarine);
                // update position
                submarine.x = _board.idxToX(pos) * TILE_SIZE;
                submarine.y = _board.idxToY(pos) * TILE_SIZE;

//                if (index == _myIndex) {
//                    _canMoveNextTick = true;
//                }
            }
        }
    }

    // from MessageReceivedListener
    public function messageReceived (event :MessageReceivedEvent) :void
    {
        var name :String = event.name;
        if (name == "tick") {
            _canMove = true;
        }
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        if (!_canMove) {
            return;
        }
        var idx :int = int(_gameObj.get("subs", _myIndex));
        var xx :int = _board.idxToX(idx);
        var yy :int = _board.idxToY(idx);

        switch (event.keyCode) {
        case Keyboard.DOWN:
            if (yy == Board.SIZE - 1) {
                return;
            }
            yy++;
            break;

        case Keyboard.UP:
            if (yy == 0) {
                return;
            }
            yy--;
            break;

        case Keyboard.RIGHT:
            if (xx == Board.SIZE - 1) {
                return;
            }
            xx++;
            break;

        case Keyboard.LEFT:
            if (xx == 0) {
                return;
            }
            xx--;
            break;

        default:
            return;
        }

        // attempt to set our new position
        _gameObj.set("subs", _board.coordsToIdx(xx, yy), _myIndex);
        _canMove = false;
    }

    /** The game object. */
    protected var _gameObj :EZGame;

    /** Represents our board. */
    protected var _board :Board;

    protected var _myIndex :int;

    protected var _seaweed :Array = [];

    protected var _subs :Array = [];

    protected var _canMove :Boolean = false;
//    protected var _canMoveNextTick :Boolean = true;
//    protected var _nextMove :Object;

    /** How many tiles in any direction can we see? */
    protected static const VISION_DISTANCE :int = 5;
}
}
