package com.threerings.msoy.item.data.all;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import com.threerings.orth.scene.data.EntityIdent.EntityType;

public enum MsoyItemType implements EntityType<MsoyItemType>
{
    // DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS
    OCCUPANT(null, -1),
    NOT_A_TYPE(null, (byte) 0),
    PHOTO (Photo.class, 1),
    DOCUMENT (Document.class, 2),
    FURNITURE (Furniture.class, 3),
    _DEPRECATED_GAME (null, 4),
    AVATAR (Avatar.class, 5),
    PET (Pet.class, 6),
    AUDIO (Audio.class, 7),
    VIDEO (Video.class, 8),
    DECOR (Decor.class, 9),
    TOY (Toy.class, 10),
    LEVEL_PACK (LevelPack.class, 11),
    ITEM_PACK (ItemPack.class, 12),
    TROPHY_SOURCE (TrophySource.class, 13),
    PRIZE (Prize.class, 14),
    PROP (Prop.class, 15),
    LAUNCHER (Prop.class, 16);
    // DON'T EVER CHANGE THE MAGIC NUMBERS ASSIGNED TO EACH CLASS

    public static MsoyItemType[] SHOP_ITEMS = {
        AVATAR, FURNITURE, DECOR, TOY, PET, LAUNCHER, PHOTO, AUDIO, VIDEO
    };

    public static MsoyItemType[] STUFF_ITEMS = {
        AVATAR, FURNITURE, DECOR, TOY, PET, LAUNCHER, LEVEL_PACK, ITEM_PACK, PHOTO, AUDIO, VIDEO
    };

    public static MsoyItemType[] FAVORITE_ITEMS = {
        NOT_A_TYPE, AVATAR, FURNITURE, DECOR, TOY, PET, LAUNCHER, PHOTO, AUDIO, VIDEO
    };

    MsoyItemType(Class<? extends Item> clazz, int num)
    {
        if (num > 0) {
            registerItemType(clazz, this);
        }
        _b = (byte) num;
    }

    /**
     * Return a canonical, dependable string version of this item type.
     */
    public String typeName()
    {
        return name().toLowerCase();
    }

    public byte toByte ()
    {
        return _b;
    }

    /**
     * Item types for use in the catalog. Note that this does not contain subtypes
     * (ie. LEVEL_PACK) as those do not have top-level categories but are only
     * shown in game shops.
     */
    public boolean isShopType ()
    {
        return Arrays.asList(SHOP_ITEMS).contains(this);
    }

    /**
     * Item types for use on the inventory page.
     */
    public boolean isStuffType ()
    {
        return Arrays.asList(STUFF_ITEMS).contains(this);
    }

    /**
     * Item types for use on the favorites page.
     */
    public boolean isFavoriteType ()
    {
        return Arrays.asList(FAVORITE_ITEMS).contains(this);
    }

    /**
     * Item types for use in giving gifts.
     */
    public boolean isGiftType ()
    {
        switch(this) {
            case AVATAR: case FURNITURE: case DECOR: case TOY: case PET: case LAUNCHER:
            case LEVEL_PACK: case ITEM_PACK: case PHOTO: case AUDIO: case VIDEO: case PROP:
                return true;
        }
        return false;
    }

    /**
     * Item types that can have their 'location' set to a roomId.
     */
    public boolean isRoomType ()
    {
        switch(this) {
            case FURNITURE: case DECOR: case TOY: case PET: case LAUNCHER:
            case PHOTO: case AUDIO: case VIDEO:
                return true;
        }
        return false;
    }

    /**
     * Item types that are entities and can have memory and other such things.
     */
    public boolean isEntityType ()
    {
        switch(this) {
            case AVATAR: case PET: case FURNITURE: case TOY: case DECOR:
                return true;
        }
        return false;
    }

    /**
     * Item types that can specify a basis item when listed.
     */
    public boolean isDerivationType ()
    {
        switch(this) {
            case AVATAR: case PET: case FURNITURE: case TOY: case DECOR: case PHOTO:
                return true;
        }
        return false;
    }

    public boolean isBrandableType ()
    {
        switch(this) {
            case AVATAR: case FURNITURE: case DECOR: case TOY: case PET: case LAUNCHER:
            case LEVEL_PACK: case ITEM_PACK: case PHOTO: case AUDIO: case VIDEO: case PROP:
                return true;
        }
        return false;
    }

    // Whether items of this type can be used even in themes they aren't marked for
    public boolean isUsableAnywhere ()
    {
        return this == AUDIO;
    }

    public Class<? extends Item> getClassForType ()
    {
        return getClassForType(this);
    }

    protected byte _b;

    /**
     * Register a concrete subclass and it's associated type code.
     */
    static void registerItemType (Class<? extends Item> iclass, MsoyItemType type)
    {
        if (_mapping == null) {
            // we can't use google collections here because this class is used in GWT.
            _mapping = new HashMap<MsoyItemType, Class<? extends Item>>();
            if (iclass != null) {
                _reverseMapping = new HashMap<Class<? extends Item>, MsoyItemType>();
            }
        }

        _mapping.put(type, iclass);
        _reverseMapping.put(iclass, type);
    }

    /**
     * Gets the class for the specified item type.
     */
    public static Class<? extends Item> getClassForType (MsoyItemType itemType)
    {
        return _mapping.get(itemType);
    }

    /**
     * Gets the item type for the specified item class.
     */
    public static MsoyItemType getTypeForClass (Class<? extends Item> iclass)
    {
        MsoyItemType type = _reverseMapping.get(iclass);
        return (type != null) ? type : NOT_A_TYPE;
    }

    static protected Map<MsoyItemType, Class<? extends Item>> _mapping;
    static protected Map<Class<? extends Item>, MsoyItemType> _reverseMapping;
}
