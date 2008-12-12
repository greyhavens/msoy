//
// $Id$

package com.threerings.msoy.party.data {

/**
 * Party constants.
 */
public class PartyCodes
{
    /** Recruitment constant indicating anyone may join the party. */
    public static const RECRUITMENT_OPEN :int = 0;

    /** Recruitment constant indicating nobody but those invited by the leader may join. */
    public static const RECRUITMENT_CLOSED :int = 1;

    /** Recruitment constant indicating that only members of the party's group may join. */
    public static const RECRUITMENT_GROUP :int = 2;

    /** Number of recruitment types a party can choose from. Update this when you add a new type. */
    public static const RECRUITMENT_COUNT :int = 3;

    /** The maximum length of a party name. */
    public static const MAX_NAME_LENGTH :int = 32;
}
}
