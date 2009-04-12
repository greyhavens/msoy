//
// $Id$

package com.threerings.msoy.party.data;

import com.samskivert.util.RandomUtil;

import com.threerings.io.SimpleStreamableObject;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains general info about a party for the party board.
 */
public class PartyBoardInfo extends SimpleStreamableObject
    implements Comparable<PartyBoardInfo> // server only
{
    /** The immutable info. */
    public PartySummary summary;

    /** The mutable info. */
    public PartyInfo info;

    /** Mister Unserializable. */
    public PartyBoardInfo ()
    {
    }

    public PartyBoardInfo (PartySummary summary, PartyInfo info)
    {
        this.summary = summary;
        this.info = info;
    }

    /**
     * Compute the score for this party (server only).
     */
    public void computeScore (MemberObject member)
    {
        // start by giving every party a random score between 0 and 1
        float score = RandomUtil.rand.nextFloat();
        // add their rank in the group. (0, 1, or 2)
        score += member.getGroupRank(summary.group.getGroupId());
        // add 3 if their friend is leading the party. (To make it more important than groups)
        if (member.isFriend(info.leaderId)) {
            score += 3;
        }
        // now, each party is in a "band" determined by group/friend, and then has a random
        // position within that band.
        _score = score;
    }

    // from Comparable
    public int compareTo (PartyBoardInfo that)
    {
        return Float.compare(that._score, this._score); // order is reversed- higher scores at top
    }

    /** A calculated score for comparison purposes, only used on the server. */
    protected transient float _score;
}
