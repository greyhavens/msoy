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

    /**
     * Returns the type of this item's suite master or {@link #NOT_A_TYPE} if this type of item is
     * not a subtype of some other item.
     */
    public abstract byte getSuiteMasterType ();

    @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && nonBlank(ident, MAX_IDENT_LENGTH);
    }
}
