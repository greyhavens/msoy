//
// $Id$

package com.threerings.msoy.admin.gwt;

import com.google.common.collect.ComparisonChain;
import com.google.common.primitives.Ints;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Provides information on Whirled "entries".
 */
public class EntrySummary
    implements IsSerializable, Comparable<EntrySummary>
{
    /** The entry vector in question. */
    public String vector;

    /** The total number of entries in the period. */
    public int entries;

    /** The number of entries in the period that played. */
    public int played;

    /** The number of entries in the period that registered. */
    public int registrations;

    /** The number of entries in the period that returned. */
    public int returns;

    // from interface Comparable<EntrySummary>
    public int compareTo (EntrySummary other)
    {
        return Ints.compare(entries, other.entries);
    }
}
