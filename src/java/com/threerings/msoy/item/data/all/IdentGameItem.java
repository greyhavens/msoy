//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * A base class for game items with an identifier (level packs, etc.).
 */
public abstract class IdentGameItem extends GameItem
{
    /** The maximum length of game identifiers (used by level and item packs and trophies). */
    public static final int MAX_IDENT_LENGTH = 32;

    /** An identifier for this sub-item, used to identify it from code. */
    public String ident;

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(ident, MAX_IDENT_LENGTH);
    }
}
