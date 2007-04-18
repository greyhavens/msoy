//
// $Id$

package com.threerings.msoy.item.data.all {

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.msoy.data.all.MemberName;

/**
 * This class supplies detailed information for an item, some of which is
 * relative to a given member.
 *
 * <p><em>Note:</em> this class and all derived classes are very strictly
 * limited in their contents as they must be translatable into JavaScript
 * ({@link IsSerializable}) and must work with the Presents streaming system
 * ({@link Streamable}).
 */
public class ItemDetail
    implements Streamable
{
    /** The Item of which we're a Detail. */
    public var item :Item;

    /** A display-friendly expansion of Item.creatorId. */
    public var creator :MemberName;

    /** A display-friendly expansion of Item.ownerId, or null. */
    public var owner :MemberName;

    /** The item's rating given by the member specified in the request. */
    public var memberRating :int;

    public function ItemDetail ()
    {
    }

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        item = (ins.readObject() as Item);
        creator = (ins.readObject() as MemberName);
        owner = (ins.readObject() as MemberName);
        memberRating = ins.readByte();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeObject(item);
        out.writeObject(creator);
        out.writeObject(owner);
        out.writeByte(memberRating);
    }
}
}
