//
// $Id$

package com.threerings.msoy.hood {

import flash.display.LoaderInfo;

import com.adobe.serialization.json.*;

/**
 * Represents the neighborhood, potentially centered around one specific member or group.
 */
public class Neighborhood
{
    /** The central member for whom this query was generated, or null. */
    public var centralMember :Neighbor;

    /** The central group for whom this query was generated, or null. */
    public var centralGroup :NeighborGroup;

    /** The member's houses, as {@link Neighbor} objects. */
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
    public static function fromParameters (params :Object) :Neighborhood
    {
        return fromJSON(new JSONDecoder(params.neighborhood).getValue());
    }

    /**
     * Instantiate and populate a {@link Neighborhood} given a JSON configuration.
     */
    public static function fromJSON (json: Object) :Neighborhood
    {
        var hood:Neighborhood = new Neighborhood();
        var i :int;

        if (json.member != null) {
            hood.centralMember = new Neighbor();
            Neighbor.fromJSON(hood.centralMember, json.member);
        }
        if (json.group != null) {
            hood.centralGroup = NeighborGroup.fromJSON(json.group);
        }

        hood.channels = new Array();
//         if (json.channels != null) {
//             for (i = 0; i < json.channels.length; i ++) {
//                 hood.channels[i] = ChatChannel.fromJSON(json.channels[i]);
//             }
//         }            

        hood.houses = new Array();
        if (json.scenes != null) {
            for (i = 0; i < json.scenes.length; i ++) {
                hood.houses[i] = new Neighbor();
                Neighbor.fromJSON(hood.houses[i], json.scenes[i])
            }
        }

        hood.groups = new Array();
//         if (json.groups != null) {
//             for (i = 0; i < json.groups.length; i ++) {
//                 hood.groups[i] = NeighborGroup.fromJSON(json.groups[i]);
//             }
//         }

        hood.games = new Array();
        if (json.games != null) {
            for (i = 0; i < json.games.length; i ++) {
                hood.games[i] = NeighborGame.fromJSON(json.games[i]);
            }
        }
        if (json.totpop != undefined) {
            hood.totalPop = json.totpop;
        }
        return hood;
    }
}
}
