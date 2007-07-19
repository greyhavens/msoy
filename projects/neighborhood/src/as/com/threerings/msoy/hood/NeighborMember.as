//
// $Id$

package com.threerings.msoy.hood {

import com.adobe.serialization.json.*;

/**
 * Represents a single member in a neighborhood: their name and id, and whether or not they are
 * online. This class will most likely fill in with more data soon.
 */
public class NeighborMember extends Neighbor
{
    /** The member's id. */
    public var memberId :Number;

    /** Whether or not the member is currently online. */
    public var isOnline :Boolean;

    /** When this member was created. */
    public var created :Date;

    /** The number of sessions this member has had. */
    public var sessions :int;

    /** The total number of minutes of this member's online sessions. */
    public var sessionMinutes :int;

    /** The last time this member was logged on. */
    public var lastSession :Date;

    /**
     * Instantiate and populate a {@link NeighborMember} give a JSON configuration.
     */
    public static function fromJSON (JSON: Object) :NeighborMember
    {
        var member :NeighborMember = new NeighborMember();
        if (JSON.id == null) {
            throw new Error("Missing id in JSON");
        }
        Neighbor.fromJSON(member, JSON);
        member.memberId = JSON.id;
        member.isOnline = JSON.isOnline;
        if (JSON.created != null) {
            member.created = new Date();
            member.created.time = JSON.created;
        }
        member.sessions = JSON.sNum;
        member.sessionMinutes = JSON.sMin;
        if (JSON.lastSess != null) {
            member.lastSession = new Date();
            member.lastSession.time = JSON.lastSess;
        }
        return member;
    }
}
}
