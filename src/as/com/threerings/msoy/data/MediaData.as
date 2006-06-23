package com.threerings.msoy.data {

import flash.geom.Point;

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

    /**
     * Get the hotspot for this media, or null if there is none.
     */
    public function get hotSpot () :Point
    {
        return (DATA[id][2] as Point);
    }

    /**
     * Get the url at which the media may be loaded.
     */
    public function get URL () :String
    {
        if (DATA[id][3]) {
            return String(DATA[id][0]);
        }
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
        // [ mediaURI, isInteractive, hotspot ]
 /* 0*/ [ "hipsterzombie.swf", true, new Point(126, 367) ],
        [ "socketbunny.swf", true, new Point(155, 360) ],
        //[ "TestAvatar.swf", false ],
        [ "pedestrian.swf", true ],
        [ "alleydoor.swf", true, new Point(36, 355) ],
        [ "bigvid.flv", false ],
        //[ "flv/320x240.swf?url=JoshuaTree.flv", false ],
        [ "JoshuaTree.flv", false ],
        [ "rainbowdoor.swf", true, new Point(144, 367) ],
        [ "fans.swf", false ],
        [ "fancyroom.png", false ],
        [ "pinball.swf", true, new Point(100, 252) ],
 /*10*/ [ "directorschair.swf", false, new Point(54, 154) ],
        [ "alley.png", false ],
        [ "curtain.swf", false, new Point(130, 530) ],
        [ "BollWeevil.mp3", false ],
        [ "http://sd165.sivit.org/random_4432/files/420.swf",
                false, null, true ],
        [ "http://www.microgames.info/games/3d_logic/loader_3d_logic.swf",
                false, null, true ],
        [ "comicroom.png", false ],
        [ "comicroomforeground.png", false ],
        [ "bendaydoor.swf", false ],
        [ "bendaytransport.swf", false ],
 /*20*/ [],
    ];

    protected static const BASE_URL :String =
        "http://tasman.sea.earth.threerings.net/~ray/";
        //"http://ice.puzzlepirates.com/msoy/";
}

}
