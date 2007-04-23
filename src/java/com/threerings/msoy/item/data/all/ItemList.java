//
// $Id$

package com.threerings.msoy.item.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.presents.dobj.DSet;

public class ItemList
    implements Streamable, IsSerializable, DSet.Entry
{
    /** The globally-unique identifier for this list. */
    public int listId;

    /** The memberId of the owner to whom this list belongs. */
    public int ownerId;

    /** The name of this list. */
    public String name;

    /** The actual items in this ItemList. IF null, indicates that this is
     * a summary entry for the ItemList that does not contain the actual content.
     * If an ItemList is truly empty, then this will be a zero-element array.
     */
    public ItemIdent[] items;

    // from DSet.Entry
    public Comparable getKey ()
    {
        // TODO: damn GWT
        return new Integer(listId);
    }
}
