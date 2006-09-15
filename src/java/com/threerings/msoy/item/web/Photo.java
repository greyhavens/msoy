//
// $Id$

package com.threerings.msoy.item.web;

/**
 * Represents an uploaded photograph for display in albumns or for use as a
 * profile picture.
 */
public class Photo extends Item
{
    /** A hash code identifying the photo media. */
    public byte[] photoMediaHash;

    /** The MIME type of the {@link #photoMediaHash} media. */
    public byte photoMimeType;

    /** A caption for this photo (max length 255 characters). */
    public String caption;

    /**
     * Returns a media descriptor for the actual photo media.
     */
    public MediaDesc getPhotoMedia ()
    {
        return new MediaDesc(photoMediaHash, photoMimeType);
    }

    // @Override // from Item
    public String getType ()
    {
        return "PHOTO";
    }

    // @Override // from Item
    public String getDescription ()
    {
        return caption;
    }

    // @Override // from Item
    public boolean isConsistent ()
    {
        return super.isConsistent() && (photoMediaHash != null) &&
            nonBlank(caption);
    }

    // @Override // from Item
    protected MediaDesc getDefaultThumbnailMedia ()
    {
        return getPhotoMedia();
    }

    // @Override // from Item
    protected MediaDesc getDefaultFurniMedia ()
    {
        return getPhotoMedia();
    }
}
