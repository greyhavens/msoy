//
// $Id$

package com.threerings.msoy.money.server.impl;

import java.io.Serializable;

import com.google.common.base.Preconditions;
import com.samskivert.util.ObjectUtil;
import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * Key identifying a secured price.  The key is made up of the member ID and item ID.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
class PriceKey
    implements Serializable
{
    public PriceKey (final int memberId, final ItemIdent itemIdent)
    {
        Preconditions.checkNotNull(itemIdent);

        _memberId = memberId;
        _ident = itemIdent;
    }
    
    public int getMemberId ()
    {
        return _memberId;
    }
    
    public ItemIdent getItem ()
    {
        return _ident;
    }

    @Override
    public int hashCode ()
    {
        int hash = _memberId;
        hash = 31 * hash + _ident.hashCode();
        return hash;
    }

    @Override
    public boolean equals (final Object obj)
    {
        if (this == obj) return true;
        if (obj == null || (getClass() != obj.getClass())) {
            return false;
        }
        final PriceKey that = (PriceKey)obj;
        return (_memberId == that._memberId) && ObjectUtil.equals(_ident, that._ident);
    }
    
    private final int _memberId;
    private final ItemIdent _ident;
}
