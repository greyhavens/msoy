//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

public class ItemListInfo
    implements Streamable, DSet_Entry
{
    public static const NO_LIST :int = 0;

    public static const AUDIO_PLAYLIST :int = 1;
    public static const VIDEO_PLAYLIST :int = 2;
    public static const CATALOG_BUNDLE :int = 3;

    public var listId :int;

//    public var memberId :int;

    public var type :int;

    public var name :String;

    public function ItemListInfo ()
    {
    }

    public function getKey () :Object
    {
        return listId;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        listId = ins.readInt();
//        memberId = ins.readInt();
        type = ins.readByte();
        name = (ins.readField(String) as String);
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(listId);
//        out.writeInt(memberId);
        out.writeByte(type);
        out.writeField(name);
    }
}
}
