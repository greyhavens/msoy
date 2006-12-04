package com.threerings.msoy.hood {

import com.adobe.serialization.json.*;

/**
 * Represents a single group in a neighborhood: its name and id, and its membership
 * count. We should probably also include its invitation policy.
 */
public class NeighborGroup
{
    /** The group's name. */
    public var groupName:String;
    /** The group's id. */
    public var groupId:Number;
    /** The number of members in this group. */
    public var members:Number;

    /**
     * Instantiate and populate a {@link NeighborGroup} give a JSON configuration.
     */
    public static function fromJSON(JSON: Object) :NeighborGroup
    {
        var group:NeighborGroup = new NeighborGroup();
        if (JSON.groupName == null || JSON.groupId == null || JSON.members == null) {
            throw new Error("Missing groupName/groupID/members in JSON");
        }
        group.groupName = JSON.groupName;
        group.groupId = JSON.groupId;
        group.members = JSON.members;
        return group;
    }
}
}