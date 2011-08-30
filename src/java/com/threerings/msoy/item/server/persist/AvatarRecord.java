//
// $Id$

package com.threerings.msoy.item.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MsoyItemType;
import com.threerings.msoy.server.MediaDescFactory;

/**
 * Represents an uploaded avatar.
 */
@TableGenerator(name="itemId", pkColumnValue="AVATAR")
public class AvatarRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AvatarRecord> _R = AvatarRecord.class;
    public static final ColumnExp<byte[]> AVATAR_MEDIA_HASH = colexp(_R, "avatarMediaHash");
    public static final ColumnExp<Byte> AVATAR_MIME_TYPE = colexp(_R, "avatarMimeType");
    public static final ColumnExp<Float> SCALE = colexp(_R, "scale");
    public static final ColumnExp<Integer> ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp<Integer> SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp<Integer> CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp<Integer> OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp<Integer> CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp<Integer> RATING_SUM = colexp(_R, "ratingSum");
    public static final ColumnExp<Integer> RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp<Item.UsedAs> USED = colexp(_R, "used");
    public static final ColumnExp<Integer> LOCATION = colexp(_R, "location");
    public static final ColumnExp<Timestamp> LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp<String> NAME = colexp(_R, "name");
    public static final ColumnExp<String> DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp<Boolean> MATURE = colexp(_R, "mature");
    public static final ColumnExp<byte[]> THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp<Byte> THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp<Byte> THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    public static final ColumnExp<byte[]> FURNI_MEDIA_HASH = colexp(_R, "furniMediaHash");
    public static final ColumnExp<Byte> FURNI_MIME_TYPE = colexp(_R, "furniMimeType");
    public static final ColumnExp<Byte> FURNI_CONSTRAINT = colexp(_R, "furniConstraint");
    // AUTO-GENERATED: FIELDS END

    /** Update this version if you change fields specific to this derived class. */
    public static final int ITEM_VERSION = 2;

    /** This combines {@link #ITEM_VERSION} with {@link #BASE_SCHEMA_VERSION} to create a version
     * that allows us to make ItemRecord-wide changes and specific derived class changes. */
    public static final int SCHEMA_VERSION = ITEM_VERSION + BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the avatar media. */
    public byte[] avatarMediaHash;

    /** The MIME type of the {@link #avatarMediaHash} media. */
    public byte avatarMimeType;

    /** The scaling to apply to the avatar. */
    @Column(defaultValue="1")
    public float scale;

    @Override // from ItemRecord
    public MsoyItemType getType ()
    {
        return MsoyItemType.AVATAR;
    }

    @Override // from ItemRecord
    public void initFromClone (CloneRecord clone)
    {
        super.initFromClone(clone);

        this.scale = ((AvatarCloneRecord) clone).scale;
    }

    @Override // from ItemRecord
    public void fromItem (Item item)
    {
        super.fromItem(item);

        Avatar avatar = (Avatar)item;
        if (avatar.avatarMedia != null) {
            avatarMediaHash = HashMediaDesc.unmakeHash(avatar.avatarMedia);
            avatarMimeType = avatar.avatarMedia.getMimeType();
        } else {
            avatarMediaHash = null;
        }
        scale = avatar.scale;
    }

    @Override // from ItemRecord
    public byte[] getPrimaryMedia ()
    {
        return avatarMediaHash;
    }

    @Override // from ItemRecord
    protected byte getPrimaryMimeType ()
    {
        return avatarMimeType;
    }

    @Override // from ItemRecord
    protected void setPrimaryMedia (byte[] hash)
    {
        avatarMediaHash = hash;
    }

    @Override // from ItemRecord
    protected Item createItem ()
    {
        Avatar object = new Avatar();
        object.avatarMedia = avatarMediaHash == null ? null :
            MediaDescFactory.createMediaDesc(avatarMediaHash, avatarMimeType);
        object.scale = scale;
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link AvatarRecord}
     * with the supplied key values.
     */
    public static Key<AvatarRecord> getKey (int itemId)
    {
        return newKey(_R, itemId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(ITEM_ID); }
    // AUTO-GENERATED: METHODS END
}
