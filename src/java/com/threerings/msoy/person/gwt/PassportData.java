//
// $Id$

package com.threerings.msoy.person.gwt;

import java.util.List;
import java.util.Map;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.badge.data.all.EarnedBadge;
import com.threerings.msoy.badge.data.all.InProgressBadge;
import com.threerings.msoy.badge.gwt.StampCategory;

public class PassportData
    implements IsSerializable
{
    /** The account name for the stamps we're currently displaying **/
    public String stampOwner;

    /** The set of badges that this player has earned. */
    // TODO: this is going to have to map to a list of some interface that badges implements, along
    // with trophies, agent-granted goodies, stamps, etc.
    public Map<StampCategory, List<EarnedBadge>> stamps;

    /** The set of badges this player has available, but not earned. */
    public List<InProgressBadge> nextBadges;
}
