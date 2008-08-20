//
// $Id$

package com.threerings.msoy.item.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.io.Streamable;

/**
 * This is used to pass search criteria for retrieving the elements of an item list.
 *
 * @author mjensen
 */
public class ItemListQuery implements Streamable, IsSerializable
{
    public int listId;

    /**
     * The start index for returning results.
     */
    public int offset;

    /**
     * The number of results to return. If this is set to the default of 0, then all matching
     * results will be returned.
     */
    public int count;

    /**
     * Whether to return result is descending order.
     */
    public boolean descending;

    /**
     * An optional field to limit the query results to a particular type of item.
     */
    public byte itemType = Item.NOT_A_TYPE;

    /**
     * This indicates whether or not the service should return the total number of items this query
     * would return if the <code>count</field> were not set.
     */
    public boolean needsCount;

    /**
     * Meant for deserialization purposes only. The listId must be set before the query can actually
     * find a list.
     */
    public ItemListQuery ()
    {
    }

    public ItemListQuery (int listId)
    {
        this.listId = listId;
    }

    public String toString ()
    {
        return "listId=" + listId + " offset=" + offset + " count=" + count + " descending="
            + descending + " itemType=" + itemType + " needsCount=" + needsCount;
    }
}
