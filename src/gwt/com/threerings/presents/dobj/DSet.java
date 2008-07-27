//
// $Id$

package com.threerings.presents.dobj;

import com.threerings.io.Streamable;

/**
 * An impostor for the real DSet class that exists to allow some GWT "over the wire" classes to
 * implement DSet.Entry.
 */
public class DSet<E extends DSet.Entry>
{
    public static interface Entry extends Streamable
    {
        Comparable getKey ();
    }
}
