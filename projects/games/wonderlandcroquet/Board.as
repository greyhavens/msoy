package {

import flash.utils.ByteArray;

/**
 * Our game board.
 */
public class Board
{
    /** Dimensions of our board. */
    public var width :int;
    public var height :int;

    /**
     * Constructs a new board.
     */
    public function Board (width :int, height :int)
    {
        this.width = width;
        this.height = height;
    }

    /**
     * Unserialize our data from a byte array.
     */
    public function readFrom (bytes :ByteArray) :void
    {
        width = bytes.readInt();
        height = bytes.readInt();
    }

    /**
     * Serialize our data to a byte array.
     */
    public function writeTo (bytes :ByteArray) :ByteArray
    {
        bytes.writeInt(width);
        bytes.writeInt(height);

        return bytes;
    }


}
}
