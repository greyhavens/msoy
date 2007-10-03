//
// $Id$

package com.threerings.msoy.game.data {

import com.threerings.io.ObjectInputStream;

import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.OidList;

import com.threerings.ezgame.data.EZGameObject;

import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Holds game state for an AVRGame.
 */
public class AVRGameObject extends DObject
{
    /** The identifier for a MessageEvent containing a user message. */
    public static const USER_MESSAGE :String = "Umsg";

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>gameMedia</code> field. */
    public static const GAME_MEDIA :String = "gameMedia";

    /** The field name of the <code>state</code> field. */
    public static const STATE :String = "state";

    /** The field name of the <code>playerOids</code> field. */
    public static const PLAYER_OIDS :String = "playerOids";

    /** The field name of the <code>players</code> field. */
    public static const PLAYERS :String = "players";

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

    /** Used to communicate with the AVRGameManager. */
    public var avrgService :AVRGameMarshaller;
    
    override public function readObject (ins :ObjectInputStream) :void
    {
        super.readObject(ins);

        gameMedia = (ins.readObject() as MediaDesc);
        state = (ins.readObject() as DSet);
        playerOids = (ins.readObject() as OidList);
        players = (ins.readObject() as DSet);
        avrgService = (ins.readObject() as AVRGameMarshaller);
    }
}
}
