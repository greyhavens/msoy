//
// $Id$

package com.threerings.msoy.server;

import java.util.List;

import com.threerings.util.StreamableArrayIntSet;

import com.threerings.crowd.server.BodyLocal;
import com.threerings.stats.data.StatSet;

import com.threerings.msoy.data.PlayerMetrics;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.EarnedBadgeSet;
import com.threerings.msoy.badge.data.InProgressBadgeSet;
import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.room.data.EntityMemoryEntry;
import com.threerings.msoy.notify.data.Notification;

/**
 * Contains server-side only information for a member.
 */
public class MemberLocal extends BodyLocal
{
    /** The number of non-idle seconds that have elapsed in this member's session. When the member
     * is forwarded between servers, this value is incremented by the time they spent on the server
     * from which they are departing. */
    public int sessionSeconds;

    /** Statistics tracked for this player. */
    public StatSet stats;

    /** Metrics tracked for this player. */
    public PlayerMetrics metrics = new PlayerMetrics();

    /** The set of badges that this player owns. */
    public EarnedBadgeSet badges;

    /** The version of the "Badges set" that this user has seen. If this is behind
     * BadgeType.VERSION, then the member's InProgressBadges will be recomputed. */
    public short badgesVersion;

    /** The set of badges that this player is working towards. */
    public InProgressBadgeSet inProgressBadges;

    /** A list of notifications that will be dispatched when the client's NotificationDirector asks
     * for them. Will be null once the deferred notifications have been dispatched. */
    public List<Notification> deferredNotifications;

    /** Rooms we've visited during our current Whirled Tour. */
    public StreamableArrayIntSet touredRooms;

    /** The memories of the member's avatar. */
    public List<EntityMemoryEntry> memories;

    /**
     * Adds an EarnedBadge to the member's BadgeSet (or updates the existing badge if the badge
     * level has increased) and dispatches an event indicating that a new badge was awarded.
     *
     * @return true if the badge was updated in the member's BadgeSet, and false otherwise.
     */
    public boolean badgeAwarded (EarnedBadge badge)
    {
        boolean added = badges.addOrUpdateBadge(badge);

        // remove this badge's associated InProgressBadge if the badge's highest level has
        // been reached
        BadgeType badgeType = BadgeType.getType(badge.badgeCode);
        if (badgeType != null && badge.level >= badgeType.getNumLevels() - 1) {
            inProgressBadges.removeBadge(badge.badgeCode);
        }

        return added;
    }

    /**
     * Adds an InProgressBadge to the member's in-progress badge set (or updates the existing badge
     * if the badge level has increased).
     *
     * @return true if the badge was updated in the member's in-progress badge set, and false
     * otherwise.
     */
    public boolean inProgressBadgeUpdated (InProgressBadge badge)
    {
        return inProgressBadges.addOrUpdateBadge(badge);
    }
}
