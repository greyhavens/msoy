//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.badge.data.all.Badge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.gwt.StampCategory;

import com.threerings.msoy.data.all.Award;
import com.threerings.msoy.data.all.GroupName;

public class PassportData
    implements IsSerializable
{
    /** The account name for the stamps we're currently displaying **/
    public String stampOwner;

    /** The set of badges that this player has earned. */
    public Map<StampCategory, List<Badge>> stamps;

    /** The set of group medals that this player has earned. */
    public Map<GroupName, List<Award>> medals;

    /** The list of groups in the medals map that are official. */
    public List<GroupName> officialGroups;

    /** The set of badges this player has available, but not earned. */
    public List<InProgressBadge> nextBadges;
}
