//
// $Id$

package com.threerings.msoy.item.web {

import mx.core.Application;

import com.threerings.util.Hashable;
import com.threerings.util.HashMap;
import com.threerings.util.StringUtil;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

/**
 * The base class for all digital items in the MSOY system.
 *
 * <p><em>Note:</em> this class and all derived classes are very strictly
 * limited in their contents as they must be translatable into JavaScript
 * ({@link IsSerializable}) and must work with the Presents streaming system
 * ({@link Streamable}) and must work with the JORA object persistence system.
 */
public /*abstract*/ class Item
    implements Hashable, Streamable, DSet_Entry
{
    // DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS
    public static const NOT_A_TYPE :int = 0;
    public static const PHOTO :int = 1; //registerItemType(Photo, 1);
    public static const DOCUMENT :int = 2; //registerItemType(Document, 2);
    public static const FURNITURE :int = 3; //registerItemType(Furniture, 3);
    public static const GAME :int = 4; //registerItemType(Game, 4);
    public static const AVATAR :int = 5; //registerItemType(Avatar, 5);
    public static const PET :int = 6; //registerItemType(Pet, 6);
    public static const AUDIO :int = 7; //registerItemType(Audio, 7);
    // Note: registery of Item types is done at the bottom of this class
    // DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS

    /** A 'used' constant value to indicate that the item is unused. */
    public static const UNUSED :int = 0;

    /** A 'used' constant value to indicate that the item is placed
     * as furniture. The 'location' field will contain the sceneId. */
    public static const USED_AS_FURNITURE :int = 1;

    /** A 'used' constant value to indicate that the item is used
     * as an avatar. */
    public static const USED_AS_AVATAR :int = 2;

    // == Instance variables follow =========================================

    /** This item's unique identifier. <em>Note:</em> this identifier is not
     * globally unique among all digital items. Each type of item has its own
     * identifier space. */
    public var itemId :int;

    /** The item ID from which this object was cloned, or -1 if this is not
     * a clone. */
    public var parentId :int;

    /** A bit-mask of flags that we need to know about every digital item
     * without doing further database lookups or network requests. */
    public var flags :int;

    /** The member id of the member that created this item. */
    public var creatorId :int;

    /** The member id of the member that owns this item, or -1 if the item
     *  is an immutable catalog listing. */
    public var ownerId :int;

    /** The current rating of this item, either 0 or between 1 and 5. */
    public var rating :Number;

    /** A code indicating where this item is being used. */
    public var used :int;

    /** A number, interpreted along with 'used' that identifies the
     * location at which this item is being used. */
    public var location :int;

    /** The media used to display this item's thumbnail representation. */
    public var thumbMedia :MediaDesc;

    /** The media used to display this item's furniture representation. */
    public var furniMedia :MediaDesc;

    /**
     * Get the item class corresponding to the specified type.
     */
    public static function getClassForType (itemType :int) :Class
    {
        return (_mapping.get(itemType) as Class);
    }

    /**
     * Get the item type for the specified item class.
     */
    public static function getTypeForClass (iclass :Class) :int
    {
        return (_reverseMapping.get(iclass) as int);
    }

    /**
     * Get an array of all the item types.
     */
    public static function getTypes () :Array
    {
        return _mapping.keys();
    }

    /**
     * Get the Stringy name of the specified item type.
     */
    public static function getTypeName (type :int) :String
    {
        // We can't use a switch statement because our final
        // variables are not actually constants (they are assigned values
        // at class initialization time).
        if (type == PHOTO) {
            return "photo"; 
        } else if (type == AVATAR) {
            return "avatar";
        } else if (type == GAME) {
            return "game";
        } else if (type == FURNITURE) {
            return "furniture";
        } else if (type == DOCUMENT) { 
            return "document";
        } else if (type == PET) { 
            return "pet";
        } else if (type == AUDIO) { 
            return "audio";
        } else {
            return null;
        }
    }

    /**
     * Returns this item's composite identifier.
     */
    public function getIdent () :ItemIdent
    {
        return new ItemIdent(getType(), itemId);
    }

    /**
     * Get the type code for this item's type.
     */
    public function getType () :int
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
     * Returns the parentId, or the itemId if this item is not a clone.
     */
    public function getProgenitorId () :int
    {
        return (parentId == -1) ? itemId : parentId;
    }

    /**
     * Is the item currently in use somewhere?
     */
    public function isUsed () :Boolean
    {
        return (used != UNUSED);
    }

    /**
     * Returns a media descriptor for the media that should be used to display
     * our thumbnail representation.
     */
    public function getThumbnailMedia () :MediaDesc
    {
        return (thumbMedia == null) ? getDefaultThumbnailMedia() : thumbMedia;
    }

    /**
     * Returns a media descriptor for the media that should be used to display
     * our furniture representation.
     */
    public function getFurniMedia () :MediaDesc
    {
        return (furniMedia == null) ? getDefaultFurniMedia() : furniMedia;
    }

    /**
     * Returns the path to a thumbnail image for this item.
     */
    public function getThumbnailPath () :String
    {
        return getThumbnailMedia().getMediaPath();
    }

    // from DSet_Entry
    public function getKey () :Object
    {
        return getIdent();
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
        out.writeInt(parentId);
        out.writeByte(flags);
        out.writeInt(creatorId);
        out.writeInt(ownerId);
        out.writeFloat(rating);
        out.writeByte(used);
        out.writeInt(location);
        out.writeObject(thumbMedia);
        out.writeObject(furniMedia);
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        itemId = ins.readInt();
        parentId = ins.readInt();
        flags = ins.readByte();
        creatorId = ins.readInt();
        ownerId = ins.readInt();
        rating = ins.readFloat();
        used = ins.readByte();
        location = ins.readInt();
        thumbMedia = (ins.readObject() as MediaDesc);
        furniMedia = (ins.readObject() as MediaDesc);
    }

    /**
     * Returns the default thumbnail media for use if this item has no provided
     * custom media.
     */
    protected function getDefaultThumbnailMedia () :MediaDesc
    {
        return new StaticMediaDesc(StaticMediaDesc.THUMBNAIL, getType());
    }

    /**
     * Returns the default furni media for use if this item has no provided
     * custom media.
     */
    protected function getDefaultFurniMedia () :MediaDesc
    {
        return new StaticMediaDesc(StaticMediaDesc.FURNI, getType());
    }

    private static function registerItemType (iclass :Class, itype :int) :int
    {
        if (_mapping == null) {
            _mapping = new HashMap();
            _reverseMapping = new HashMap();
        }

        _mapping.put(itype, iclass);
        _reverseMapping.put(iclass, itype);
        return itype;
    }

    private static var _mapping :HashMap;
    private static var _reverseMapping :HashMap;

    /**
     * Behold the twisted backbends to get this to work.
     * If I do it like I do in Java, it starts to initialize the
     * Item class when it sees the first use of a subclass, which is
     * usually Avatar (in your MemberObject). It starts to set up the static
     * variables in Item and then has to resolve the Photo class, and while
     * it's doing that it sees that Photo extends Item so it tries to
     * resolve the still-unresolved Item and discovers the circular
     * dependancy and freaks out.
     *
     * So we need to make sure to not reference our subclasses until
     * this class is fully initialized.
     * There may be a better way.
     */
    private static function registerAll () :void
    {
        registerItemType(Avatar, AVATAR);
        registerItemType(Document, DOCUMENT);
        registerItemType(Furniture, FURNITURE);
        registerItemType(Game, GAME);
        registerItemType(Photo, PHOTO);
        registerItemType(Pet, PET);
        registerItemType(Audio, AUDIO);
    }
    private static function staticInit () :void
    {
        Application.application.callLater(registerAll);
    }
    staticInit();
}
}
