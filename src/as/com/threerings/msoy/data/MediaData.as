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

    public static function getTestCount () :int
    {
        return URLS.length;
    }

    /** Temporary constructor. */
    public function MediaData (id :int = 0)
    {
        this.id = id;
        _width = _height = -1;
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
        return (URLS[id] as String);
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

    /* TEMP. */
    protected static const URLS :Array = [
        //"http://bogocorp.com/blaaaah.gif",
//        "http://tasman.sea.earth.threerings.net/~ray/Joshua%20Tree.flv",
        "http://tasman.sea.earth.threerings.net/~ray/bunny_walk.swf",
        "http://www.puzzlepirates.com/images/index/screen2.png",
//        "http://www.puzzlepirates.com/images/index/screen3.png",
//        "http://www.puzzlepirates.com/images/puzzles/bilge/girl.swf",
//        "http://www.puzzlepirates.com/images/puzzles/sword/girl.swf",
//        "http://www.youtube.com/v/SbY0Jh9_RJ8",
        "http://tasman.sea.earth.threerings.net/~ray/AvatarTest.swf",
//        "http://bogocorp.com/bogologo.gif"
    ];
}

}
