//
// $Id$

package com.threerings.msoy.avrg.data {

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.OidList;

import com.threerings.util.Iterator;
import com.threerings.util.Name;

import com.threerings.msoy.avrg.data.PlayerLocation;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Holds game state for an AVRGame.
 */
public class AVRGameObject extends DObject
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

    /** The field name of the <code>state</code> field. */
    public static const STATE :String = "state";

    /** The field name of the <code>playerOids</code> field. */
    public static const PLAYER_OIDS :String = "playerOids";

    /** The field name of the <code>players</code> field. */
    public static const PLAYERS :String = "players";

    /** The field name of the <code>playerLocs</code> field. */
    public static const PLAYER_LOCS :String = "playerLocs";

    /** The field name of the <code>avrgService</code> field. */
    public static const AVRG_SERVICE :String = "avrgService";

    // AUTO-GENERATED: FIELDS END

    /** The defining media of the AVRGame. */
    public var gameMedia :MediaDesc;

    /** Contains the game's state. */
    public var state :DSet = new DSet();

    /** Tracks the oid of the body objects of all of the active players of this game. */
    public var playerOids :OidList;

    /** Contains an {@link OccupantInfo} record for each player of this game. */
    public var players :DSet;

    /**
     * Tracks the (scene) location of each player. This data is only updated when the agent
     * has successfully subscribed to the scene's RoomObject and it's safe for clients to make
     * requests.
     */
    public var playerLocs :DSet = new DSet();
    PlayerLocation; // no-op reference to force link

    /** Used to communicate with the AVRGameManager. */
    public var avrgService :AVRGameMarshaller;
    
    /**
     * Looks up a player's occupant info by name.
     *
     * @return the occupant info record for the named user or null if no
     * user in the room has that username.
     */
    public function getOccupantInfo (username :Name) :OccupantInfo
    {
        var itr :Iterator = players.iterator();
        while (itr.hasNext()) {
            var info :OccupantInfo = (itr.next() as OccupantInfo);
            if (info.username.equals(username)) {
                return info;
            }
        }
        return null;
    }

    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        gameMedia = (ins.readObject() as MediaDesc);
        state = (ins.readObject() as DSet);
        playerOids = (ins.readObject() as OidList);
        players = (ins.readObject() as DSet);
        playerLocs = (ins.readObject() as DSet);
        avrgService = (ins.readObject() as AVRGameMarshaller);
    }
}
}
