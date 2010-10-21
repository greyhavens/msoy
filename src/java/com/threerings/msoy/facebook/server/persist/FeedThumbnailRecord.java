//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import com.google.common.base.Function;
import com.google.common.base.Preconditions;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.facebook.gwt.FeedThumbnail;

/**
 * Represents a feed thumbnail for a game or application.
 */
public class FeedThumbnailRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FeedThumbnailRecord> _R = FeedThumbnailRecord.class;
    public static final ColumnExp APP_ID = colexp(_R, "appId");
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp CODE = colexp(_R, "code");
    public static final ColumnExp VARIANT = colexp(_R, "variant");
    public static final ColumnExp POS = colexp(_R, "pos");
    public static final ColumnExp HASH = colexp(_R, "hash");
    public static final ColumnExp MIME_TYPE = colexp(_R, "mimeType");
    public static final ColumnExp CONSTRAINT = colexp(_R, "constraint");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** Functor to get a media path from a thumbnail record. */
    public static final Function<FeedThumbnailRecord, String> TO_MEDIA_PATH =
        new Function<FeedThumbnailRecord, String>() {
        public String apply (FeedThumbnailRecord thumb) {
            return HashMediaDesc.getMediaPath(thumb.hash, thumb.mimeType);
        }
    };

    /** Functor to get a runtime thumbnail from a thumbnail record. */
    public static final Function<FeedThumbnailRecord, FeedThumbnail> TO_THUMBNAIL =
        new Function<FeedThumbnailRecord, FeedThumbnail>() {
        public FeedThumbnail apply (FeedThumbnailRecord thumb) {
            return thumb.toFeedThumbnail();
        }
    };

    /**
     * Creates a thumbnail record for the given game and runtime data.
     */
    public static FeedThumbnailRecord forGame (int gameId, FeedThumbnail thumb)
    {
        return new FeedThumbnailRecord(0, gameId, thumb);
    }

    /**
     * Creates a thumbnail record for the given app and runtime data.
     */
    public static FeedThumbnailRecord forApp (int appId, FeedThumbnail thumb)
    {
        return new FeedThumbnailRecord(appId, 0, thumb);
    }

    /** The id of the application that this thumbnail is assigned to, or 0 if it is for a game. */
    @Id public int appId;

    /** The id of the game that this thumbnail is assigned to, or 0 if it is for an application. */
    @Id public int gameId;

    /** Identifies the functionality of this thumbnail's feed post, e.g. "trophy". */
    @Id public String code;

    /** The variant. Only thumbnails with the same variant appear together. */
    @Id public String variant;

    /** The position. Multiple thumbnails will appear in order of pos. */
    @Id public byte pos;

    /** A hash code identifying the media used to display the thumbnail. */
    public byte[] hash;

    /** The MIME type of the {@link #mediaHash} media. */
    public byte mimeType;

    /** The size constraint on the {@link #mediaHash} media. */
    public byte constraint;

    /**
     * For deserializing.
     */
    public FeedThumbnailRecord ()
    {
    }

    /**
     * Converts this persistent record to one usable at runtime by gwt.
     */
    public FeedThumbnail toFeedThumbnail ()
    {
        FeedThumbnail thumbnail = new FeedThumbnail();
        thumbnail.media = new HashMediaDesc(hash, mimeType, constraint);
        thumbnail.pos = pos;
        thumbnail.code = code;
        thumbnail.variant = variant;
        return thumbnail;
    }

    /**
     * Creates a new thumbnail record for the given game or app and media.
     */
    protected FeedThumbnailRecord (int appId, int gameId, FeedThumbnail thumbnail)
    {
        Preconditions.checkArgument(appId == 0 ^ gameId == 0);
        this.gameId = gameId;
        this.appId = appId;
        hash = HashMediaDesc.unmakeHash(thumbnail.media);
        mimeType = thumbnail.media.getMimeType();
        constraint = thumbnail.media.getConstraint();
        code = thumbnail.code;
        variant = thumbnail.variant;
        pos = thumbnail.pos;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FeedThumbnailRecord}
     * with the supplied key values.
     */
    public static Key<FeedThumbnailRecord> getKey (int appId, int gameId, String code, String variant, byte pos)
    {
        return newKey(_R, appId, gameId, code, variant, pos);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(APP_ID, GAME_ID, CODE, VARIANT, POS); }
    // AUTO-GENERATED: METHODS END
}
