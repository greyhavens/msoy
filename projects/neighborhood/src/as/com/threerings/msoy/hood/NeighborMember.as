package com.threerings.msoy.hood {

import com.adobe.serialization.json.*;

/**
 * Represents a single member in a neighborhood: their name and id, and whether or not
 * they are online. This class will most likely fill in with more data soon.
 */
public class NeighborMember
    implements Neighbor
{
    /** The member's name. */
    public var memberName:String;
    /** The member's id. */
    public var memberId:Number;
    /** Whether or not the member is currently online. */
    public var isOnline:Boolean;

    /**
     * Instantiate and populate a {@link NeighborMember} give a JSON configuration.
     */
    public static function fromJSON(JSON: Object) :NeighborMember
    {
        var member:NeighborMember = new NeighborMember();
        if (JSON.name == null || JSON.id == null) {
            throw new Error("Missing name/id in JSON");
        }
        member.memberName = JSON.name;
        member.memberId = JSON.id;
        member.isOnline = JSON.isOnline;
        return member;
    }
}
}