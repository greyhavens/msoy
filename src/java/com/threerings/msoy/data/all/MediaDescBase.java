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


    /**
     * Return true if this media has a visual component that can be shown in
     * flash.
     */
    public boolean hasFlashVisual ()
    {
        switch (mimeType) {
        case MediaMimeTypes.IMAGE_PNG:
        case MediaMimeTypes.IMAGE_JPEG:
        case MediaMimeTypes.IMAGE_GIF:
        case MediaMimeTypes.VIDEO_FLASH:
        case MediaMimeTypes.EXTERNAL_YOUTUBE:
        case MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH:
            return true;

        default:
            return false;
        }
    }

    /**
     * Is this a zip of some sort?
     */
    public boolean isRemixed ()
    {
        return MediaMimeTypes.isRemixed(mimeType);
    }

    /**
     * Is this media remixable?
     */
    public boolean isRemixable ()
    {
        return MediaMimeTypes.isRemixable(mimeType);
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
