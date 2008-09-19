//
// $Id$

package com.threerings.msoy.badge.server;

import java.util.Iterator;

import com.threerings.msoy.data.MemberObject;
import com.threerings.stats.data.Stat;
import com.threerings.stats.data.StatSet;

/**
 * Stats in Whirled are often associated with badges; when such a stat is updated, ServerStatSet
 * takes care of updating its associated badges.
 */
public class ServerStatSet extends StatSet
{
    /** Creates a stat set with the specified contents. */
    public ServerStatSet (Iterator<Stat> contents, BadgeManager badgeMan, MemberObject memObj)
    {
        super(contents);
        init(badgeMan, memObj);
    }

    /** Creates an empty stat set (for deserialization purposes). */
    public ServerStatSet ()
    {
    }

    /**
     * Called to initialize an instance unserlialized from the network.
     */
    public void init (BadgeManager badgeMan, MemberObject memObj)
    {
        _badgeMan = badgeMan;
        _memObj = memObj;
    }

    @Override // from StatSet
    protected void addStat (Stat stat, boolean syncingWithRepo)
    {
        super.addStat(stat, syncingWithRepo);
        if (!syncingWithRepo) {
            _badgeMan.updateBadges(_memObj);
        }
    }

    @Override // from StatSet
    protected void updateStat (Stat stat, boolean syncingWithRepo)
    {
        super.updateStat(stat, syncingWithRepo);
        if (!syncingWithRepo) {
            _badgeMan.updateBadges(_memObj);
        }
    }

    protected transient BadgeManager _badgeMan;
    protected transient MemberObject _memObj;
}
