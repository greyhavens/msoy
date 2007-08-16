//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.util.Comparable;
import com.threerings.util.Hashable;

import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;

/**
 * Does something extraordinary.
 */
public class SceneBookmarkEntry
    implements Comparable, DSet_Entry, Hashable
{
    /** The scene id being represented. */
    public var sceneId :int;

    /** The name of the scene. */
    public var sceneName :String;

    /** The time of the last visit. */
    public var lastVisit :Number;

    /** Suitable for deserialization. */
    public function SceneBookmarkEntry ()
    {
    }

    // from Hashable
    public function hashCode () :int
    {
        return sceneId;
    }

    // from Comparable
    public function compareTo (other :Object) :int
    {
        var that :SceneBookmarkEntry = (other as SceneBookmarkEntry);
        return this.sceneId - that.sceneId;
    }

    // from Hashable
    public function equals (other :Object) :Boolean
    {
        return (other is SceneBookmarkEntry) &&
            (other as SceneBookmarkEntry).sceneId == this.sceneId;
    }

    public function toString () :String
    {
        return sceneName;
    }

    // from DSet.Entry
    public function getKey () :Object
    {
        return sceneId;
    }

    // from superinterface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        sceneId = ins.readInt();
        sceneName = (ins.readField(String) as String);
        lastVisit = ins.readDouble();
    }

    // from superinterface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(sceneId);
        out.writeField(sceneName);
        out.writeDouble(lastVisit);
    }
}
}
