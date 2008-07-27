package com.threerings.msoy.data.all;

import com.threerings.presents.dobj.DSet;


/**
 * Represents a player's connection to another person of interest.
 */
public interface PeerEntry
    extends Comparable, DSet.Entry
{
    /** The member ID of the peer. */
    public int getMemberId ();

    /** The peer's MemberName. */
    public MemberName getName ();

    /** The peer's mugshot. */
    public MediaDesc getPhoto ();
}
