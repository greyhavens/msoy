//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Represents an uploaded avatar.
 */
@Entity
@Table
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="AVATAR")
public class AvatarRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #avatarMediaHash} field. */
    public static final String AVATAR_MEDIA_HASH = "avatarMediaHash";

    /** The qualified column identifier for the {@link #avatarMediaHash} field. */
    public static final ColumnExp AVATAR_MEDIA_HASH_C =
        new ColumnExp(AvatarRecord.class, AVATAR_MEDIA_HASH);

    /** The column identifier for the {@link #avatarMimeType} field. */
    public static final String AVATAR_MIME_TYPE = "avatarMimeType";

    /** The qualified column identifier for the {@link #avatarMimeType} field. */
    public static final ColumnExp AVATAR_MIME_TYPE_C =
        new ColumnExp(AvatarRecord.class, AVATAR_MIME_TYPE);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the avatar media. */
    public byte[] avatarMediaHash;

    /** The MIME type of the {@link #avatarMediaHash} media. */
    public byte avatarMimeType;

    public AvatarRecord ()
    {
        super();
    }

    protected AvatarRecord (Avatar avatar)
    {
        super(avatar);

        if (avatar.avatarMedia != null) {
            avatarMediaHash = avatar.avatarMedia.hash;
            avatarMimeType = avatar.avatarMedia.mimeType;
        }
    }

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.AVATAR;
    }

    @Override
    protected Item createItem ()
    {
        Avatar object = new Avatar();
        object.avatarMedia = avatarMediaHash == null ? null :
            new MediaDesc(avatarMediaHash, avatarMimeType);
        return object;
    }
}
