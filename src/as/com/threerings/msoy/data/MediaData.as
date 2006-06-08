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
        return DATA.length;
    }

    /** Temporary constructor. */
    public function MediaData (id :int = 0)
    {
        this.id = id;
        _width = _height = -1;
        isAVM1 = true;
    }

    // TODO
    public function get originX () :Number
    {
        if (id == 0) {
            return 30;
        }
        return 0;
    }

    // TODO
    public function get originY () :Number
    {
        if (id == 0) {
            return 200;
        }
        return 0;
    }

    /**
     * Get the url at which the media may be loaded.
     */
    public function get URL () :String
    {
        return BASE_URL + DATA[id][0];
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
        return Boolean(DATA[id][1]);
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
    protected static const DATA :Array = [
        // [ mediaURI, isInteractive ]
 /* 0*/ [ "hipsterzombie.swf", true ],
        [ "socketbunny.swf", true ],
        [ "pedestrian.swf", true ],
        [ "alleydoor.swf", true ],
        [ null, false ],
        [ null, false ],
        [ "rainbowdoor.swf", true ],
        [ "fans.swf", false ],
        [ "fancyroom.png", false ],
        [ "pinball.swf", true ],
 /*10*/ [ "directorschair.swf", false ],
        [ "alley.png", false ],
        [ "curtain.swf", false ]
    ];

    protected static const BASE_URL :String =
        "http://tasman.sea.earth.threerings.net/~ray/";
}

}
