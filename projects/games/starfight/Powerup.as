package {

import flash.display.Sprite;
import mx.core.MovieClipAsset;

import flash.utils.ByteArray;

public class Powerup extends Sprite
{
    public static const SHIELDS :int = 1;
    public static const SPEED :int = 2;
    public static const SPREAD :int = 3;

    public var type :int;
    public var boardX :int;
    public var boardY :int;

    public function Powerup (type :int, boardX :int, boardY :int) :void
    {
        this.type = type;
        this.boardX = boardX;
        this.boardY = boardY;

        x = boardX * Codes.PIXELS_PER_TILE;
        y = boardY * Codes.PIXELS_PER_TILE;

        setupGraphics();
    }

    protected function setupGraphics () :void
    {
        var powMovie :MovieClipAsset = MovieClipAsset(new powerupAnim());
        powMovie.gotoAndStop(type);
        addChild(powMovie);
    }

    /**
     * Unserialize our data from a byte array.
     */
    public function readFrom (bytes :ByteArray) :void
    {
        type = bytes.readInt();
        x = bytes.readInt();
        y = bytes.readInt();
        boardX = x / Codes.PIXELS_PER_TILE;
        boardY = y / Codes.PIXELS_PER_TILE; 

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

    [Embed(source="rsrc/powerups.swf#power_ups_movie")]
    protected var powerupAnim :Class;
}
}
