package com.threerings.msoy.hood {

import flash.display.LoaderInfo;

import com.adobe.serialization.json.*;

/**
 * Represents the neighborhood around a central member: that member's name and id,
 * and arrays of their friends and groups.
 */
public class Neighborhood
{
    public var centralMember :NeighborMember;
    public var centralGroup :NeighborGroup;

    /** The member's friends, as {@link NeighborMember} objects. */
    public var friends:Array;
    /** The member's groups, as {@link NeighborGroup} objects. */
    public var groups:Array;

    /**
     * Instantiate and populate a {@link Neighborhood} from JSON configuration
     * extracted from the LoaderInfo FlashVars parameter 'neighborhood'.
     */
    public static function fromLoaderInfo(info :LoaderInfo) :Neighborhood
    {
        if (info == null || info.parameters == null) {
            return fromJSON(new JSONDecoder("{\"groups\":[{\"members\":1,\"name\":\"Spud Muffins\",\"id\":3},{\"members\":1,\"name\":\"Madison Bird Fondlers (MBF)\",\"id\":2},{\"members\":1,\"name\":\"sdfsdf\",\"id\":4},{\"members\":1,\"name\":\"Spam Is Good\",\"id\":5},{\"members\":1,\"name\":\"A B C\",\"id\":6}],\"friends\":[],\"member\":{\"name\":\"Zell\",\"id\":1}}").getObject());
        }
        return fromJSON(new JSONDecoder(info.parameters.neighborhood).getObject());
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
        if (JSON.friends != null) {
            hood.friends = new Array();
            for (i = 0; i < JSON.friends.length; i ++) {
                hood.friends[i] = NeighborMember.fromJSON(JSON.friends[i]);
            }
        }
        if (JSON.groups != null) {
            hood.groups = new Array();
            for (i = 0; i < JSON.groups.length; i ++) {
                hood.groups[i] = NeighborGroup.fromJSON(JSON.groups[i]);
            }
        }
        return hood;
    }
}
}