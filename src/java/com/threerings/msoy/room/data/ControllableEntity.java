//
// $Id$

package com.threerings.msoy.room.data;

import com.threerings.msoy.data.MemberObject;
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
        if (other instanceof ControllableEntity) {
            return _ident.compareTo(((ControllableEntity) other).getItemIdent());
        }
        throw new IllegalArgumentException("Unknown Controllable subclass: " + other.getClass());
    }

    public ItemIdent getItemIdent ()
    {
        return _ident;
    }

    @Override
    public boolean isControllableBy (MemberObject member)
    {
        // any member can control an entity
        return true;
    }

    protected ItemIdent _ident;
}
