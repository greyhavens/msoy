//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectOutputStream;
import com.threerings.io.ObjectInputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * Summarizes a person's membership in a group.
 */
public class QuestState
    implements Streamable, DSet_Entry
{
    public static const STEP_NEW :int = -1;

    public static const STEP_COMPLETED :int = -2;

    public var questId :String;

    public var step :int;

    public var status :String;

    public var sceneId :int;

    // from DSet.Entry
    public function getKey () :Object
    {
       return questId;
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        questId = ins.readField(String) as String;
        step = ins.readInt();
        status = ins.readField(String) as String;
        sceneId = ins.readInt();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(questId);
        out.writeInt(step);
        out.writeField(status);
        out.writeInt(sceneId);
    }
}
}
