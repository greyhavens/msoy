//
// $Id$

package com.threerings.msoy.facebook.server.persist;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.facebook.gwt.FacebookInfo;

/**
 * Contains information on a game's Facebook app.
 */
public class FacebookInfoRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<FacebookInfoRecord> _R = FacebookInfoRecord.class;
    public static final ColumnExp<Integer> GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp<Integer> APP_ID = colexp(_R, "appId");
    public static final ColumnExp<String> API_KEY = colexp(_R, "apiKey");
    public static final ColumnExp<String> APP_SECRET = colexp(_R, "appSecret");
    public static final ColumnExp<Long> FB_UID = colexp(_R, "fbUid");
    public static final ColumnExp<Boolean> CHROMELESS = colexp(_R, "chromeless");
    public static final ColumnExp<String> CANVAS_NAME = colexp(_R, "canvasName");
    // AUTO-GENERATED: FIELDS END

    /** Increment this value if you modify the definition of this persistent object in a way that
     * will result in a change to its SQL counterpart. */
    public static final int SCHEMA_VERSION = 6;

    /**
     * Converts the supplied runtime record into a persistent record.
     */
    public static FacebookInfoRecord fromFacebookInfo (FacebookInfo info)
    {
        FacebookInfoRecord record = new FacebookInfoRecord();
        record.gameId = info.gameId;
        record.appId = info.appId;
        record.apiKey = info.apiKey;
        record.appSecret = info.appSecret;
        record.fbUid = info.fbUid;
        record.chromeless = info.chromeless;
        record.canvasName = info.canvasName;
        return record;
    }

    /** The game for which we contain metadata, or 0 if this is for an app. */
    @Id public int gameId;

    /** The app for which we contain metadata, or 0 if this is for a game. */
    @Id public int appId;

    /** The Facebook API key for this game's Facebook app. */
    @Column(length=FacebookInfo.KEY_LENGTH)
    public String apiKey;

    /** The Facebook app secret for this game's Facebook app. */
    @Column(length=FacebookInfo.SECRET_LENGTH)
    public String appSecret;

    /** The id of the application's facebook user (for profile page). */
    public long fbUid;

    /** If true Whirled won't display any chrome when in Facebook App mode. */
    public boolean chromeless;

    /** The Facebook canvas name so Whirled can send redirects to the application. NOTE: this would
    * ideally be obtained instead from the API using the key & secrect, but the getAppProperties
    * method in the facebook API doesn't work. */
    @Column(length=FacebookInfo.CANVAS_NAME_LENGTH)
    public String canvasName;

    /**
     * Converts this persistent record into a runtime record.
     */
    public FacebookInfo toFacebookInfo ()
    {
        FacebookInfo info = new FacebookInfo();
        info.gameId = this.gameId;
        info.appId = this.appId;
        info.apiKey = this.apiKey;
        info.appSecret = this.appSecret;
        info.fbUid = this.fbUid;
        info.chromeless = this.chromeless;
        info.canvasName = this.canvasName;
        return info;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link FacebookInfoRecord}
     * with the supplied key values.
     */
    public static Key<FacebookInfoRecord> getKey (int gameId, int appId)
    {
        return newKey(_R, gameId, appId);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(GAME_ID, APP_ID); }
    // AUTO-GENERATED: METHODS END
}
