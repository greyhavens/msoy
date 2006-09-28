//
// $Id$

package com.threerings.msoy.item.server.persist;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.TableGenerator;

import com.threerings.msoy.item.util.ItemEnum;
import com.threerings.msoy.item.web.Item;
import com.threerings.msoy.item.web.Avatar;

/**
 * Represents an uploaded avatargraph for display in albumns or for use as a
 * profile picture.
 */
@Entity
@Table
@TableGenerator(
    name="itemId",
    allocationSize=1,
    pkColumnValue="AVATAR")
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

        this.avatarMediaHash = avatar.avatarMediaHash == null ?
            null : avatar.avatarMediaHash.clone();
        this.avatarMimeType = avatar.avatarMimeType;
        this.description = avatar.description;
    }

    @Override // from ItemRecord
    public ItemEnum getType ()
    {
        return ItemEnum.AVATAR;
    }
    
    @Override
    public Object clone ()
    {
        AvatarRecord clone = (AvatarRecord) super.clone();
        clone.avatarMediaHash = avatarMediaHash.clone();
        return clone;
    }

    @Override
    protected Item createItem ()
    {
        Avatar object = new Avatar();
        object.avatarMediaHash = this.avatarMediaHash == null ?
            null : this.avatarMediaHash.clone();
        object.avatarMimeType = this.avatarMimeType;
        object.description = this.description;
        return object;
    }
}
