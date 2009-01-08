//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Entity;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.annotation.Index;
import com.samskivert.depot.expression.ColumnExp;

import com.samskivert.util.StringUtil;

import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.game.data.all.Trophy;
import com.threerings.msoy.item.data.all.TrophySource;
import java.sql.Timestamp;

/**
 * Contains persistent data on a player's trophy.
 */
@Entity
public class TrophyRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<TrophyRecord> _R = TrophyRecord.class;
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp MEMBER_ID = colexp(_R, "memberId");
    public static final ColumnExp IDENT = colexp(_R, "ident");
    public static final ColumnExp NAME = colexp(_R, "name");
    public static final ColumnExp TROPHY_MEDIA_HASH = colexp(_R, "trophyMediaHash");
    public static final ColumnExp TROPHY_MIME_TYPE = colexp(_R, "trophyMimeType");
    public static final ColumnExp WHEN_EARNED = colexp(_R, "whenEarned");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 4;

    /** The game that awarded this trophy. */
    @Id
    public int gameId;

    /** The id of the member that holds this trophy. */
    @Id @Index(name="ixMember")
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
     * Create and return a primary {@link Key} to identify a {@link TrophyRecord}
     * with the supplied key values.
     */
    public static Key<TrophyRecord> getKey (int gameId, int memberId, String ident)
    {
        return new Key<TrophyRecord>(
                TrophyRecord.class,
                new ColumnExp[] { GAME_ID, MEMBER_ID, IDENT },
                new Comparable[] { gameId, memberId, ident });
    }
    // AUTO-GENERATED: METHODS END
}
