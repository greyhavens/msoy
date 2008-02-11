//
// $Id$

package com.threerings.msoy.world.data;

import com.threerings.msoy.item.data.all.ItemIdent;

/**
 * A reference to a controllable entity.
 */
public class ControllableEntity extends Controllable
{
    public ControllableEntity (ItemIdent ident)
    {
        _ident = ident;
    }
    
    public Comparable getKey ()
    {
        return _ident;
    }
    
    protected ItemIdent _ident;
}