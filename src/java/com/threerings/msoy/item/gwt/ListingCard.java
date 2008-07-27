//
// $Id$

package com.threerings.msoy.item.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Contains a smidgen of information on an item.
 */
public class ListingCard implements IsSerializable
{
    /** The type of item listed. */
    public byte itemType;

    /** The item's catalog identifier. */
    public int catalogId;

    /** The item's name. */
    public String name;

    /** This item's thumbnail media. */
    public MediaDesc thumbMedia;

    /** The creator of this item. */
    public MemberName creator;

    /** The item's description. */
    public String descrip;

    /** Is this item remixable? */
    public boolean remixable;

    /** The item's rating. */
    public float rating;

    /** The item's flow cost. */
    public int flowCost;

    /** The item's gold cost. */
    public int goldCost;

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof ListingCard) {
            ListingCard oc = (ListingCard)other;
            return oc.itemType == itemType && oc.catalogId == catalogId;
        }
        return false;
    }

    /**
     * Returns this listing's item thumbnail media or the default.
     */
    public MediaDesc getThumbnailMedia ()
    {
        return thumbMedia == null ? Item.getDefaultThumbnailMediaFor(itemType) : thumbMedia;
    }
}
