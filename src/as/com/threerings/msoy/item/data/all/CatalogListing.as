//
// $Id$

package com.threerings.msoy.item.data.all {

import java.util.Date;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;

import com.threerings.msoy.data.all.MemberName;

/**
 * Represents a catalog listing of an item.
 */
public class CatalogListing
    implements Streamable
{
    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static const SORT_BY_NOTHING :int = 0;

    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static const SORT_BY_RATING :int = 1;

    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static const SORT_BY_LIST_DATE :int = 2;

    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static const SORT_BY_SATISFACTION :int = 3;

    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static const SORT_BY_PRICE_ASC :int = 4;

    /** A constant for requesting catalog search results to be returned in a certain order. */
    public static const SORT_BY_PRICE_DESC :int = 5;

    /** A constant that encodes the rarity of a listed item. */
    public static const RARITY_PLENTIFUL :int = 1;

    /** A constant that encodes the rarity of a listed item. */
    public static const RARITY_COMMON :int = 2;

    /** A constant that encodes the rarity of a listed item. */
    public static const RARITY_NORMAL :int = 3;

    /** A constant that encodes the rarity of a listed item. */
    public static const RARITY_UNCOMMON :int = 4;

    /** A constant that encodes the rarity of a listed item. */
    public static const RARITY_RARE :int = 5;

    /** The item being listed. */
    public var item :Item;

    /** The date on which the item was listed. */
    public var listedDate :Date;

    /** The creator of the item. */
    public var creator :MemberName;

    /** The amount of flow it costs to purchase this item, if it's listed, else zero. */
    public var flowCost :int;

    /** The amount of gold it costs to purchase this item, if it's listed, else zero. */
    public var goldCost :int;

    /** The rarity of this item, if it's listed, else zero. */
    public var rarity :int;

    public function CatalogListing ()
    {
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        item = (ins.readObject() as Item);
        listedDate = (ins.readObject() as Date);
        creator = (ins.readObject() as MemberName);
        flowCost = ins.readInt();
        goldCost = ins.readInt();
        rarity = ins.readInt();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(item);
        out.writeObject(listedDate);
        out.writeObject(creator);
        out.writeInt(flowCost);
        out.writeInt(goldCost);
        out.writeInt(rarity);
    }
}
}
