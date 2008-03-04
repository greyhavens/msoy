//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.ItemIdent;
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

    /** The item's rating. */
    public float rating;

    /** The item's flow cost. */
    public int flowCost;

    /** The item's gold cost. */
    public int goldCost;

    /**
     * Returns this listing's item thumbnail media or the default.
     */
    public MediaDesc getThumbnailMedia ()
    {
        return thumbMedia == null ? Item.getDefaultThumbnailMediaFor(itemType) : thumbMedia;
    }
}
