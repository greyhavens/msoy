//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.ArrayList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;
import com.samskivert.jdbc.WriteOnlyUnit;
import com.samskivert.util.Invoker;
import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.badge.data.EarnedBadge;
import com.threerings.msoy.data.MemberObject;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;

@Singleton @EventThread
public class BadgeManager
{
    /**
     * Awards a badge of the specified type to the user if they don't already
     * have it.
     */
    public void awardBadge (MemberObject user, BadgeType badgeType)
    {
        if (!user.badges.containsBadge(badgeType)) {
            ArrayList<BadgeType> badgeList = new ArrayList<BadgeType>();
            badgeList.add(badgeType);
            this.awardBadges(user, badgeList);
        }
    }

    /**
     * For each Badge type, awards the Badge to the user if the Badge's award conditions
     * have been met.
     */
    public void updateBadges (MemberObject user)
    {
        // guests are not awarded badges
        if (user.isGuest()) {
            return;
        }

        // iterate the list of badges to see if the player has won any new ones
        ArrayList<BadgeType> newBadges = null;
        for (BadgeType badgeType : BadgeType.values()) {
            if (!user.badges.containsBadge(badgeType) && badgeType.hasEarned(user)) {
                if (newBadges == null) {
                    newBadges = new ArrayList<BadgeType>();
                }
                newBadges.add(badgeType);
            }
        }

        if (newBadges != null) {
            this.awardBadges(user, newBadges);
        }
    }

    protected void awardBadges (final MemberObject user, final ArrayList<BadgeType> badgeTypes)
    {
        final long whenEarned = System.currentTimeMillis();

        // create badges and stick them in the MemberObject
        final ArrayList<EarnedBadge> badges = createBadges(badgeTypes, whenEarned);
        for (EarnedBadge badge : badges) {
            user.badges.addBadge(badge);
        }

        // stick the badges in the database
        _invoker.postUnit(new WriteOnlyUnit("awardBadges") {
            public void invokePersist () throws PersistenceException {
                for (BadgeType badgeType : badgeTypes) {
                    // BadgeUtil.awardBadge handles putting the badge in the repository
                    // and publishing a member feed about the event
                    BadgeUtil.awardBadge(user, badgeType, whenEarned);
                }
            }
            public void handleFailure (Exception error) {
                // rollback the changes to the user's BadgeSet
                for (EarnedBadge badge : badges) {
                    user.badges.removeBadge(badge);
                }

                super.handleFailure(error);
            }
            protected String getFailureMessage () {
                StringBuilder builder = new StringBuilder("Failed to award badges: ");
                for (BadgeType badgeType : badgeTypes) {
                    builder.append(badgeType.name()).append(", ");
                }
                return builder.toString();
            }
        });
    }

    protected static ArrayList<EarnedBadge> createBadges (final ArrayList<BadgeType> badgeTypes,
        long whenEarned)
    {
        ArrayList<EarnedBadge> badges = new ArrayList<EarnedBadge>(badgeTypes.size());
        for (BadgeType type : badgeTypes) {
            badges.add(new EarnedBadge(type, whenEarned));
        }

        return badges;
    }

    @Inject protected @MainInvoker Invoker _invoker;
}
