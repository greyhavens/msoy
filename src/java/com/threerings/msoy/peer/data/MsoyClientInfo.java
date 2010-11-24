//
// $Id$

package com.threerings.msoy.peer.data;

import com.threerings.crowd.peer.data.CrowdClientInfo;

import com.threerings.msoy.data.all.MemberName;

/**
 * Contains information on a player logged into one of our peer servers.
 */
public class MsoyClientInfo extends CrowdClientInfo
{
    /** Returns this member's unique identifier. */
    public int getMemberId ()
    {
        return ((MemberName)visibleName).getId();
    }
}
