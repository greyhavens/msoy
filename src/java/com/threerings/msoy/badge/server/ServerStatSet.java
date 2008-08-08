//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.Iterator;

import com.threerings.msoy.badge.data.BadgeType;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.StatType;
import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;

/**
 * Stats in Whirled are often associated with badges; when such a stat is updated, ServerStatSet
 * takes care of updating its associated badges.
 */
public class ServerStatSet extends StatSet
{
    /** Creates a stat set with the specified contents. */
    public ServerStatSet (Iterator<Stat> contents, MemberObject memObj)
    {
        super(contents);
        _memObj = memObj;
    }

    /** Creates an empty stat set (for deserialization purposes). */
    public ServerStatSet ()
    {
    }

    public void setMemberObject (MemberObject memObj)
    {
        _memObj = memObj;
    }

    @Override // from StatSet
    protected void addStat (Stat stat)
    {
        // TODO - remove this when Passport goes live
        if (!DeploymentConfig.devDeployment) {
            return;
        }

        super.addStat(stat);
        updatedAssociatedBadges(stat.getType());
    }

    @Override // from StatSet
    protected void updateStat (Stat stat)
    {
        // TODO - remove this when Passport goes live
        if (!DeploymentConfig.devDeployment) {
            return;
        }

        super.updateStat(stat);
        updatedAssociatedBadges(stat.getType());
    }

    /** Called when a Stat has been modified to update any associated badges for the member. */
    protected void updatedAssociatedBadges (Stat.Type type)
    {
        if (!(type instanceof StatType)) {
            return;
        }

        StatType statType = (StatType) type;
        for (BadgeType badgeType : BadgeType.values()) {
            if (badgeType.getRelevantStat() == statType) {
                updateBadge(badgeType);
            }
        }
    }

    /** Called to update a single badge type when its associated StatType is modified. */
    protected void updateBadge (BadgeType badgeType)
    {
        // TODO
    }

    protected transient MemberObject _memObj;
}
