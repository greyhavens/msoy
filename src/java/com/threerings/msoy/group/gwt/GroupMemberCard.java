//
// $Id$

package com.threerings.msoy.group.gwt;

import com.threerings.msoy.web.data.MemberCard;

/**
 * Extends the {@link MemberCard} with information on a group member's rank.
 */
public class GroupMemberCard extends MemberCard
{
    /** The member's rank in the group. */
    public byte rank;

    /** When this member's rank was assigned in millis since the epoch. */
    public long rankAssigned;
}
