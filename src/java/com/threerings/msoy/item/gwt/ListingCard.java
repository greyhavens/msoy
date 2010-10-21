//
// $Id$

package com.threerings.msoy.item.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.GroupName;
import com.threerings.orth.data.MediaDesc;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.money.data.all.Currency;
import com.threerings.util.Name;

/**
 * Contains a smidgen of information on an item.
 */
public class ListingCard implements IsSerializable
{
    /** The type of item listed. */
    public MsoyItemType itemType;

    /** The item's catalog identifier. */
    public int catalogId;

    /** The item's name. */
    public String name;

    /** This item's thumbnail media. */
    public MediaDesc thumbMedia;

    /** The brand for this listing, or null. */
    public GroupName brand;

    /** The creator of this item. */
    public MemberName creator;

    /** The item's description. */
    public String descrip;

    /** Is this item remixable? */
    public boolean remixable;

    /** The item's rating. */
    public float rating;

    /** The currency the price is in. */
    public Currency currency;

    /** The item's price. */
    public int cost;

    public Name getListedBy ()
    {
        return (brand != null) ? brand : creator;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof ListingCard) {
            ListingCard oc = (ListingCard)other;
            return oc.itemType == itemType && oc.catalogId == catalogId;
        }
        return false;
    }

    @Override // from Object
    public int hashCode ()
    {
        return itemType.toByte() << 24 + catalogId;
    }
}
