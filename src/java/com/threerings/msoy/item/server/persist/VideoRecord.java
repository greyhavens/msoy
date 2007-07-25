//
// $Id$

package com.threerings.msoy.item.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.annotation.Entity;
import com.samskivert.jdbc.depot.annotation.Table;
import com.samskivert.jdbc.depot.annotation.TableGenerator;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.threerings.msoy.item.data.all.Video;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;

/**
 * Represents an uploaded piece of video.
 */
@Entity
@Table
@TableGenerator(name="itemId", pkColumnValue="VIDEO")
public class VideoRecord extends ItemRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #videoMediaHash} field. */
    public static final String VIDEO_MEDIA_HASH = "videoMediaHash";

    /** The qualified column identifier for the {@link #videoMediaHash} field. */
    public static final ColumnExp VIDEO_MEDIA_HASH_C =
        new ColumnExp(VideoRecord.class, VIDEO_MEDIA_HASH);

    /** The column identifier for the {@link #videoMimeType} field. */
    public static final String VIDEO_MIME_TYPE = "videoMimeType";

    /** The qualified column identifier for the {@link #videoMimeType} field. */
    public static final ColumnExp VIDEO_MIME_TYPE_C =
        new ColumnExp(VideoRecord.class, VIDEO_MIME_TYPE);
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1 +
        BASE_SCHEMA_VERSION * BASE_MULTIPLIER;

    /** A hash code identifying the video media. */
    public byte[] videoMediaHash;

    /** The MIME type of the {@link #videoMediaHash} media. */
    public byte videoMimeType;

    public VideoRecord ()
    {
        super();
    }

    protected VideoRecord (Video video)
    {
        super(video);

        if (video.videoMedia != null) {
            videoMediaHash = video.videoMedia.hash;
            videoMimeType = video.videoMedia.mimeType;
        }
    }

    @Override // from ItemRecord
    public byte getType ()
    {
        return Item.VIDEO;
    }

    @Override
    protected Item createItem ()
    {
        Video object = new Video();
        object.videoMedia = videoMediaHash == null ? null :
            new MediaDesc(videoMediaHash, videoMimeType);
        return object;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #VideoRecord}
     * with the supplied key values.
     */
    public static Key<VideoRecord> getKey (int itemId)
    {
        return new Key<VideoRecord>(
                VideoRecord.class,
                new String[] { ITEM_ID },
                new Comparable[] { itemId });
    }
    // AUTO-GENERATED: METHODS END
}
