//
// $Id$

package com.threerings.msoy.item.web {

import com.threerings.util.Hashable;
import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * The base class for all digital items in the MSOY system.
 *
 * <p><em>Note:</em> this class and all derived classes are very strictly
 * limited in their contents as they must be translatable into JavaScript
 * ({@link IsSerializable}) and must work with the Presents streaming system
 * ({@link Streamable}) and must work with the JORA object persistence system.
 */
public /*abstract*/ class Item
    implements Hashable, Streamable
{
    /** This item's unique identifier. <em>Note:</em> this identifier is not
     * globally unique among all digital items. Each type of item has its own
     * identifier space. */
    public var itemId :int;

    /** A bit-mask of flags that we need to know about every digital item
     * without doing further database lookups or network requests. */
    public var flags :int;

    /** The member id of the member that owns this item. */
    public var ownerId :int;

    /**
     * This is used to map {@link Item} concrete classes to ItemEnum values. We
     * cannot simply reference the ItemEnum itself because item classes must be
     * translatable to JavaScript which doesn't support enums. So be sure to
     * properly wire things up when creating a new concrete item class.
     */
    public function getType () :String
    {
        throw new Error("abstract");
    }

    /**
     * Returns the text that should be displayed under the thumbnail image
     * shown in a player's inventory.
     */
    public function getDescription () :String
    {
        throw new Error("abstract");
    }

    /**
     * Returns the path to a thumbnail image for this item.
     */
    public function getThumbnailPath () :String
    {
        return "/media/static/items/" + getType().toLowerCase() + ".png";
    }

    // from Hashable
    public function hashCode () :int
    {
        return itemId;
    }

    // from Hashable
    public function equals (other :Object) :Boolean
    {
        if (other is Item) {
            var that :Item = (other as Item);
            // cheap comparison first...
            return (this.itemId == that.itemId) &&
                (this.getType() === that.getType());
        }
        return false;
    }

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(itemId);
        out.writeByte(flags);
        out.writeInt(ownerId);
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        itemId = ins.readInt();
        flags = ins.readByte();
        ownerId = ins.readInt();
    }
}
}
