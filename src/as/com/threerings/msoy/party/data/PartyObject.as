//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.data.VizMemberName;

public class PartyObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>mates</code> field. */
    public static const MATES :String = "mates";

    /** The field name of the <code>id</code> field. */
    public static const ID :String = "id";

    /** The field name of the <code>leaderId</code> field. */
    public static const LEADER_ID :String = "leaderId";

    /** The field name of the <code>sceneId</code> field. */
    public static const SCENE_ID :String = "sceneId";

    /** The field name of the <code>status</code> field. */
    public static const STATUS :String = "status";

    /** The field name of the <code>recruiting</code> field. */
    public static const RECRUITING :String = "recruiting";
    // AUTO-GENERATED: FIELDS END

    /** The list of people in this party. */
    public var mates :DSet; // of VizMemberName

    /** This party's guid. */
    public var id :int;

    /** The member ID of the current leader. */
    public var leaderId :int;

    /** The current location of the party. */
    public var sceneId :int;

    /** Customizable flavor text. */
    public var status :String;

    /** TODO: Doc. */
    public var recruiting :int;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        mates = DSet(ins.readObject());
        id = ins.readInt();
        leaderId = ins.readInt();
        sceneId = ins.readInt();
        status = (ins.readField(String) as String);
        recruiting = ins.readByte();
    }
}
}
