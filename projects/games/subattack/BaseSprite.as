package {

import flash.display.Sprite;

import flash.ui.Keyboard;

/**
 * A base sprite that moves around the sea.
 */
public class BaseSprite extends Sprite
{
    public function BaseSprite (board :Board)
    {
        _board = board;
    }

    public function getX () :int
    {
        return _x;
    }

    public function getY () :int
    {
        return _y;
    }

    public function getOrient () :int
    {
        return _orient;
    }

    /**
     * Advance our location in the direction of our orientation.
     */
    protected function advanceLocation () :Boolean
    {
        switch (_orient) {
        case Keyboard.DOWN:
            if (!_board.isTraversable(_x, _y + 1)) {
                return false;
            }
            _y++;
            break;

        case Keyboard.UP:
            if (!_board.isTraversable(_x, _y - 1)) {
                return false;
            }
            _y--;
            break;

        case Keyboard.LEFT:
            if (!_board.isTraversable(_x - 1, _y)) {
                return false;
            }
            _x--;
            break;

        case Keyboard.RIGHT:
            if (!_board.isTraversable(_x + 1, _y)) {
                return false;
            }
            _x++;
            break;
        }

        updateLocation();
        return true;
    }

    /**
     * Update the location of this sprite in the SeaDisplay.
     */
    protected function updateLocation () :void
    {
        x = _x * SeaDisplay.TILE_SIZE;
        y = _y * SeaDisplay.TILE_SIZE;
    }

    /** The board. */
    protected var _board :Board;

    /** Our logical coordinates. */
    protected var _x :int;
    protected var _y :int;

    /** Our orientation, which is specified using the keyboard codes
     * for UP/LEFT/DOWN/RIGHT... */
    protected var _orient :int;
}
}
