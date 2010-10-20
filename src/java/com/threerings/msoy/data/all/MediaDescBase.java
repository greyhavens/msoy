package com.threerings.msoy.data.all;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.orth.scene.data.EntityMedia;

public abstract class MediaDescBase implements IsSerializable, EntityMedia
{
    /** The MIME type of the media associated with this item. */
    public byte mimeType;

    /** Used for deserialization. */
    public MediaDescBase ()
    {
    }

    public MediaDescBase (byte mimeType)
    {
        this.mimeType = mimeType;
    }

    public byte getMimeType ()
    {
        return mimeType;
    }

    /**
     * Is this media merely an image type?
     */
    public boolean isImage ()
    {
        return MediaMimeTypes.isImage(mimeType);
    }

    /**
     * Is this media a SWF?
     */
    public boolean isSWF ()
    {
        return (mimeType == MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH);
    }

    /**
     * Is this media purely audio?
     */
    public boolean isAudio ()
    {
        return MediaMimeTypes.isAudio(mimeType);
    }

    /**
     * Is this media video?
     */
    public boolean isVideo ()
    {
        return MediaMimeTypes.isVideo(mimeType);
    }

    public boolean isExternal ()
    {
        return MediaMimeTypes.isExternal(mimeType);
    }

    @Override // from Object
    public int hashCode ()
    {
        return mimeType;
    }

	@Override // from Object
	public boolean equals (Object other)
	{
		return (other instanceof MediaDescBase) &&
			(this.mimeType == ((MediaDesc) other).mimeType);
	}


    /** Hexadecimal digits. */
    protected static final String HEX = "0123456789abcdef";
}
