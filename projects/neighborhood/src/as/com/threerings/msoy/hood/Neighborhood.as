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