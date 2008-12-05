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


    /** Error codes. */
    public static final String E_NO_SUCH_PARTY = "e.no_such_party";
}
