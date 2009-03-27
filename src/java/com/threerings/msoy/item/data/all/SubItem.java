//
// $Id$

package com.threerings.msoy.item.data.all;

/**
 * A base class for sub-items (level packs, etc.).
 */
public abstract class SubItem extends Item
{
    /** The maximum length of game identifiers (used by level and item packs and trophies). */
    public static final int MAX_IDENT_LENGTH = 32;

    /** The identifier of the suite to which this sub-item belongs. This is either the negated
     * catalogId of the listing for the primary item (if this and the primary item are listed) or
     * the item id of the primary item (if this and the primary item are not listed). */
    public int suiteId;

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
