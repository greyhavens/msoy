//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.depot.PersistentRecord;
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
    public static final ColumnExp HASH = colexp(_R, "hash");
    public static final ColumnExp MIME_TYPE = colexp(_R, "mimeType");
    public static final ColumnExp CONSTRAINT = colexp(_R, "constraint");
    // AUTO-GENERATED: FIELDS END

    public static final int SCHEMA_VERSION = 1;

    /** A hash code identifying the media used to display the thumbnail. */
    @Index public int gameId;

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
    public GameThumbnailRecord (int gameId, MediaDesc media)
    {
        this.gameId = gameId;
        hash = media.hash;
        mimeType = media.mimeType;
        constraint = media.constraint;
    }

    /**
     * Converts this persistent record to one usable at runtime by gwt.
     */
    public GameThumbnail toGameThumbnail ()
    {
        GameThumbnail thumbnail = new GameThumbnail();
        thumbnail.media = new MediaDesc(hash, mimeType);
        thumbnail.media.constraint = constraint;
        return thumbnail;
    }

}
