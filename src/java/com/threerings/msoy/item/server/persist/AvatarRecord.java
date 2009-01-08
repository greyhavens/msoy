//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.TableGenerator;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Avatar;

/**
 * Represents an uploaded avatar.
 */
@TableGenerator(name="itemId", pkColumnValue="AVATAR")
public class AvatarRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<AvatarRecord> _R = AvatarRecord.class;
    public static final ColumnExp AVATAR_MEDIA_HASH = colexp(_R, "avatarMediaHash");
    public static final ColumnExp AVATAR_MIME_TYPE = colexp(_R, "avatarMimeType");
    public static final ColumnExp SCALE = colexp(_R, "scale");
    public static final ColumnExp ITEM_ID = colexp(_R, "itemId");
    public static final ColumnExp SOURCE_ID = colexp(_R, "sourceId");
    public static final ColumnExp CREATOR_ID = colexp(_R, "creatorId");
    public static final ColumnExp OWNER_ID = colexp(_R, "ownerId");
    public static final ColumnExp CATALOG_ID = colexp(_R, "catalogId");
    public static final ColumnExp RATING = colexp(_R, "rating");
    public static final ColumnExp RATING_COUNT = colexp(_R, "ratingCount");
    public static final ColumnExp USED = colexp(_R, "used");
    public static final ColumnExp LOCATION = colexp(_R, "location");
    public static final ColumnExp LAST_TOUCHED = colexp(_R, "lastTouched");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp DESCRIPTION = colexp(_R, "description");
    public static final ColumnExp MATURE = colexp(_R, "mature");
    public static final ColumnExp THUMB_MEDIA_HASH = colexp(_R, "thumbMediaHash");
    public static final ColumnExp THUMB_MIME_TYPE = colexp(_R, "thumbMimeType");
    public static final ColumnExp THUMB_CONSTRAINT = colexp(_R, "thumbConstraint");
    public static final ColumnExp FURNI_MEDIA_HASH = colexp(_R, "furniMediaHash");
    public static final ColumnExp FURNI_MIME_TYPE = colexp(_R, "furniMimeType");
    public static final ColumnExp FURNI_CONSTRAINT = colexp(_R, "furniConstraint");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the avatar media. */
    public byte[] avatarMediaHash;

    /** The MIME type of the {@link #avatarMediaHash} media. */
    public byte avatarMimeType;

    /** The scaling to apply to the avatar. */
    @Column(defaultValue="1")
    public float scale;

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.AVATAR;
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
            avatarMediaHash = avatar.avatarMedia.hash;
            avatarMimeType = avatar.avatarMedia.mimeType;
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
            new MediaDesc(avatarMediaHash, avatarMimeType);
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
        return new Key<AvatarRecord>(
                AvatarRecord.class,
                new ColumnExp[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
