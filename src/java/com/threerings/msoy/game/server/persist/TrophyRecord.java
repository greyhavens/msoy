//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.jdbc.depot.Key;
import com.samskivert.jdbc.depot.PersistentRecord;
import com.samskivert.jdbc.depot.annotation.Column;
import com.samskivert.jdbc.depot.annotation.Id;
import com.samskivert.jdbc.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.TrophySource;

import java.sql.Timestamp;

/**
 * Contains persistent data on a player's trophy.
 */
public class TrophyRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    /** The column identifier for the {@link #gameId} field. */
    public static final String GAME_ID = "gameId";

    /** The qualified column identifier for the {@link #gameId} field. */
    public static final ColumnExp GAME_ID_C =
        new ColumnExp(TrophyRecord.class, GAME_ID);

    /** The column identifier for the {@link #memberId} field. */
    public static final String MEMBER_ID = "memberId";

    /** The qualified column identifier for the {@link #memberId} field. */
    public static final ColumnExp MEMBER_ID_C =
        new ColumnExp(TrophyRecord.class, MEMBER_ID);

    /** The column identifier for the {@link #ident} field. */
    public static final String IDENT = "ident";

    /** The qualified column identifier for the {@link #ident} field. */
    public static final ColumnExp IDENT_C =
        new ColumnExp(TrophyRecord.class, IDENT);

    /** The column identifier for the {@link #name} field. */
    public static final String NAME = "name";

    /** The qualified column identifier for the {@link #name} field. */
    public static final ColumnExp NAME_C =
        new ColumnExp(TrophyRecord.class, NAME);

    /** The column identifier for the {@link #trophyMediaHash} field. */
    public static final String TROPHY_MEDIA_HASH = "trophyMediaHash";

    /** The qualified column identifier for the {@link #trophyMediaHash} field. */
    public static final ColumnExp TROPHY_MEDIA_HASH_C =
        new ColumnExp(TrophyRecord.class, TROPHY_MEDIA_HASH);

    /** The column identifier for the {@link #trophyMimeType} field. */
    public static final String TROPHY_MIME_TYPE = "trophyMimeType";

    /** The qualified column identifier for the {@link #trophyMimeType} field. */
    public static final ColumnExp TROPHY_MIME_TYPE_C =
        new ColumnExp(TrophyRecord.class, TROPHY_MIME_TYPE);

    /** The column identifier for the {@link #whenEarned} field. */
    public static final String WHEN_EARNED = "whenEarned";

    /** The qualified column identifier for the {@link #whenEarned} field. */
    public static final ColumnExp WHEN_EARNED_C =
        new ColumnExp(TrophyRecord.class, WHEN_EARNED);
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 3;

    /** The game that awarded this trophy. */
    @Id
    public int gameId;

    /** The id of the member that holds this trophy. */
    @Id
    public int memberId;

    /** The identifier for this trophy, provided by the game. */
    @Id @Column(length=TrophySource.MAX_IDENT_LENGTH)
    public String ident;

    /** This trophy's name. */
    public String name;

    /** A hash code identifying the media used to display this trophy's image. */
    public byte[] trophyMediaHash;

    /** The MIME type of the {@link #trophyMediaHash} media. */
    public byte trophyMimeType;

    /** The date and time on which this trophy was earned. */
    public Timestamp whenEarned;

    /**
     * Converts this persistent record to a runtime record.
     */
    public Trophy toTrophy ()
    {
        Trophy trophy = new Trophy();
        trophy.gameId = gameId;
        trophy.name = name;
        trophy.trophyMedia = new MediaDesc(
            trophyMediaHash, trophyMimeType, MediaDesc.computeConstraint(
                MediaDesc.THUMBNAIL_SIZE, TrophySource.TROPHY_WIDTH, TrophySource.TROPHY_HEIGHT));
        trophy.whenEarned = whenEarned.getTime();
        return trophy;
    }

    @Override // from Object
    public String toString ()
    {
        return StringUtil.fieldsToString(this);
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link #TrophyRecord}
     * with the supplied key values.
     */
    public static Key<TrophyRecord> getKey (int gameId, int memberId, String ident)
    {
        return new Key<TrophyRecord>(
                TrophyRecord.class,
                new String[] { GAME_ID, MEMBER_ID, IDENT },
                new Comparable[] { gameId, memberId, ident });
    }
    // AUTO-GENERATED: METHODS END
}
