package com.threerings.msoy.data.all;

import com.threerings.io.Streamable;

import com.threerings.orth.data.MediaDesc;

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
        this(mimeType, NOT_CONSTRAINED);
    }

    /**
     * Creates a media descriptor from the supplied configuration.
     */
    public MediaDescImpl (byte mimeType, byte constraint)
    {
        _mimeType = mimeType;
        _constraint = constraint;
    }

    /** The MIME type of the media associated with this item. */
    public byte getMimeType ()
    {
        return _mimeType;
    }

    public String getProxyMediaPath ()
    {
        throw new IllegalArgumentException("Not implemented");
    }

    public byte getConstraint ()
    {
        return _constraint;
    }

    public void setConstraint (byte constraint)
    {
        _constraint = constraint;
    }

    public boolean isImage ()
    {
        return MediaMimeTypes.isImage(getMimeType());
    }

    public boolean isSWF ()
    {
        return (getMimeType() == MediaMimeTypes.APPLICATION_SHOCKWAVE_FLASH);
    }

    public boolean isAudio ()
    {
        return MediaMimeTypes.isAudio(getMimeType());
    }

    public boolean isVideo ()
    {
        return MediaMimeTypes.isVideo(getMimeType());
    }

    public boolean isExternal ()
    {
        return MediaMimeTypes.isExternal(getMimeType());
    }

    public boolean hasFlashVisual ()
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

    public boolean isRemixed ()
    {
        return MediaMimeTypes.isRemixed(getMimeType());
    }

    public boolean isRemixable ()
    {
        return MediaMimeTypes.isRemixable(getMimeType());
    }

    @Override // from Object
    public int hashCode ()
    {
        return (_mimeType * 43) + _constraint;
    }

	@Override // from Object
	public boolean equals (Object other)
	{
		return (other instanceof MediaDesc) &&
			(_mimeType == ((MediaDesc) other).getMimeType()) &&
            (_constraint == ((MediaDesc) other).getConstraint());

	}

    protected byte _mimeType;

    protected byte _constraint;
}
