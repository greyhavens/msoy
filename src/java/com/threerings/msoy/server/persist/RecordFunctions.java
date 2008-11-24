//
// $Id$

package com.threerings.msoy.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;

/**
 * General purpose {@link Function}s that operate on {@link PersistentRecord}s.
 */
public class RecordFunctions
{
    /**
     * Extracts an integer key from a record's {@link Key}.
     */
    public static <T extends PersistentRecord> Function<Key<T>,Integer> getIntKey ()
    {
        return new Function<Key<T>,Integer>() {
            public Integer apply (Key<T> key) {
                return (Integer)key.getValues()[0];
            }
        };
    }
}
