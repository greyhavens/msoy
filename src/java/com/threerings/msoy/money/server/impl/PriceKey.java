//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.io.Serializable;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Key identifying a secured price.  The key is made up of the member ID and item ID.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 */
class PriceKey implements Serializable
{
    public PriceKey (final int memberId, final ItemIdent item) {
        this.memberId = memberId;
        this.itemId = item.itemId;
        this.itemType = item.type;
    }
    
    public int getMemberId ()
    {
        return memberId;
    }
    
    public ItemIdent getItem ()
    {
        return new ItemIdent(itemType, itemId);
    }

    @Override
    public int hashCode ()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + itemId;
        result = prime * result + memberId;
        return result;
    }

    @Override
    public boolean equals (final Object obj)
    {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final PriceKey other = (PriceKey)obj;
        if (itemId != other.itemId)
            return false;
        if (itemType != other.itemType)
            return false;
        if (memberId != other.memberId)
            return false;
        return true;
    }
    
    private final int memberId;
    private final int itemId;
    private final byte itemType;
}
