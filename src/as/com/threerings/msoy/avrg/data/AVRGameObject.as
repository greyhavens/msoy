//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.crowd.data.PlaceObject;

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DSet;

import com.threerings.msoy.avrg.data.PlayerLocation;

import com.whirled.game.client.PropertySpaceHelper;
import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.data.WhirledGameMessageMarshaller;

/**
 * Holds game state for an AVRGame.
 */
public class AVRGameObject extends PlaceObject
    implements PropertySpaceObject
{
    /** The identifier for a MessageEvent containing a user message. */
    public static const USER_MESSAGE :String = "Umsg";

    /** The identifier for a MessageEvent containing ticker notifications. */
    public static const TICKER :String = "Utick";

    /** A message dispatched to each player's client object when a task is completed. */
    public static const TASK_COMPLETED_MESSAGE :String = "TaskCompleted";

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>playerLocs</code> field. */
    public static const PLAYER_LOCS :String = "playerLocs";

    /** The field name of the <code>avrgService</code> field. */
    public static const AVRG_SERVICE :String = "avrgService";

    // AUTO-GENERATED: FIELDS END

    /**
     * Tracks the (scene) location of each player. This data is only updated when the agent
     * has successfully subscribed to the scene's RoomObject and it's safe for clients to make
     * requests.
     */
    public var playerLocs :DSet = new DSet();
    PlayerLocation; // no-op reference to force link

    /** Used to communicate with the AVRGameManager. */
    public var avrgService :AVRGameMarshaller;

    /** Used to send messages. */
    public var messageService :WhirledGameMessageMarshaller;

    /** Used to communicate with the AVRGameManager. */
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
        playerLocs = (ins.readObject() as DSet);
        avrgService = (ins.readObject() as AVRGameMarshaller);
        messageService = (ins.readObject() as WhirledGameMessageMarshaller);
        propertiesService = (ins.readObject() as PropertySpaceMarshaller);
    }

    /** The raw properties set by the game. */
    protected var _props :Object = new Object();
}
}
