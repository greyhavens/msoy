//
// $Id$

package com.threerings.msoy.admin.gwt;

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

    /** The number of entries in the period that registered. */
    public int registrations;

    // from interface Comparable<EntrySummary>
    public int compareTo (EntrySummary other)
    {
        return (entries > other.entries) ? 1 : ((entries < other.entries) ? -1 : 0);
    }
}
