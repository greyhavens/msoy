package {

import flash.display.Graphics;

import flash.utils.ByteArray;

/**
 * Represents something in the world that ships may interact with.
 */
public class Obstacle
{
    /** Constants for types of obstacles. */
    public static const WALL :int = 0;

    public static const LEFT :int = 0;
    public static const RIGHT :int = 1;
    public static const UP :int = 2;
    public static const DOWN :int = 3;

    public var type :int;
    public var x :int;
    public var y :int;

    public function Obstacle (type :int, x :int, y :int) :void
    {
        this.type = type;
        this.x = x;
        this.y = y;
    }

    /**
     * Draw the obstacle.
     */
    public function paint (gfx :Graphics) :void
    {
        var sx :int = x * Codes.PIXELS_PER_TILE - StarFight.WIDTH/2;
        var sy :int = y * Codes.PIXELS_PER_TILE - StarFight.HEIGHT/2;
        gfx.beginFill(GREY);
        gfx.drawRect(sx, sy, Codes.PIXELS_PER_TILE, Codes.PIXELS_PER_TILE);
    }

    /**
     * Returns the direction the obstacle lies from the position.
     */
    public function getDir (boardX :Number, boardY :Number) :int
    {
        var dx :Number = x - boardX;
        var dy :Number = y - boardY;

        if (Math.abs(dx) > Math.abs(dy)) {
            if (dx > 0) {
                return RIGHT;
            } else {
                return LEFT;
            }
        } else {
            if (dy > 0) {
                return UP;
            } else {
                return DOWN;
            }
        }
    }

    /**
     * Get a value for how much bounce ships should get off the obstacle.
     */
    public function getElasticity () :Number
    {
        // TODO: Something different for different obstacles.
        return 1.0;
    }

    /**
     * Unserialize our data from a byte array.
     */
    public function readFrom (bytes :ByteArray) :void
    {
        type = bytes.readInt();
        x = bytes.readInt();
        y = bytes.readInt();
    }

    /**
     * Serialize our data to a byte array.
     */
    public function writeTo (bytes :ByteArray) :ByteArray
    {
        bytes.writeInt(type);
        bytes.writeInt(x);
        bytes.writeInt(y);

        return bytes;
    }

    /** Color constants. */
    protected static const GREY :uint = uint(0x808080);
}
}
