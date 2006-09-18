//
// $Id$

package com.threerings.msoy.item.web {

import flash.utils.ByteArray;

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

    /** The member id of the member that created this item. */
    public var creatorId :int;

    /** The member id of the member that owns this item. */
    public var ownerId :int;

    /** A hash code identifying the media used to display this item's thumbnail
     * representation. */
    public var thumbMediaHash :ByteArray;

    /** The MIME type of the thumbMediaHash media. */
    public var thumbMimeType :int;

    /** A hash code identifying the media used to display this item's furniture
     * representation. */
    public var furniMediaHash :ByteArray;

    /** The MIME type of the furniMediaHash media. */
    public var furniMimeType :int;

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

    /**
     * Returns a media descriptor for the media that should be used to display
     * our thumbnail representation.
     */
    public MediaDesc getThumbnailMedia ()
    {
        return (thumbMediaHash == null) ? getDefaultThumbnailMedia() :
            new MediaDesc(thumbMediaHash, thumbMimeType);
    }

    /**
     * Returns a media descriptor for the media that should be used to display
     * our furniture representation.
     */
    public MediaDesc getFurniMedia ()
    {
        return (furniMediaHash == null) ? getDefaultFurniMedia() :
            new MediaDesc(furniMediaHash, furniMimeType);
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
        out.writeInt(creatorId);
        out.writeInt(ownerId);
        out.writeField(thumbMediaHash);
        out.writeByte(thumbMimeType);
        out.writeField(furniMediaHash);
        out.writeByte(furniMimeType);
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        itemId = ins.readInt();
        flags = ins.readByte();
        creatorId = ins.readInt();
        ownerId = ins.readInt();
        thumbMediaHash = (ins.readField(ByteArray) as ByteArray);
        thumbMimeType = ins.readByte();
        furniMediaHash = (ins.readField(ByteArray) as ByteArray);
        furniMimeType = ins.readByte();
    }

    /**
     * Returns the default thumbnail media for use if this item has no provided
     * custom media.
     */
    protected function getDefaultThumbnailMedia () :MediaDesc
    {
        return new StaticMediaDesc(
            "/media/static/thumbnails/" + getType().toLowerCase() + ".png");
    }

    /**
     * Returns the default furni media for use if this item has no provided
     * custom media.
     */
    protected function getDefaultFurniMedia () :MediaDesc
    {
        return new StaticMediaDesc(
            "/media/static/furni/" + getType().toLowerCase() + ".png");
    }
}
}
