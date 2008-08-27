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
    public PriceKey (final int memberId, final ItemIdent item)
    {
        _memberId = memberId;
        _itemId = item.itemId;
        _itemType = item.type;
    }
    
    public int getMemberId ()
    {
        return _memberId;
    }
    
    public ItemIdent getItem ()
    {
        return new ItemIdent(_itemType, _itemId);
    }

    @Override
    public int hashCode ()
    {
        final int prime = 31;
        int result = 1;
        result = prime * result + _itemId;
        result = prime * result + _memberId;
        return result;
    }

    @Override
    public boolean equals (final Object obj)
    {
        if (this == obj) return true;
        if (obj == null || (getClass() != obj.getClass())) {
            return false;
        }
        final PriceKey that = (PriceKey)obj;
        return (this._itemId == that._itemId) && (this._itemType == that._itemType) &&
            (this._memberId == that._memberId);
    }
    
    private final int _memberId;
    private final int _itemId;
    private final byte _itemType;
}
