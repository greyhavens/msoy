//
// $Id$

package com.threerings.msoy.badge.data;

import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.presents.dobj.DSet;

/**
 * A distributed class containing {@link EarnedBadge} objects.
 */
public final class EarnedBadgeSet extends DSet<EarnedBadge>
{
    /** Creates a BadgeSet with the specified contents. */
    public EarnedBadgeSet (Iterable<EarnedBadge> source)
    {
        super(source);
    }

    /** Creates an empty BadgeSet. */
    public EarnedBadgeSet ()
    {
    }

    /** Returns true if the set contains a badge of the given type. */
    public boolean containsBadge (BadgeType type)
    {
        return this.containsBadge(type.getCode());
    }

    /** Returns true if the set contains a badge of the given type. */
    public boolean containsBadge (int badgeCode)
    {
        EarnedBadge badge = new EarnedBadge();
        badge.badgeCode = badgeCode;
        return this.contains(badge);
    }

    /**
     * Adds a new badge to the BadgeSet, or updates an existing badge if the specified badge
     * already exists and the specified level is higher than the existing level.
     *
     * @return true if the badge was newly added or updated, false otherwise.
     */
    public boolean addOrUpdateBadge (EarnedBadge badge)
    {
        EarnedBadge existingBadge = this.get(badge.badgeCode);
        if (existingBadge == null) {
            super.add(badge);
            return true;
        } else if (badge.level > existingBadge.level) {
            existingBadge.level = badge.level;
            existingBadge.whenEarned = badge.whenEarned;
            return true;
        }

        return false;
    }

    /**
     * Removes a badge from the BadgeSet.
     *
     * @return true if the badge was removed, false if it wasn't in the set.
     */
    public boolean removeBadge (int badgeCode)
    {
        return (super.removeKey(badgeCode) != null);
    }
}
