package com.threerings.msoy.hood {

import com.adobe.serialization.json.*;

/**
 * Represents the neighborhood around a central member: that member's name and id,
 * and arrays of their friends and groups.
 */
public class Neighborhood
{
    /** The member's name. */
    public var memberName:String;
    /** The member's id. */
    public var memberId:Number;
    /** The member's friends, as {@link NeighborFriend} objects. */
    public var friends:Array;
    /** The member's groups, as {@link NeighborGroup} objects. */
    public var groups:Array;

    /**
     * Instantiate and populate a {@link Neighborhood} give a JSON configuration.
     */
    public static function fromJSON(JSON: Object) :Neighborhood
    {
        var i :int;
        var hood:Neighborhood = new Neighborhood();
        if (JSON.name == null || JSON.id == null) {
            throw new Error("Missing name/id in JSON");
        }
        hood.memberName = JSON.name;
        hood.memberId = JSON.id;
        if (JSON.friends != null) {
            hood.friends = new Array();
            for (i = 0; i < JSON.friends.length; i ++) {
                hood.friends[i] = NeighborFriend.fromJSON(JSON.friends[i]);
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