package com.threerings.msoy.hood {

import com.adobe.serialization.json.*;

/**
 * Represents a single friend in a neighborhood: their name and id, and whether or not
 * they are online. This class will most likely fill in with more data soon.
 */
public class NeighborFriend
{
    /** The member's name. */
    public var memberName:String;
    /** The member's id. */
    public var memberId:Number;
    /** Whether or not the member is currently online. */
    public var isOnline:Boolean;

    /**
     * Instantiate and populate a {@link NeighborFriend} give a JSON configuration.
     */
    public static function fromJSON(JSON: Object) :NeighborFriend
    {
        var friend:NeighborFriend = new NeighborFriend();
        if (JSON.memberName == null || JSON.memberId == null) {
            throw new Error("Missing memberName/memberID in JSON");
        }
        friend.memberName = JSON.memberName;
        friend.memberId = JSON.memberId;
        friend.isOnline = JSON.isOnline;
        return friend;
    }
}
}