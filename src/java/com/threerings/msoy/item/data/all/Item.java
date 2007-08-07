//
// $Id$

package com.threerings.msoy.item.data.all;

import java.util.HashMap;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.io.Streamable;
import com.threerings.presents.dobj.DSet;

/**
 * The base class for all digital items in the MSOY system.
 *
 * <p><em>Note:</em> this class and all derived classes are very strictly limited in their contents
 * as they must be translatable into JavaScript ({@link IsSerializable}) and must work with the
 * Presents streaming system ({@link Streamable}).
 */
public abstract class Item implements Comparable, Streamable, IsSerializable, DSet.Entry
{
    // DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS
    public static final byte OCCUPANT = (byte) -1; // runtime only
    public static final byte NOT_A_TYPE = (byte) 0;
    public static final byte PHOTO = registerItemType(Photo.class, 1);
    public static final byte DOCUMENT = registerItemType(Document.class, 2);
    public static final byte FURNITURE = registerItemType(Furniture.class, 3);
    public static final byte GAME = registerItemType(Game.class, 4);
    public static final byte AVATAR = registerItemType(Avatar.class, 5);
    public static final byte PET = registerItemType(Pet.class, 6);
    public static final byte AUDIO = registerItemType(Audio.class, 7);
    public static final byte VIDEO = registerItemType(Video.class, 8);
    public static final byte DECOR = registerItemType(Decor.class, 9);
    public static final byte TOY = registerItemType(Toy.class, 10);
    // DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS
    //
    // Note: If the number of item types surpasses 31, we need to change the loadedInventory field
    // of MemberObject to be a BitSet or something.

    /** A canonical ordering of our item types for use in the catalog, inventory, etc. */
    public static final byte[] TYPES = {
        AVATAR, FURNITURE, DECOR, TOY, PET, GAME, PHOTO, AUDIO, VIDEO
    };

    /** A 'used' constant value to indicate that the item is unused. */
    public static final byte UNUSED = (byte) 0;

    /** A 'used' constant value to indicate that the item is placed as furniture. The 'location'
     * field will contain the sceneId. */
    public static final byte USED_AS_FURNITURE = (byte) 1;

    /** A 'used' constant value to indicate that the item is used as an avatar. */
    public static final byte USED_AS_AVATAR = (byte) 2;

    /** A 'used' constant value to indicate that the item is used as a pet let out in a room. */
    public static final byte USED_AS_PET = (byte) 3;

    /** A 'used' constant value to indicate that the item is used in a scene as background
     *  bitmap or music (as appropriate). The 'location' field will contain the sceneId. */
    public static final byte USED_AS_BACKGROUND = (byte) 4;

    /** An identifier used to coordinate with the server when uploading media. */
    public static final String FURNI_MEDIA = "furni";

    /** An identifier used to coordinate with the server when uploading media. */
    public static final String THUMB_MEDIA = "thumb";

    /** An identifier used to coordinate with the server when uploading media. */
    public static final String MAIN_MEDIA = "main";

    /** Indicates that somebody has flagged this item as mature content. */
    public static final byte FLAG_FLAGGED_MATURE = 0x1 << 0;

    /** Indicates that somebody has flagged this item as copyrighted content. */
    public static final byte FLAG_FLAGGED_COPYRIGHT= 0x1 << 1;

    /** Indicates that this item has been flagged by an administrator as mature content. */
    public static final byte FLAG_MATURE = 0x1 << 2;

    // == Instance variables follow =========================================

    /** This item's unique identifier. <em>Note:</em> this identifier is not globally unique among
     * all digital items. Each type of item has its own identifier space. */
    public int itemId;

    /** The item ID from which this object was cloned, or 0 if this is not a clone. */
    public int parentId;

    /** A bit-mask of flags that we need to know about every digital item without doing further
     * database lookups or network requests. */
    public byte flags;

    /** The member id of the member that created this item. */
    public int creatorId;

    /** The member id of the member that owns this item, or 0 if it's not in any inventory;
     * e.g. it's listed in the catalog or a gifted item in a mail message. */
    public int ownerId;

    /** The current rating of this item, either 0 or between 1 and 5. */
    public float rating;

    /** A code indicating where this item is being used. */
    public byte used;

    /** A number, interpreted along with 'used' that identifies the location at which this item is
     * being used. */
    public int location;

    /** Our last-touched timestamp, expressed as a double (for actionscript compat, groan). */
    public double lastTouched;

    /** The user provided name for this item. */
    public String name;

    /** The user provided description for this item. */
    public String description;

    /** The media used to display this item's thumbnail representation. */
    public MediaDesc thumbMedia;

    /** The media used to display this item's furniture representation. */
    public MediaDesc furniMedia;

    /**
     * Returns a {@link MediaDesc} configured to display the default furniture media for items of
     * the specified type.
     */
    public static MediaDesc getDefaultFurniMediaFor (byte itemType)
    {
        return new StaticMediaDesc(MediaDesc.IMAGE_PNG, itemType, FURNI_MEDIA);
    }

    /**
     * Returns a {@link MediaDesc} configured to display the default thumbnaiul media for items of
     * the specified type.
     */
    public static MediaDesc getDefaultThumbnailMediaFor (byte itemType)
    {
        return new StaticMediaDesc(
            MediaDesc.IMAGE_PNG, itemType, THUMB_MEDIA, MediaDesc.HALF_VERTICALLY_CONSTRAINED);
    }

    /**
     * Gets the class for the specified item type.
     */
    public static Class getClassForType (byte itemType)
    {
        return (Class) _mapping.get(new Byte(itemType));
    }

    /**
     * Gets the item type for the specified item class.
     */
    public static byte getTypeForClass (Class iclass)
    {
        Byte val = (Byte) _reverseMapping.get(iclass);
        return (val != null) ? val.byteValue() : NOT_A_TYPE;
    }

    /**
     * Gets the Stringy name of the specified item type.
     */
    public static String getTypeName (byte type)
    {
        // We can't use a switch statement because our final variables are not actually constants
        // (they are assigned values at class initialization time).
        if (type == PHOTO) {
            return "photo";
        } else if (type == DOCUMENT) {
            return "document";
        } else if (type == AVATAR) {
            return "avatar";
        } else if (type == FURNITURE) {
            return "furniture";
        } else if (type == GAME) {
            return "game";
        } else if (type == PHOTO) {
            return "photo";
        } else if (type == PET) {
            return "pet";
        } else if (type == AUDIO) {
            return "audio";
        } else if (type == VIDEO) {
            return "video";
        } else if (type == DECOR) {
            return "decor";
        } else if (type == TOY) {
            return "toy";
        } else {
            return "unknown:" + type;
        }
    }

    /**
     * Returns this item's composite identifier.
     */
    public ItemIdent getIdent ()
    {
        return new ItemIdent(getType(), itemId);
    }

    /**
     * Gets the type code for this item.
     */
    public abstract byte getType ();

    /**
     * Returns the parentId, or this itemId if this item is not a clone.
     */
    public int getPrototypeId ()
    {
        return (parentId == 0) ? itemId : parentId;
    }

    /**
     * Tests whether a given flag is set on this item.
     */
    public boolean isSet (byte flag)
    {
        return (flags & flag) != 0;
    }

    /**
     * Sets a given flag to on or off.
     */
    public void setFlag (byte flag, boolean value)
    {
        flags = (byte) (value ? flags | flag : flags ^ ~flag);
    }

    /**
     * Is the item currently in use somewhere?
     */
    public boolean isUsed ()
    {
        return (used != UNUSED);
    }

    /**
     * Returns true if this item can be rated (it is a clone of a catalog entry, or a catalog entry
     * itself), false if not.
     */
    public boolean isRatable ()
    {
        return (parentId != 0) || (parentId == 0 && ownerId == 0);
    }

    /**
     * Returns the media that should be shown when previewing this item.
     */
    public abstract MediaDesc getPreviewMedia ();

    /**
     * Returns a media descriptor for the media that should be used to display our thumbnail
     * representation.
     */
    public MediaDesc getThumbnailMedia ()
    {
        return (thumbMedia == null) ? getDefaultThumbnailMedia() : thumbMedia;
    }

    /**
     * Returns a media descriptor for the media that should be used to display our furniture
     * representation.
     */
    public MediaDesc getFurniMedia ()
    {
        return (furniMedia == null) ? getDefaultFurniMedia() : furniMedia;
    }

    /**
     * Returns the path to a thumbnail image for this item.
     */
    public String getThumbnailPath ()
    {
        return getThumbnailMedia().getMediaPath();
    }

    /**
     * Called during item creation to ensure that media descriptors are not duplicated in this item
     * more than necessary.
     */
    public void checkConsolidateMedia ()
    {
        if (thumbMedia != null &&
            getThumbnailMedia().equals(getDefaultThumbnailMedia())) {
            thumbMedia = null;
        }

        if (furniMedia != null &&
            getFurniMedia().equals(getDefaultFurniMedia())) {
            furniMedia = null;
        }
    }

    /**
     * Verifise that all the required fields in this particular Item subclass are filled in, make
     * sense, and are consistent with each other. This is used to verify the data being edited by a
     * user during item creation, and also that the final uploaded item isn't hacked.
     */
    public boolean isConsistent ()
    {
        return true;
    }

    // from DSet.Entry
    public Comparable getKey ()
    {
        return getIdent();
    }

    // @Override // from Object
    public int hashCode ()
    {
        return itemId;
    }

    // @Override // from Object
    public boolean equals (Object other)
    {
        if (other instanceof Item) {
            Item that = (Item) other;
            // cheap comparison first...
            return (this.itemId == that.itemId) && (this.getType() == that.getType());
        }
        return false;
    }

    // from Comparable
    public int compareTo (Object other)
    {
        double thatTouched = ((Item) other).lastTouched;
        if (lastTouched > thatTouched) {
            return -1;

        } else if (lastTouched < thatTouched) {
            return 1;

        } else {
            return 0;
        }
    }

    // @Override // from Object
    public String toString ()
    {
        return "[type=" + getTypeName(getType()) + ", id=" + itemId + ", name=" + name + "]";
    }

    /**
     * Returns the default thumbnail media for use if this item has no provided custom media.
     */
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        return getDefaultThumbnailMediaFor(getType());
    }

    /**
     * Returns the default furni media for use if this item has no provided custom media.
     */
    protected MediaDesc getDefaultFurniMedia ()
    {
        return getDefaultFurniMediaFor(getType());
    }

    /**
     * A handy method that makes sure that the specified text is not null or all-whitespace.
     * Usually used by isConsistent().
     */
    protected static boolean nonBlank (String text)
    {
        return (text != null) && (text.trim().length() > 0);
    }

    /**
     * Register a concrete subclass and it's associated type code.
     */
    private static byte registerItemType (Class iclass, int itype)
    {
        byte type = (byte) itype;
        if (itype != type) {
            throw new IllegalArgumentException("not a byte");
        }

        if (_mapping == null) {
            _mapping = new HashMap();
            _reverseMapping = new HashMap();
        }

        Byte otype = new Byte(type);
        _mapping.put(otype, iclass);
        _reverseMapping.put(iclass, otype);

        return type;
    }

    private static HashMap _mapping;
    private static HashMap _reverseMapping;
}
