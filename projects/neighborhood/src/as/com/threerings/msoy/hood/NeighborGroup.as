package com.threerings.msoy.hood {

import com.adobe.serialization.json.*;

/**
 * Represents a single group in a neighborhood: its name and id, and its membership
 * count. We should probably also include its invitation policy.
 */
public class NeighborGroup
    implements Neighbor
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
        if (JSON.name == null || JSON.id == null) {
            throw new Error("Missing name/id in JSON");
        }
        group.groupName = JSON.name;
        group.groupId = JSON.id;
        group.members = JSON.members;
        return group;
    }
}
}