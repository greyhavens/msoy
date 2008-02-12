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
    
    public int compareTo (Controllable other)
    {
        if (other instanceof ControllableAVRGame) {
            return 1;
        }
        if (other instanceof ControllableEntity) {
            return _ident.compareTo(((ControllableEntity) other).getItemIdent());
        }
        throw new IllegalArgumentException("Unknown Controllable subclass: " + other.getClass());
    }
    
    public ItemIdent getItemIdent ()
    {
        return _ident;
    }
    
    protected ItemIdent _ident;
}