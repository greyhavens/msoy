//
// $Id$

package com.threerings.msoy.badge.data;

import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.presents.dobj.DSet;

/**
 * A distributed class containing {@link EarnedBadge} objects.
 */
public final class BadgeSet extends DSet<EarnedBadge>
{
    /** Creates a BadgeSet with the specified contents. */
    public BadgeSet (Iterable<EarnedBadge> source)
    {
        super(source);
    }

    /** Creates an empty BadgeSet. */
    public BadgeSet ()
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
     * Adds a badge to the BadgeSet.
     *
     * @return true if the badge was newly added, false if it already existed in the set.
     */
    public boolean addBadge (EarnedBadge badge)
    {
        return super.add(badge);
    }

    /**
     * Removes a badge from the BadgeSet.
     *
     * @return true if the badge was removed, false if it wasn't in the set.
     */
    public boolean removeBadge (EarnedBadge badge)
    {
        return super.remove(badge);
    }
}
