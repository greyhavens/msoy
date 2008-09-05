//
// $Id$

package com.threerings.msoy.money.server;

import java.io.Serializable;

import com.google.common.base.Preconditions;
import com.samskivert.util.ObjectUtil;
import com.threerings.msoy.item.data.all.CatalogIdent;

/**
 * Key identifying a secured price.  The key is made up of the member id and catalog id.
 * 
 * @author Kyle Sampson <kyle@threerings.net>
 * @author Ray Greenwell <ray@threerings.net>
 */
public class PriceKey
    implements Serializable
{
    public PriceKey (final int memberId, final CatalogIdent item)
    {
        Preconditions.checkNotNull(item);

        _memberId = memberId;
        _ident = item;
    }
    
    public int getMemberId ()
    {
        return _memberId;
    }
    
    public CatalogIdent getCatalogIdent ()
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
    
    protected final int _memberId;
    protected final CatalogIdent _ident;
}
