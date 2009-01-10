//
// $Id$

package com.threerings.msoy.party.data;

/**
 * Party constants.
 */
public class PartyCodes
{
    /** Recruitment constant indicating anyone may join the party. */
    public static final byte RECRUITMENT_OPEN = 0;

    /** Recruitment constant indicating that only members of the party's group may join. */
    public static final byte RECRUITMENT_GROUP = 1;

    /** Recruitment constant indicating nobody but those invited by the leader may join. */
    public static final byte RECRUITMENT_CLOSED = 2;

    /** The maximum length of a party name. */
    public static final int MAX_NAME_LENGTH = 32;

    /** The maximum size of a party. */
    public static final int MAX_PARTY_SIZE = 50;

    /** Error codes. */
    public static final String E_NO_SUCH_PARTY = "e.no_such_party";
    public static final String E_PARTY_FULL = "e.party_full";
    public static final String E_PARTY_CLOSED = "e.party_closed";
    public static final String E_ALREADY_IN_PARTY = "e.already_in_party";
    public static final String E_GROUP_MGR_REQUIRED = "e.group_mgr_req";
    public static final String E_CANT_INVITE_CLOSED = "e.cant_invite_closed";
}
