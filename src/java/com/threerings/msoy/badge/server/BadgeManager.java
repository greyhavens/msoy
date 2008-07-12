//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.ArrayList;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;
import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.data.MemberObject;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.util.PersistingUnit;

@Singleton @EventThread
public class BadgeManager
{
    /**
     * Awards a badge of the specified type to the user if they don't already
     * have it.
     */
    public void awardBadge (MemberObject user, BadgeType badgeType,
        InvocationService.ResultListener listener)
    {
        if (!user.badges.containsBadge(badgeType)) {
            ArrayList<BadgeType> badgeList = new ArrayList<BadgeType>();
            badgeList.add(badgeType);
            this.awardBadges(user, badgeList, listener);
        }
    }

    /**
     * For each Badge type, awards the Badge to the user if the Badge's award conditions
     * have been met.
     */
    public void updateBadges (MemberObject user, InvocationService.ResultListener listener)
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
            this.awardBadges(user, newBadges, listener);
        }
    }

    protected void awardBadges (final MemberObject user, final ArrayList<BadgeType> newBadges,
        InvocationService.ResultListener listener)
    {
        _invoker.postUnit(new PersistingUnit("awardBadges", listener) {
            public void invokePersistent () throws PersistenceException {
                for (BadgeType badgeType : newBadges) {
                    // BadgeUtil.awardBadge handles putting the badge in the repository
                    // and publishing a member feed about the event
                    BadgeUtil.awardBadge(user, badgeType);
                }
            }
            public void handleSuccess () {
                // TODO something happens here?
            }
            protected String getFailureMessage () {
                StringBuilder builder = new StringBuilder("Failed to award badges: ");
                for (BadgeType badgeType : newBadges) {
                    builder.append(badgeType.name()).append(", ");
                }
                return builder.toString();
            }
        });
    }


    @Inject protected @MainInvoker Invoker _invoker;
}
