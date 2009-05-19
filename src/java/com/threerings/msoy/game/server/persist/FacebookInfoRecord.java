//
// $Id$

package com.threerings.msoy.game.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.game.gwt.FacebookInfo;

/**
 * Contains information on a game's Facebook app.
 */
public class FacebookInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FacebookInfoRecord> _R = FacebookInfoRecord.class;
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp FACEBOOK_KEY = colexp(_R, "facebookKey");
    public static final ColumnExp FACEBOOK_SECRET = colexp(_R, "facebookSecret");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 1;

    /**
     * Converts the supplied runtime record into a persistent record.
     */
    public static FacebookInfoRecord fromFacebookInfo (FacebookInfo info)
    {
        FacebookInfoRecord record = new FacebookInfoRecord();
        record.gameId = info.gameId;
        record.key = info.key;
        record.secret = info.secret;
        return record;
    }

    /** The game for which we contain metadata. */
    @Id public int gameId;

    /** The Facebook API key for this game's Facebook app. */
    @Column(length=FacebookInfo.KEY_LENGTH)
    public String key;

    /** The Facebook app secret for this game's Facebook app. */
    @Column(length=FacebookInfo.SECRET_LENGTH)
    public String secret;

    /**
     * Converts this persistent record into a runtime record.
     */
    public FacebookInfo toFacebookInfo ()
    {
        FacebookInfo info = new FacebookInfo();
        info.gameId = this.gameId;
        info.key = this.key;
        info.secret = this.secret;
        return info;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FacebookInfoRecord}
     * with the supplied key values.
     */
    public static Key<FacebookInfoRecord> getKey (int gameId)
    {
        return new Key<FacebookInfoRecord>(
                FacebookInfoRecord.class,
                new ColumnExp[] { GAME_ID },
                new Comparable[] { gameId });
    }
    // AUTO-GENERATED: METHODS END
}
