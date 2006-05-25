package com.threerings.msoy.data {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * A class containing metadata about a media object.
 */
public class MediaData
    implements Streamable
{
    public var isAVM1 :Boolean;

    /** Temp: a universal id for this media. */
    public var id :int;

    /** Temporary constructor. */
    public function MediaData (url :String, width :int = -1, height :int = -1)
    {
        _url = url;
        _width = width;
        _height = height;
    }

    // TODO
    public function get originX () :Number
    {
        return 30;
    }

    // TODO
    public function get originY () :Number
    {
        return 30;
    }

    /**
     * Get the url at which the media may be loaded.
     */
    public function get URL () :String
    {
        return _url;
    }

    /**
     * Return the width of the media, or -1 if unknown.
     */
    public function get width () :int
    {
        return _width;
    }

    public function get height () :int
    {
        return _height;
    }

    /**
     * @return true if the media is clickable.
     */
    public function isInteractive () :Boolean
    {
        return true;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(id);
        // TODO
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        id = ins.readInt();
        // TODO
    }

    /** temp */
    protected var _url :String;

    /** Width and height, if known. */
    protected var _width :int, _height :int;
}

}
