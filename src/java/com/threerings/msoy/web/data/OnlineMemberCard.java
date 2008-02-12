//
// $Id$

package com.threerings.msoy.web.data;

/**
 * Extends {@link MemberCard} with information about where a member is right now.
 */
public class OnlineMemberCard extends MemberCard
{
    /** A {@link #placeType} constant. */
    public static final byte NO_PLACE = 0;

    /** A {@link #placeType} constant. */
    public static final byte ROOM_PLACE = 1;

    /** A {@link #placeType} constant. */
    public static final byte GAME_PLACE = 2;

    /** The type of place occupied by this member if any. */
    public byte placeType;

    /** The id of the place occupied by this member. */
    public int placeId;

    /** The name of the place occupied by this member. */
    public String placeName;
}
