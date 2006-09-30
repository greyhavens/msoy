//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Avatar;
import com.threerings.msoy.item.web.MediaDesc;

/**
 * Represents an uploaded avatar.
 */
@Entity
@Table
@TableGenerator(name="itemId", allocationSize=1, pkColumnValue="AVATAR")
public class AvatarRecord extends ItemRecord
{
    public static final int SCHEMA_VERSION = BASE_SCHEMA_VERSION*0x100 + 1;

    public static final String AVATAR_MEDIA_HASH = "avatarMediaHash";
    public static final String AVATAR_MIME_TYPE = "avatarMimeType";

    /** A hash code identifying the avatar media. */
    @Column(nullable=false)
    public byte[] avatarMediaHash;

    /** The MIME type of the {@link #avatarMediaHash} media. */
    @Column(nullable=false)
    public byte avatarMimeType;

    /** A description for this avatar (max length 255 characters). */
    @Column(nullable=false)
    public String description;

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
        description = avatar.description;
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
        object.description = description;
        return object;
    }
}
