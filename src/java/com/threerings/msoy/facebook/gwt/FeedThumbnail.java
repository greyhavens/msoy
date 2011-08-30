//
// $Id$

package com.threerings.msoy.facebook.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.orth.data.MediaDesc;

/**
 * A thumbnail image for use in a feed.
 */
public class FeedThumbnail
    implements IsSerializable
{
    public static final int COUNT = 3;

    /** Identifies the functionality of this thumbnail's feed post, e.g. "trophy". */
    public String code;

    /** The variant. Only thumbnails with the same variant may be used together. */
    public String variant;

    /** The position. When using multiple thumbnails at once, they will be ordered by pos. */
    public byte pos;

    /** The image media for the thumbnail. */
    public MediaDesc media;

    /**
     * For deserializing.
     */
    public FeedThumbnail ()
    {
    }

    /**
     * Creates a new thumbnail with the given media.
     */
    public FeedThumbnail (MediaDesc media, String code, String variant, byte pos)
    {
        this.media = media;
        this.code = code;
        this.variant = variant;
        this.pos = pos;
    }
}
