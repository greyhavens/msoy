//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.google.common.base.Function;

import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.gwt.GameThumbnail;

/**
 * Represents a thumbnail for a game. Currently only planned for use by facebook feed stories,
 * though it may be extended to other features.
 */
public class GameThumbnailRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GameThumbnailRecord> _R = GameThumbnailRecord.class;
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp TYPE = colexp(_R, "type");
    public static final ColumnExp VARIANT = colexp(_R, "variant");
    public static final ColumnExp POS = colexp(_R, "pos");
    public static final ColumnExp HASH = colexp(_R, "hash");
    public static final ColumnExp MIME_TYPE = colexp(_R, "mimeType");
    public static final ColumnExp CONSTRAINT = colexp(_R, "constraint");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 2;

    /** Functor to get a media path from a thumbnail record. */
    public static final Function<GameThumbnailRecord, String> TO_MEDIA_PATH =
        new Function<GameThumbnailRecord, String>() {
        @Override public String apply (GameThumbnailRecord thumb) {
            return MediaDesc.getMediaPath(thumb.hash, thumb.mimeType, false);
        }
    };

    /** The id of the game that this thumbnail is assigned to, or 0 if it is global. */
    @Index public int gameId;

    /** The type of thumbnail. */
    @Column(defaultValue="0")
    @Index public GameThumbnail.Type type;

    /** The variant. Only thumbnails with the same variant appear together. */
    @Column(defaultValue="''")
    public String variant;

    /** The position. Multiple thumbnails will appear in order of pos. */
    public byte pos;

    /** A hash code identifying the media used to display the thumbnail. */
    public byte[] hash;

    /** The MIME type of the {@link #mediaHash} media. */
    public byte mimeType;

    /** The size constraint on the {@link #mediaHash} media. */
    public byte constraint;

    /**
     * For deserializing.
     */
    public GameThumbnailRecord ()
    {
    }

    /**
     * Creates a new thumbnail record for the given game id and media.
     */
    public GameThumbnailRecord (int gameId, GameThumbnail thumbnail)
    {
        this.gameId = gameId;
        hash = thumbnail.media.hash;
        mimeType = thumbnail.media.mimeType;
        constraint = thumbnail.media.constraint;
        type = thumbnail.type;
        variant = thumbnail.variant;
        pos = thumbnail.pos;
    }

    /**
     * Converts this persistent record to one usable at runtime by gwt.
     */
    public GameThumbnail toGameThumbnail ()
    {
        GameThumbnail thumbnail = new GameThumbnail();
        thumbnail.media = new MediaDesc(hash, mimeType);
        thumbnail.media.constraint = constraint;
        thumbnail.pos = pos;
        thumbnail.type = type;
        thumbnail.variant = variant;
        return thumbnail;
    }

}
