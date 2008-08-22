//
// $Id$

package com.threerings.msoy.item.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.item.data.all.Item;

/**
 * Contains information needed to query the catalog.
 */
public class CatalogQuery
    implements IsSerializable
{
    /** Indicates the order in which catalog search results should be returned. */
    public static final byte SORT_BY_RATING = 1;
    /** Indicates the order in which catalog search results should be returned. */
    public static final byte SORT_BY_LIST_DATE = 2;
    /** Indicates the order in which catalog search results should be returned. */
    public static final byte SORT_BY_SATISFACTION = 3;
    /** Indicates the order in which catalog search results should be returned. */
    public static final byte SORT_BY_PRICE_ASC = 4;
    /** Indicates the order in which catalog search results should be returned. */
    public static final byte SORT_BY_PRICE_DESC = 5;
    /** Indicates the order in which catalog search results should be returned. */
    public static final byte SORT_BY_PURCHASES = 6;
    /** Indicates the order in which catalog search results should be returned. */
    public static final byte SORT_BY_NEW_AND_HOT = 7;
    /** Indicates the order in which catalog search results should be returned. */
    public static final byte SORT_BY_REMIXABLE = 8;

    /** The type of item being browsed. */
    public byte itemType = Item.NOT_A_TYPE;

    /** The order in which to return the catalog listings. */
    public byte sortBy = SORT_BY_NEW_AND_HOT;

    /** The tag by which to filter the listings or null. */
    public String tag;

    /** A user supplied search string, or null. */
    public String search;

    /** The member id of the creator whose listings we want exclusively to see, or 0. */
    public int creatorId;

    public CatalogQuery ()
    {
    }

    public CatalogQuery (CatalogQuery source)
    {
        this.itemType = source.itemType;
        this.sortBy = source.sortBy;
        this.tag = source.tag;
        this.search = source.search;
        this.creatorId = source.creatorId;
    }

    @Override // from Object
    public int hashCode ()
    {
        return itemType ^ sortBy ^ (tag == null ? 0 : tag.hashCode()) ^
            (search == null ? 0 : search.hashCode()) ^ creatorId;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        CatalogQuery oquery = (CatalogQuery)other;
        return (itemType == oquery.itemType && sortBy == oquery.sortBy &&
                (tag == null ? (oquery.tag == null) : tag.equals(oquery.tag)) &&
                (search == null ? (oquery.search == null) : search.equals(oquery.search)));
    }

    @Override // from Object
    public String toString ()
    {
        return "[type=" + itemType + ", sort=" + sortBy + ", tag=" + tag +
            ", search=" + search + ", creator=" + creatorId + "]";
    }
}
