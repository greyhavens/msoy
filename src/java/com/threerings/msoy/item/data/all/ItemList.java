//
// $Id$

package com.threerings.msoy.item.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.presents.dobj.DSet;

/**
 * Represents a list of items created by a user.
 */
public class ItemList
    implements Streamable, IsSerializable, DSet.Entry
{
    /** The summary info about this list. */
    public ItemListInfo info;

    /** The actual items in this ItemList. IF null, indicates that this is
     * a summary entry for the ItemList that does not contain the actual content.
     * If an ItemList is truly empty, then this will be a zero-element array.
     */
    public Item[] items;

    // from DSet.Entry
    public Comparable<?> getKey ()
    {
        return info.getKey();
    }
}
