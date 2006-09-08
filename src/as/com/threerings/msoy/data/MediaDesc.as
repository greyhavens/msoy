package com.threerings.msoy.data {

import flash.geom.Point;

import flash.utils.ByteArray;

import com.threerings.util.Hashable;
import com.threerings.util.Util;
import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.data.Item;
import com.threerings.msoy.item.data.MediaItem;

/**
 * A class containing metadata about a media object.
 */
public class MediaDesc
    implements Hashable, Streamable
{
    public var isAVM1 :Boolean;

    /** Temp: a universal id for this media. */
    /** or -1 if newstyle. */
    public var id :int;

    public var hash :ByteArray;

    public var mimeType :int;


    /**
     * Create a media descriptor from the specified item.
     */
    public static function fromItem (item :Item) :MediaDesc
    {
        var data :MediaDesc = new MediaDesc(-1);
        if (item is MediaItem) {
            var mitem :MediaItem = (item as MediaItem);
            data.hash = mitem.mediaHash;
            data.mimeType = mitem.mimeType;
        } else {
            // other kinds of items should have default representations
            // of some special media already in the system...
            // TODO
        }

        return data;
    }

    public static function getTestCount () :int
    {
        return DATA.length;
    }

    /** Temporary constructor. */
    public function MediaDesc (id :int = 0)
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
        if (id == -1) {
            return null;
        }
        return (DATA[id][2] as Point);
    }

    /**
     * Get the url at which the media may be loaded.
     */
    public function get URL () :String
    {
        if (id == -1) {
            return MediaItem.getMediaPath(hash, mimeType);
        }

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
        if (id == -1) {
            // TODO? Fawk
            return false;
        }
        return Boolean(DATA[id][1]);
    }

    // documentation inherited from Hashable
    public function hashCode () :int
    {
        return id;
    }

    // documentation inherited from Hashable
    public function equals (other :Object) :Boolean
    {
        if (other is MediaDesc) {
            var that :MediaDesc = (other as MediaDesc);
            return (this.id == that.id) && Util.equals(this.hash, that.hash);
        }
        return false;
    }

    // documentation inherited from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(id);
        out.writeField(hash);
        out.writeByte(mimeType);
    }

    // documentation inherited from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        id = ins.readInt();
        hash = (ins.readField(ByteArray) as ByteArray);
        mimeType = ins.readByte();
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
 //* 5*/ [ "JoshuaTree.flv", false ],
 /* 5*/ [ "mario.flv", false ],
        [ "rainbowdoor.swf", true, new Point(144, 367) ],
        [ "fans.swf", false ],
        [ "fancyroom.png", false ],
        [ "pinball.swf", true, new Point(100, 252) ],
 /*10*/ [ "directorschair.swf", false, new Point(54, 154) ],
        [ "alley.png", false ],
        [ "curtain.swf", true, new Point(130, 530) ],
        [ "BollWeevil.mp3", false ],
        [ "http://sd165.sivit.org/random_4432/files/420.swf",
                false, null, true ],
// /*15*/ [ "http://www.microgames.info/games/3d_logic/loader_3d_logic.swf",
//                true, null, true ],
 /*15*/ [ "Reversi.swf", true ],
        [ "comicroom.png", false ],
        [ "comicroomforeground.png", false ],
        [ "bendaydoor.swf", false ],
        [ "bendaytransport.swf", false ],
 /*20*/ [ "teenqueen.swf", true ],
        [ "critter.swf", true ],
        [ "wendle6.swf", true, new Point(116, 362) ],
        //[ "cliff_background.png", false ],
        [ "cliff_background_anim.swf", false ],
        [ "cliff_foreground.png", false ],
 /*25*/ [ "octocar_all.swf", false ],
        [ "faucet_scene.png", false ],
        [ "pipe_door.png", false, new Point(40, 215) ],
        [ "crayon_room.png", false ],
        [ "cactus.png", false ],
 /*30*/ [ "fishbowl.png", false ],
        [ "ladder.png", false, new Point(70, 36) ],
        [ "red_door.png", false, new Point(90, 333) ],
        [ "aqua_door.png", false ],
        [ "smile_door.png", false, new Point(88, 327) ],
 /*35*/ [ "candles.png", false, new Point(76, 0) ],
        [ "cedric.swf", false, new Point(133, 376) ],
        [ "frame.png", false, new Point(151, 111) ],
        [ "soy.swf", false ],
        [ "mthomasbunny.swf", false, new Point(155, 360) ],
 /*40*/ [ "mjohnsonbunny.swf", false, new Point(155, 360) ],
        [ "npopovichbunny.swf", false, new Point(155, 360) ],
        [ "rbeerbunny.swf", false, new Point(155, 360) ],
        [ "akapolkabunny.swf", false, new Point(155, 360) ],
        [ "mbaynebunny.swf", false, new Point(155, 360) ],
 /*45*/ [ "jleplastrierbunny.swf", false, new Point(155, 360) ],
        [ "jgramsebunny.swf", false, new Point(155, 360) ],
        [ "template_human.swf", false, new Point(155, 360) ],
        [ "rgreenwellbunny.swf", false, new Point(155, 360) ],
        [ "rbeerbunny2.swf", false, new Point(155, 360) ],
 /*50*/ [ "mtungbunny.swf", false, new Point(155, 360) ],
        [ "clothestest.swf", false ],
        [ "humanbase.swf", false ],
        [ "square100.png", false, new Point(50, 50) ],
        [ "djamesbunny.swf", false, new Point(155, 360) ],
 /*55*/ [ "ClickFest.swf", true ],
        [ "BigTwo.swf", true ],
    ];

    public static const BASE_URL :String = "/media/static/";
}

}
