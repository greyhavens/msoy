//
// $Id$

package com.threerings.msoy.data {

import com.threerings.msoy.data.all.VisitorInfo;

/**
 * Encapsulates a small amount of information about the place or places the user is currently in.
 * Note that it is possible to be in a room or a game or both or neither.
 */
public class PlaceInfo
{
    /** The id of the scene we are in, or 0 if none. */
    public var sceneId :int;

    /** The name of the scene we are in, or null if none. */
    public var sceneName :String;

    /** The id of the game we are in, or 0 if none. */
    public var gameId :int;

    /** The name of the game we are in, or null if none. */
    public var gameName :String;

    /** True if we are in a game and it is an avr game. (AVR games can be sceneless). */
    public var avrGame :Boolean;

    /**
     * Tests if we are in a game and if the game is not a development version.
     */
    public function get inGame () :Boolean
    {
        return gameId != 0;
    }

    /**
     * Gets the name of the game if we are in a game, otherwise the name of the scene or null if
     * neither.
     */
    public function get name () :String
    {
        return inGame ? gameName : sceneName;
    }

    /**
     * Gets the id of the game if we are in a game, otherwise the id of the scene or null if
     * neither.
     */
    public function get id () :int
    {
        return inGame ? gameId : sceneId;
    }

    /**
     * Return a list of variables suitable for the FlashVars embed param. Includes entry vector
     * information.
     */
    public function makeEmbedVars () :String
    {
        var vars :Array = [];
        var vec :String = VisitorInfo.VECTOR_ID;
        if (gameId != 0) {
            vars.push(vec + "=e.whirled." + GAME_VECTOR + "." + gameId);
            if (avrGame) {
                vars.push("worldGame=" + gameId);
                if (sceneId != 0) {
                    vars.push("gameRoomId=" + sceneId);
                }
            } else {
                vars.push("gameId=" + gameId);
            }

        } else if (sceneId != 0) {
            vars.push(vec + "=e.whirled." + ROOM_VECTOR + "." + sceneId);
            vars.push("sceneId=" + sceneId);
        }

        return vars.join("&");
    }

    /** This vector string represents an embedded room. */
    protected static const ROOM_VECTOR :String = "rooms";

    /** This vector string represents an embedded game. */
    protected static const GAME_VECTOR :String = "games";
}
}
