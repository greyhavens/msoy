//
// $Id$

package com.threerings.msoy.party.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.chat.data.SpeakMarshaller;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MediaDesc;

public class PartyObject extends DObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>id</code> field. */
    public static const ID :String = "id";

    /** The field name of the <code>name</code> field. */
    public static const NAME :String = "name";

    /** The field name of the <code>icon</code> field. */
    public static const ICON :String = "icon";

    /** The field name of the <code>group</code> field. */
    public static const GROUP :String = "group";

    /** The field name of the <code>peeps</code> field. */
    public static const PEEPS :String = "peeps";

    /** The field name of the <code>leaderId</code> field. */
    public static const LEADER_ID :String = "leaderId";

    /** The field name of the <code>sceneId</code> field. */
    public static const SCENE_ID :String = "sceneId";

    /** The field name of the <code>status</code> field. */
    public static const STATUS :String = "status";

    /** The field name of the <code>recruitment</code> field. */
    public static const RECRUITMENT :String = "recruitment";

    /** The field name of the <code>partyService</code> field. */
    public static const PARTY_SERVICE :String = "partyService";

    /** The field name of the <code>speakService</code> field. */
    public static const SPEAK_SERVICE :String = "speakService";
    // AUTO-GENERATED: FIELDS END

    /** This party's guid. */
    public var id :int;

    /** The name of this party. */
    public var name :String;

    /** The icon for this party. */
    public var icon :MediaDesc;

    /** The group under whose auspices we woop-it-up. */
    public var group :GroupName;

    /** The list of people in this party. */
    public var peeps :DSet; /* of */ PartyPeep; // link the class in. :)

    /** The member ID of the current leader. */
    public var leaderId :int;

    /** The current location of the party. */
    public var sceneId :int;

    /** Customizable flavor text. */
    public var status :String;

    /** This party's access control. @see PartyCodes */
    public var recruitment :int;

    /** The service for doing things on this party. */
    public var partyService :PartyMarshaller;

    /** Speaking on this party object. */
    public var speakService :SpeakMarshaller;

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        id = ins.readInt();
        name = ins.readField(String) as String;
        icon = MediaDesc(ins.readObject());
        group = GroupName(ins.readObject());
        peeps = DSet(ins.readObject());
        leaderId = ins.readInt();
        sceneId = ins.readInt();
        status = ins.readField(String) as String;
        recruitment = ins.readByte();
        partyService = PartyMarshaller(ins.readObject());
        speakService = SpeakMarshaller(ins.readObject());
    }
}
}
