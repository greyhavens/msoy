//
// $Id$

package com.threerings.msoy.avrg.data {

import com.whirled.game.client.PropertySpaceHelper;
import com.whirled.game.data.ContentMarshaller;
import com.whirled.game.data.GameData;
import com.whirled.game.data.PrizeMarshaller;
import com.whirled.game.data.PropertySpaceMarshaller;
import com.whirled.game.data.PropertySpaceObject;
import com.whirled.game.data.WhirledGameMessageMarshaller;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.TypedArray;

import com.threerings.presents.dobj.DSet;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.avrg.data.PlayerLocation;
import com.threerings.msoy.party.data.PartyLeader;
import com.threerings.msoy.party.data.PartyPlaceObject;
import com.threerings.msoy.party.data.PartySummary;

/**
 * Holds game state for an AVRGame.
 */
public class AVRGameObject extends PlaceObject
    implements PropertySpaceObject, PartyPlaceObject
{
    /** The identifier for a MessageEvent containing a user message. */
    public static const USER_MESSAGE :String = "Umsg";

    /** The identifier for a MessageEvent containing a user message. */
    public static const USER_MESSAGE_EXCLUDE_AGENT :String = "UmsgEA";

    /** The identifier for a MessageEvent containing ticker notifications. */
    public static const TICKER :String = "Utick";

    /** A message dispatched to each player's client object when a task is completed. */
    public static const TASK_COMPLETED_MESSAGE :String = "TaskCompleted";

    // AUTO-GENERATED: FIELDS START
    /** The field name of the <code>playerLocs</code> field. */
    public static const PLAYER_LOCS :String = "playerLocs";

    /** The field name of the <code>parties</code> field. */
    public static const PARTIES :String = "parties";

    /** The field name of the <code>partyLeaders</code> field. */
    public static const PARTY_LEADERS :String = "partyLeaders";

    /** The field name of the <code>avrgService</code> field. */
    public static const AVRG_SERVICE :String = "avrgService";
    // AUTO-GENERATED: FIELDS END

    // no-op reference; force linkage
    AVRGameOccupantInfo;

    /**
     * Tracks the (scene) location of each player. This data is only updated when the agent
     * has successfully subscribed to the scene's RoomObject and it's safe for clients to make
     * requests.
     */
    public var playerLocs :DSet; /*of*/ PlayerLocation; // no-op reference to force link

    /** Information on the parties presently in this room. */
    public var parties :DSet; /*of*/ PartySummary; // no-op reference to force link

    /** Current party leaders. */
    public var partyLeaders :DSet; /* of */ PartyLeader;

    /** The various game data available to this game. */
    public var gameData :TypedArray; /*of*/ GameData;

    /** Used to communicate with the AVRGameManager. */
    public var avrgService :AVRGameMarshaller;

    /** The service interface for handling game content. */
    public var contentService :ContentMarshaller;

    /** The service interface for awarding prizes and trophies. */
    public var prizeService :PrizeMarshaller;

   /** Used to send messages. */
    public var messageService :WhirledGameMessageMarshaller;

    /** Used to communicate with the AVRGameManager. */
    public var propertiesService :PropertySpaceMarshaller;

    /** Can this game control the horizontal and the vertical? */
    public var isApproved :Boolean;

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

    // from PartyPlaceObject
    public function getParties () :DSet
    {
        return parties;
    }

    // from PartyPlaceObject
    public function getOccupants () :DSet
    {
        return occupantInfo;
    }

    // from PartyPlaceObject
    public function getPartyLeaders () :DSet
    {
        return partyLeaders;
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
        playerLocs = DSet(ins.readObject());
        parties = DSet(ins.readObject());
        partyLeaders = DSet(ins.readObject());
        gameData = TypedArray(ins.readObject());
        avrgService = AVRGameMarshaller(ins.readObject());
        contentService = ContentMarshaller(ins.readObject());
        prizeService = PrizeMarshaller(ins.readObject());
        messageService = WhirledGameMessageMarshaller(ins.readObject());
        propertiesService = PropertySpaceMarshaller(ins.readObject());
        isApproved = ins.readBoolean();
    }

    /** The raw properties set by the game. */
    protected var _props :Object = new Object();
}
}
