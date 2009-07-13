//
// $Id: GroupMembership.java 15991 2009-04-13 17:21:33Z ray $

package com.threerings.msoy.group.gwt;

import java.util.ArrayList;
import java.util.List;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;

/**
 * Contains details about a brand.
 */
public class BrandDetail
    implements Streamable, IsSerializable
{
    public static final int MAX_SHARES = 5;

    /**
     * Holds information about a member's share in this brand.
     */
    public static class BrandShare
        implements Streamable, IsSerializable
    {
        public MemberName member;

        public int shares;

        /** Used when unserializing */
        public BrandShare ()
        {
        }

        public BrandShare (MemberName member, int shares)
        {
            this.member = member;
            this.shares = shares;
        }
    }

    /** The name of the brand's group. */
    public GroupName group;

    /** The members with a share in the brand. */
    public List<BrandShare> shareHolders;

    /** Used for unserializing. */
    public BrandDetail ()
    {
    }

    public BrandDetail (GroupName group)
    {
        this.group = group;
        this.shareHolders = new ArrayList<BrandShare>();
    }

    public int getShares (int memberId)
    {
        for (BrandShare share : shareHolders) {
            if (share.member.getMemberId() == memberId) {
                return share.shares;
            }
        }
        return 0;
    }

    public int getTotalShares ()
    {
        int total = 0;
        for (BrandShare share : shareHolders) {
            total += share.shares;
        }
        return total;
    }
}
