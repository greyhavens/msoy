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

    /** Recruitment constant indicating that only members of the party's group may join. */
    public static const RECRUITMENT_GROUP :int = 1;

    /** Recruitment constant indicating nobody but those invited by the leader may join. */
    public static const RECRUITMENT_CLOSED :int = 2;

    /** Number of recruitment types a party can choose from. Update this when you add a new type. */
    public static const RECRUITMENT_COUNT :int = 3;

    /** The maximum length of a party name. */
    public static const MAX_NAME_LENGTH :int = 32;

    public static const GAME_STATE_NONE :int = 0;
    public static const GAME_STATE_AVRG :int = 1;
    public static const GAME_STATE_LOBBY :int = 2;
    public static const GAME_STATE_INGAME :int = 3;

    public static const STATUS_TYPE_USER :int = 0;
    public static const STATUS_TYPE_SCENE :int = 1;
    public static const STATUS_TYPE_PLAYING :int = 2;
    public static const STATUS_TYPE_LOBBY :int = 3;

    /** Board mode for a normal party board. */
    public static const BOARD_NORMAL :int = 0;
    /** Board mode for a normal party board. */
    public static const BOARD_AWAITING_PLAYERS :int = 1;
    /** Board mode for a party board showing all friends, even when not leading. */
    public static const BOARD_FRIENDS :int = 2;
}
}
