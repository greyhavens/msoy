package com.threerings.msoy.game.data {

import com.threerings.util.Cloneable;
import com.threerings.util.Equalable;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.msoy.item.web.MediaDesc;

public class GameSummary 
    implements Cloneable, Streamable, Equalable
{
    /** The game item id */
    public var gameId :int

    /** The thumbnail of the game - used as a game icon. */
    public var thumbMedia :MediaDesc;

    /** The name of the game - used as a tooltip */
    public var name :String;

    // documentation from Equalable
    public function equals (other :Object) :Boolean 
    {
        if (other is GameSummary) {
            var data :GameSummary = other as GameSummary;
            return data.gameId == this.gameId;
        }
        return false;
    }

    // documentation from Cloneable
    public function clone () :Object
    {
        var data :GameSummary = new GameSummary();
        data.gameId = this.gameId;
        data.name = this.name;
        if (thumbMedia != null) {
            data.thumbMedia = new MediaDesc(thumbMedia.hash, thumbMedia.mimeType, 
                thumbMedia.constraint);
        }
        return data;
    }

    // documentation from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        gameId = ins.readInt();
        thumbMedia = ins.readObject() as MediaDesc;
        name = ins.readField(String) as String;
    }

    // documntation from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(gameId);
        out.writeObject(thumbMedia);
        out.writeField(name);
    }
}
}
