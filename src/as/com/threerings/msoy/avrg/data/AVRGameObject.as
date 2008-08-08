//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.crowd.data.OccupantInfo;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.OidList;

import com.threerings.util.Iterator;
import com.threerings.util.Name;

import com.threerings.msoy.avrg.data.PlayerLocation;
import com.threerings.msoy.data.all.MediaDesc;

import com.whirled.game.client.PropertySpaceHelper;
import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.data.WhirledGameObject;

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

    /** A message dispatched to each player's client object when coins are awarded. */
    public static const COINS_AWARDED_MESSAGE :String = "FlowAwarded";

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>gameMedia</code> field. */
    public static const GAME_MEDIA :String = "gameMedia";

    /** The field name of the <code>playerLocs</code> field. */
    public static const PLAYER_LOCS :String = "playerLocs";

    /** The field name of the <code>avrgService</code> field. */
    public static const AVRG_SERVICE :String = "avrgService";

    // AUTO-GENERATED: FIELDS END

    /** The defining media of the AVRGame. */
    public var gameMedia :MediaDesc;

    /**
     * Tracks the (scene) location of each player. This data is only updated when the agent
     * has successfully subscribed to the scene's RoomObject and it's safe for clients to make
     * requests.
     */
    public var playerLocs :DSet = new DSet();
    PlayerLocation; // no-op reference to force link

    /** Used to communicate with the AVRGameManager. */
    public var avrgService :AVRGameMarshaller;

     // from PropertySpaceObject
    public function getUserProps () :Object
    {
        return _props;
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
        gameMedia = (ins.readObject() as MediaDesc);
        playerLocs = (ins.readObject() as DSet);
        avrgService = (ins.readObject() as AVRGameMarshaller);
    }

    /** The raw properties set by the game. */
    protected var _props :Object = new Object();
}
}
