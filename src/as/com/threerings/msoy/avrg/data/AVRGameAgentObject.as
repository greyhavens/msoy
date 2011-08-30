//
// $Id$

package com.threerings.msoy.avrg.data {

import com.whirled.game.client.PropertySpaceHelper;
import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DSet;

import com.threerings.bureau.data.AgentObject;

import com.threerings.msoy.avrg.data.SceneInfo;

public class AVRGameAgentObject extends AgentObject
    implements PropertySpaceObject
{
    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>scenes</code> field. */
    public static const SCENES :String = "scenes";

    /** The field name of the <code>gameOid</code> field. */
    public static const GAME_OID :String = "gameOid";
    // AUTO-GENERATED: FIELDS END

    /** A set of scenes containing (or having recently contained) players of this AVRG. */
    public var scenes :DSet = new DSet();
    SceneInfo; // no-op reference to force link

    /** ID of the game object. */
    public var gameOid :int;

    /** ID of the game record. */
    public var gameId :int;

    /** Service for doing agent things. */
    public var agentService :AVRGameAgentMarshaller;

    /** Used to read and write properties private to the agent with the AVRGameManager. */
    public var propertiesService :PropertySpaceMarshaller;

     // from PropertySpaceObject
    public function getUserProps () :Object
    {
        return _props;
    }

    // from PropertySpaceObject
    public function getPropService () :PropertySpaceMarshaller
    {
        return propertiesService;
    }

    // from interface Streamable
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        // first read any regular bits
        readDefaultFields(ins);

        // then user properties
        PropertySpaceHelper.readProperties(this, ins);
    }

    /**
     * Reads the fields written by the default serializer for this instance.
     */
    protected function readDefaultFields (ins :ObjectInputStream) :void
    {
        scenes = DSet(ins.readObject());
        gameOid = ins.readInt();
        gameId = ins.readInt();
        agentService = AVRGameAgentMarshaller(ins.readObject());
        propertiesService = PropertySpaceMarshaller(ins.readObject());
    }

    /** The raw properties set by the game. */
    protected var _props :Object = new Object();
}
}
