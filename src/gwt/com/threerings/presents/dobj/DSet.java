//
// $Id$

package com.threerings.presents.dobj;

/**
 * This is an impostor for the real DSet class that exists to allow some GWT "over the wire"
 * classes to implement DSet.Entry.
 */
public class DSet
{
    public static interface Entry
    {
        public Comparable getKey ();
    }
}
