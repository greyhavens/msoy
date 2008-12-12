//
// $Id$

package com.threerings.msoy.item.data.all {

import com.threerings.util.Comparable;
import com.threerings.util.HashMap;
import com.threerings.util.Hashable;
import com.threerings.util.MethodQueue;

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

import com.threerings.presents.dobj.DSet_Entry;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * The base class for all digital items in the MSOY system.
 *
 * <p><em>Note:</em> this class and all derived classes are very strictly limited in their contents
 * as they must be translatable into JavaScript (IsSerializable) and must work with the Presents
 * streaming system (Streamable) and must work with the JORA object persistence system.
 */
public /*abstract*/ class Item
    implements Comparable, Hashable, Streamable, DSet_Entry
{
    // New type constants must be added to ItemTypes.as, then reflected here. This is in order to 
    // decouple simple static queries from factory functionality which requires compilation of the
    // entire Item hierarchy.

    /** The type constant for an Occupant. */
    public static const OCCUPANT :int = ItemTypes.OCCUPANT; // only used at runtime

    /** The type constant for an unassigned or invalid item. */
    public static const NOT_A_TYPE :int = ItemTypes.NOT_A_TYPE;

    /** The type constant for a {@link Photo} item. */
    public static const PHOTO :int = ItemTypes.PHOTO;

    /** The type constant for a {@link Document} item. */
    public static const DOCUMENT :int = ItemTypes.DOCUMENT;

    /** The type constant for a {@link Furniture} item. */
    public static const FURNITURE :int = ItemTypes.FURNITURE;

    /** The type constant for a {@link Game} item. */
    public static const GAME :int = ItemTypes.GAME;

    /** The type constant for a {@link Avatar} item. */
    public static const AVATAR :int = ItemTypes.AVATAR;

    /** The type constant for a {@link Pet} item. */
    public static const PET :int = ItemTypes.PET;

    /** The type constant for a {@link Audio} item. */
    public static const AUDIO :int = ItemTypes.AUDIO;

    /** The type constant for a {@link Video} item. */
    public static const VIDEO :int = ItemTypes.VIDEO;

    /** The type constant for a {@link Decor} item. */
    public static const DECOR :int = ItemTypes.DECOR;

    /** The type constant for a {@link Toy} item. */
    public static const TOY :int = ItemTypes.TOY;

    /** The type constant for a {@link LevelPack} item. */
    public static const LEVEL_PACK :int = ItemTypes.LEVEL_PACK;

    /** The type constant for a {@link ItemPack} item. */
    public static const ITEM_PACK :int = ItemTypes.ITEM_PACK;

    /** The type constant for a {@link TrophySource} item. */
    public static const TROPHY_SOURCE :int = ItemTypes.TROPHY_SOURCE;

    /** The type constant for a {@link Prize} item. */
    public static const PRIZE :int = ItemTypes.PRIZE;

    /** The type constant for a {@link Prop} item. */
    public static const PROP :int = ItemTypes.PROP;

    /** A 'used' constant value to indicate that the item is unused. */
    public static const UNUSED :int = 0;

    /** A 'used' constant value to indicate that the item is placed
     * as furniture. The 'location' field will contain the sceneId. */
    public static const USED_AS_FURNITURE :int = 1;

    /** A 'used' constant value to indicate that the item is used as an avatar. */
    public static const USED_AS_AVATAR :int = 2;

    /** A 'used' constant value to indicate that the item is used as a pet let out in a room.
     * The 'location' field will contain the sceneId. */
    public static const USED_AS_PET :int = 3;

    /** A 'used' constant value to indicate that the item is used in a scene as background
     *  bitmap or music (as appropriate). The 'location' field will contain the sceneId. */
    public static const USED_AS_BACKGROUND :int = 4;

    /** Identifies our thumbnail media. */
    public static const THUMB_MEDIA :String = "thumb";

    /** Identifies our furniture media. */
    public static const FURNI_MEDIA :String = "furni";

    /** Identifies other types of media. */
    public static const MAIN_MEDIA :String = "main";

    // == Instance variables follow =========================================

    /** This item's unique identifier. <em>Note:</em> this identifier is not globally unique among
     * all digital items. Each type of item has its own identifier space. */
    public var itemId :int;

    /** The item ID from which this object was cloned, or 0 if this is not a clone. */
    public var sourceId :int;

    /** A bit-mask of runtime attributes about this item. These are not saved in the database
     * anywhere, these are created on-the-fly when looking at metadata not otherwise sent
     * down from the server. */
    public var attrs :int;

    /** The member id of the member that created this item. */
    public var creatorId :int;

    /** The member id of the member that owns this item, or 0 if it's not in any inventory;
     * e.g. it's listed in the catalog or a gifted item in a mail message. */
    public var ownerId :int;

    /** The id of the catalog listing associated with this item. This item may be the
     * original item or a purchased clone. Use isListedOriginal() to check. */
    public var catalogId :int;

    /** The current rating of this item, either 0 or between 1 and 5. */
    public var rating :Number;

    /** The number of user ratings that went into the average rating. */
    public var ratingCount :int;

    /** A code indicating where this item is being used. */
    public var used :int;

    /** A number, interpreted along with 'used' that identifies the location at which this item is
     * being used. */
    public var location :int;

    /** Our last-touched timetamp. */
    public var lastTouched :Number;

    /** The name of this item (max length 255 characters). */
    public var name :String;

    /** A description for this item (max length 255 characters). */
    public var description :String;

    /** Whether or not this item represents mature content. */
    public var mature :Boolean;

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
        throw new Error("Not implemented on the client");
    }

    /**
     * Get an array of all the item types.
     */
    public static function getTypes () :Array
    {
        return _mapping.keys();
    }

    /**
     * Get the translation key for the specified item type.
     */
    public static function getTypeKey (type :int) :String
    {
        return "m." + getTypeName(type);
    }

    /**
     * Get the Stringy name of the specified item type.
     */
    public static function getTypeName (type :int) :String
    {
        return ItemTypes.getTypeName(type);
    }

    /**
     * Returns a MediaDesc configured to display the default furniture media for items of the
     * specified type.
     */
    public static function getDefaultFurniMediaFor (itemType :int) :MediaDesc
    {
        return new DefaultItemMediaDesc(MediaDesc.IMAGE_PNG, itemType, FURNI_MEDIA);
    }

    /**
     * Returns a MediaDesc configured to display the default thumbnaiul media for items of the
     * specified type.
     */
    public static function getDefaultThumbnailMediaFor (itemType :int) :MediaDesc
    {
        return new DefaultItemMediaDesc(MediaDesc.IMAGE_PNG, itemType, THUMB_MEDIA);
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
     * Get the translation key for this type of item.
     */
    public function getTypeKey () :String
    {
        return Item.getTypeKey(getType());
    }

    /**
     * Returns the id of the item of which this item is a clone, or this item's own item id if it
     * is an original item.
     */
    public function getMasterId () :int
    {
        return (sourceId == 0) ? itemId : sourceId;
    }

    /**
     * Returns true if this item is an original item that has been listed in the catalog and now
     * serves as the work-in-progress item from which the litsing is generated.
     */
    public function isListedOriginal () :Boolean
    {
        return (sourceId == 0) && (catalogId != 0) && (ownerId != 0);
    }

    /**
     * Returns true if this item is a clone of something listed in the catalog.
     */
    public function isCatalogClone () :Boolean
    {
        return (sourceId != 0) && (catalogId != 0);
    }

    /**
     * Returns true if this item is a catalog master from which clones are configured,
     * rather than just a clone, or the original item.
     */
    public function isCatalogMaster () :Boolean
    {
        return (sourceId == 0) && (catalogId != 0) && (ownerId == 0);
    }

    /**
     * Returns the suite for which this item is the parent. If the item is a listed catalog master,
     * the suite id will be its negated catalog listing id. If the item is a mutable original, the
     * suite id will be its item id.
     */
    public function getSuiteId () :int
    {
        return (isCatalogMaster() || isCatalogClone()) ? -catalogId : itemId;
    }

    /**
     * Is the item currently in use somewhere?
     */
    public function isUsed () :Boolean
    {
        return (used != UNUSED);
    }

    /**
     * Returns a media descriptor for the media that should be used to display our thumbnail
     * representation.
     */
    public function getThumbnailMedia () :MediaDesc
    {
        return (_thumbMedia == null) ? getDefaultThumbnailMedia() : _thumbMedia;
    }

    /**
     * Returns our raw thumbnail media which may be null. Don't call this method.
     */
    public function getRawThumbnailMedia () :MediaDesc
    {
        return _thumbMedia;
    }

    /**
     * Configures this item's thumbnail media.
     */
    public function setThumbnailMedia (thumbMedia :MediaDesc) :void
    {
        _thumbMedia = thumbMedia;
    }

    /**
     * Returns a media descriptor for the media that should be used to display our furniture
     * representation.
     */
    public function getFurniMedia () :MediaDesc
    {
        return (_furniMedia == null) ? getDefaultFurniMedia() : _furniMedia;
    }

    /**
     * Returns our raw furniture media which may be null. Don't call this method.
     */
    public function getRawFurniMedia () :MediaDesc
    {
        return _furniMedia;
    }

    /**
     * Configures this item's furniture media.
     */
    public function setFurniMedia (furniMedia :MediaDesc) :void
    {
        _furniMedia = furniMedia;
    }

    /**
     * Returns the media that should be shown when previewing this item.
     */
    public function getPreviewMedia () :MediaDesc
    {
        throw new Error("abstract");
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
            return (this.itemId == that.itemId) && (this.getType() === that.getType());
        }
        return false;
    }

    // from Comparable
    public function compareTo (other :Object) :int
    {
        var thatTouched :Number = Item(other).lastTouched;
        if (lastTouched > thatTouched) {
            return -1;

        } else if (lastTouched < thatTouched) {
            return 1;

        } else {
            return 0;
        }
    }

    // from Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        out.writeInt(itemId);
        out.writeInt(sourceId);
        out.writeByte(attrs);
        out.writeInt(creatorId);
        out.writeInt(ownerId);
        out.writeInt(catalogId);
        out.writeFloat(rating);
        out.writeInt(ratingCount);
        out.writeByte(used);
        out.writeInt(location);
        out.writeDouble(lastTouched);
        out.writeField(name);
        out.writeField(description);
        out.writeBoolean(mature);
        out.writeObject(_thumbMedia);
        out.writeObject(_furniMedia);
    }

    // from Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        itemId = ins.readInt();
        sourceId = ins.readInt();
        attrs = ins.readByte();
        creatorId = ins.readInt();
        ownerId = ins.readInt();
        catalogId = ins.readInt();
        rating = ins.readFloat();
        ratingCount = ins.readInt();
        used = ins.readByte();
        location = ins.readInt();
        lastTouched = ins.readDouble();
        name = (ins.readField(String) as String);
        description = (ins.readField(String) as String);
        mature = ins.readBoolean();
        _thumbMedia = MediaDesc(ins.readObject());
        _furniMedia = MediaDesc(ins.readObject());
    }

    /**
     * Returns the default thumbnail media for use if this item has no provided custom media.
     */
    protected function getDefaultThumbnailMedia () :MediaDesc
    {
        return getDefaultThumbnailMediaFor(getType());
    }

    /**
     * Returns the default furni media for use if this item has no provided custom media.
     */
    protected function getDefaultFurniMedia () :MediaDesc
    {
        return getDefaultFurniMediaFor(getType());
    }

    private static function registerItemType (iclass :Class, itype :int) :int
    {
        if (_mapping == null) {
            _mapping = new HashMap();
        }
        _mapping.put(itype, iclass);
        return itype;
    }

    /**
     * Behold the twisted backbends to get this to work.  If I do it like I do in Java, it starts
     * to initialize the Item class when it sees the first use of a subclass, which is usually
     * Avatar (in your MemberObject). It starts to set up the static variables in Item and then has
     * to resolve the Photo class, and while it's doing that it sees that Photo extends Item so it
     * tries to resolve the still-unresolved Item and discovers the circular dependancy and freaks
     * out.
     *
     * So we need to make sure to not reference our subclasses until this class is fully
     * initialized.  There may be a better way.
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
        registerItemType(Video, VIDEO);
        registerItemType(Decor, DECOR);
        registerItemType(Toy, TOY);
        registerItemType(LevelPack, LEVEL_PACK);
        registerItemType(ItemPack, ITEM_PACK);
        registerItemType(TrophySource, TROPHY_SOURCE);
        registerItemType(Prize, PRIZE);
        registerItemType(Prop, PROP);
    }

    private static function staticInit () :void
    {
        MethodQueue.callLater(registerAll);
    }

    /** The media used to display this item's thumbnail representation. */
    protected var _thumbMedia :MediaDesc;

    /** The media used to display this item's furniture representation. */
    protected var _furniMedia :MediaDesc;

    staticInit();
    private static var _mapping :HashMap;
}
}
