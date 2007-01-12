package {

import flash.display.Graphics;

import flash.utils.ByteArray;

import flash.display.Sprite;

import mx.core.MovieClipAsset;

/**
 * Represents something in the world that ships may interact with.
 */
public class Obstacle extends Sprite
{
    /** Constants for types of obstacles. */
    public static const ASTEROID_1 :int = 2;
    public static const ASTEROID_2 :int = 3;
    public static const JUNK :int = 4;
    public static const WALL :int = 6;

    public static const LEFT :int = 0;
    public static const RIGHT :int = 1;
    public static const UP :int = 2;
    public static const DOWN :int = 3;

    public var type :int;

    /** Board-coords. */
    public var bX :Number;
    public var bY :Number;

    public function Obstacle (type :int, x :int, y :int, anim :Boolean) :void
    {
        this.type = type;
        this.x = x * Codes.PIXELS_PER_TILE;
        this.y = y * Codes.PIXELS_PER_TILE;

        bX = x;
        bY = y;

        if (anim) {
            setupGraphics();
        }

    }

    protected function setupGraphics () :void
    {
        var obsMovie :MovieClipAsset = MovieClipAsset(new obstacleAnim);
        obsMovie.gotoAndStop(type);
        addChild(obsMovie);
    }

    /**
     * Get a value for how much bounce ships should get off the obstacle.
     */
    public function getElasticity () :Number
    {
        // TODO: Something different for different obstacles.
        return 0.75;
    }

    /**
     * Unserialize our data from a byte array.
     */
    public function readFrom (bytes :ByteArray) :void
    {
        type = bytes.readInt();
        x = bytes.readInt();
        y = bytes.readInt();
        bX = x / Codes.PIXELS_PER_TILE;
        bY = y / Codes.PIXELS_PER_TILE;

        setupGraphics();
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

    [Embed(source="rsrc/obstacle.swf#obstacles_movie")]
    protected var obstacleAnim :Class;
}
}
