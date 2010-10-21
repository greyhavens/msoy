package com.threerings.msoy.data.all;

import com.threerings.io.Streamable;

import com.google.gwt.user.client.rpc.IsSerializable;

public abstract class MediaDescImpl
    implements MediaDesc, IsSerializable, Streamable
{
    /** Used for deserialization. */
    public MediaDescImpl ()
    {
    }

    public MediaDescImpl (byte mimeType)
    {
        this._mimeType = mimeType;
    }

    /** The MIME type of the media associated with this item. */
    @Override public byte getMimeType ()
    {
        return _mimeType;
    }

    @Override public boolean isImage ()
    {
        return MediaMimeTypes.isImage(getMimeType());
    }

    @Override public boolean isSWF ()
    {
        return (getMimeType() == MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH);
    }

    @Override public boolean isAudio ()
    {
        return MediaMimeTypes.isAudio(getMimeType());
    }

    @Override public boolean isVideo ()
    {
        return MediaMimeTypes.isVideo(getMimeType());
    }

    @Override public boolean isExternal ()
    {
        return MediaMimeTypes.isExternal(getMimeType());
    }

    @Override public boolean hasFlashVisual ()
    {
        switch (getMimeType()) {
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

    @Override public boolean isRemixed ()
    {
        return MediaMimeTypes.isRemixed(getMimeType());
    }

    @Override public boolean isRemixable ()
    {
        return MediaMimeTypes.isRemixable(getMimeType());
    }

    @Override // from Object
    public int hashCode ()
    {
        return getMimeType();
    }

	@Override // from Object
	public boolean equals (Object other)
	{
		return (other instanceof MediaDescImpl) &&
			(this.getMimeType() == ((MediaDesc) other).getMimeType());
	}

    protected byte _mimeType;
}
