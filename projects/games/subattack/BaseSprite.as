package {

import flash.display.Sprite;

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
     * Return the distance from this sub to the specified coordinate.
     */
    public function distance (xx :int, yy :int) :Number
    {
        var dx :Number = xx - _x;
        var dy :Number = yy - _y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    /**
     * Advance our location in the direction of our orientation.
     */
    protected function advanceLocation () :Boolean
    {
        var newX :int = advancedX();
        var newY :int = advancedY();
        if (!_board.isTraversable(newX, newY)) {
            return false;

        } else {
            _x = newX;
            _y = newY;
            updateLocation();
            return true;
        }
    }

    protected function advancedX () :int
    {
        switch (_orient) {
        default:
            return _x;

        case Action.LEFT:
            return (_x - 1);

        case Action.RIGHT:
            return (_x + 1);
        }
    }

    protected function advancedY () :int
    {
        switch (_orient) {
        default:
            return _y;

        case Action.DOWN:
            return (_y + 1);

        case Action.UP:
            return (_y - 1);
        }
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

    /** Our orientation, which is specified using the direction action codes. */
    protected var _orient :int;
}
}
