package com.threerings.msoy.data.all {

import com.threerings.util.Comparable;
import com.threerings.util.Hashable;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.MediaDesc;

/**
 * Represents a player's connection to another person of interest.
 */
public interface PeerEntry
    extends Comparable, DSet_Entry, Hashable
{
    function getMemberId () :int;
    function getName () :MemberName;
    function getPhoto () :MediaDesc;
}

}
