//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.Item;

/**
 * Contains top-level information for the Whirled Catalog.
 */
public class ShopData implements IsSerializable
{
    /** The number of featured items to be provided. */
    public static final int TOP_ITEM_COUNT = 5;

    /** The top-rated avatars. */
    public ListingCard[] topAvatars;

    /** The top-rated furniture. */
    public ListingCard[] topFurniture;

    /** A featured pet. */
    public ListingCard featuredPet;

    /** A featured toy. */
    public ListingCard featuredToy;
}
