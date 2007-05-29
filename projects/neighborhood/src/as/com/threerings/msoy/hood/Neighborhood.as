package com.threerings.msoy.hood {

import flash.display.LoaderInfo;

import com.adobe.serialization.json.*;

/**
 * Represents the neighborhood, potentially centered around one specific member or group.
 */
public class Neighborhood
{
    /** The central member for whom this query was generated, or null. */
    public var centralMember :NeighborMember;

    /** The central group for whom this query was generated, or null. */
    public var centralGroup :NeighborGroup;

    /** The member's houses, as {@link NeighborMember} objects. */
    public var houses :Array;

    /** The member's groups, as {@link NeighborGroup} objects. */
    public var groups :Array;

    /** The member's games, as {@link NeighborGame} objects. */
    public var games: Array;

    /** A list of public chat channels. */
    public var channels :Array;

    /** The total population to display, or -1 if none given. */
    public var totalPop :int = -1;

    /**
     * Instantiate and populate a {@link Neighborhood} from JSON configuration
     * extracted from the LoaderInfo FlashVars parameter 'neighborhood'.
     */
    public static function fromParameters(params :Object) :Neighborhood
    {
        return fromJSON(new JSONDecoder(params.neighborhood).getObject());
    }

    /**
     * Instantiate and populate a {@link Neighborhood} given a JSON configuration.
     */
    public static function fromJSON(JSON: Object) :Neighborhood
    {
        var hood:Neighborhood = new Neighborhood();
        var i :int;

        if (JSON.member != null) {
            hood.centralMember = NeighborMember.fromJSON(JSON.member);
        }
        if (JSON.group != null) {
            hood.centralGroup = NeighborGroup.fromJSON(JSON.group);
        }

        hood.channels = new Array();
        if (JSON.channels != null) {
            for (i = 0; i < JSON.channels.length; i ++) {
                hood.channels[i] = ChatChannel.fromJSON(JSON.channels[i]);
            }
        }            

        hood.houses = new Array();
        if (JSON.friends != null) {
            for (i = 0; i < JSON.friends.length; i ++) {
                hood.houses[i] = NeighborMember.fromJSON(JSON.friends[i]);
            }
        }

        hood.groups = new Array();
        if (JSON.groups != null) {
            for (i = 0; i < JSON.groups.length; i ++) {
                hood.groups[i] = NeighborGroup.fromJSON(JSON.groups[i]);
            }
        }

        hood.games = new Array();
        if (JSON.games != null) {
            for (i = 0; i < JSON.games.length; i ++) {
                hood.games[i] = NeighborGame.fromJSON(JSON.games[i]);
            }
        }
        if (JSON.totpop != undefined) {
            hood.totalPop = JSON.totpop;
        }
        return hood;
    }
}
}
