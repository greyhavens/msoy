package com.threerings.msoy.hood {

import flash.display.LoaderInfo;

import com.adobe.serialization.json.*;

/**
 * Represents the neighborhood around a central member: that member's name and id,
 * and arrays of their houses, groups and games.
 */
public class Neighborhood
{
    public var centralMember :NeighborMember;
    public var centralGroup :NeighborGroup;

    /** The member's houses, as {@link NeighborMember} objects. */
    public var houses :Array;
    /** The member's groups, as {@link NeighborGroup} objects. */
    public var groups :Array;
    /** The member's games, as {@link NeighborGame} objects. */
    public var games: Array;

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
        var i :int;
        var hood:Neighborhood = new Neighborhood();
        if (JSON.member != null) {
            hood.centralMember = NeighborMember.fromJSON(JSON.member);
        }
        if (JSON.group != null) {
            hood.centralGroup = NeighborGroup.fromJSON(JSON.group);
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