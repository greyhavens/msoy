package {

import flash.display.Sprite;

import flash.ui.Keyboard;

import com.threerings.ezgame.EZGame;

public class Submarine extends Sprite
{
    public function Submarine (
        playerIdx :int, playerName :String, startx :int, starty :int,
        board :Board)
    {
        _playerIdx = playerIdx;
        _playerName = playerName;
        _x = startx;
        _y = starty;
        _orient = (_x == 0) ? Keyboard.RIGHT : Keyboard.LEFT;
        _board = board;

        updateVisual();
        updateLocation();
    }

    /**
     * Perform the action specified by the keycode, or return false
     * if unable.
     */
    public function performAction (keyCode :int) :Boolean
    {
        if (keyCode == Keyboard.SPACE) {
            // TODO: shooting!
            return false;
        }

        // otherwise, it's a move request

        // we can always re-orient
        if (keyCode != _orient) {
            _orient = keyCode;
            updateVisual();
            return true;

        } else if (_moved) {
            // but we can't move twice in the same tick
            return false;
        }

        switch (keyCode) {
        case Keyboard.DOWN:
            if (_y >= Board.SIZE - 1) {
                return false;
            }
            _y++;
            break;

        case Keyboard.UP:
            if (_y <= 0) {
                return false;
            }
            _y--;
            break;

        case Keyboard.LEFT:
            if (_x <= 0) {
                return false;
            }
            _x--;
            break;

        case Keyboard.RIGHT:
            if (_x >= Board.SIZE - 1) {
                return false;
            }
            _x++;
            break;
        }

        updateLocation();
        _moved = true;
        return true;
    }

    public function resetMoveCounter () :void
    {
        _moved = false;
    }

    protected function updateLocation () :void
    {
        x = _x * SubAttack.TILE_SIZE;
        y = _y * SubAttack.TILE_SIZE;
    }

    protected function updateVisual () :void
    {
        // draw the circle
        graphics.lineStyle(2, 0x000000);
        graphics.beginFill((_playerIdx == 0) ? 0xFFFF00 : 0x00FFFF);
        graphics.drawCircle(SubAttack.TILE_SIZE / 2, SubAttack.TILE_SIZE / 2,
            SubAttack.TILE_SIZE / 2);

        // draw our orientation
        var xx :int = SubAttack.TILE_SIZE / 2;
        var yy :int = xx;
        graphics.moveTo(xx, yy);
        switch (_orient) {
        case Keyboard.UP:
            yy = 0;
            break;

        case Keyboard.DOWN:
            yy = SubAttack.TILE_SIZE;
            break;

        case Keyboard.LEFT:
            xx = 0;
            break;

        case Keyboard.RIGHT:
            xx = SubAttack.TILE_SIZE;
            break;
        }
        graphics.lineTo(xx, yy);
    }

    /** Our logical x coordinate. */
    protected var _x :int;

    /** Our logical y coordinate. */
    protected var _y :int;

    /** The number of kills we've had. */
    protected var _kills :int;

    /** The number of times we've been killed. */
    protected var _deaths :int;

    /** The orientation of our sub, this is actually set to the keyboard
     * code for UP/LEFT/DOWN/RIGHT... */
    protected var _orient :int;

    /** Have we moved this tick yet? */
    protected var _moved :Boolean;

    /** The player index that this submarine corresponds to. */
    protected var _playerIdx :int;

    /** The name of the player controlling this sub. */
    protected var _playerName :String;

    /** A reference to the board that gives us the love. */
    protected var _board :Board;
}
}
