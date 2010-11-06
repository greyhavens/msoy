//
// $Id$

package com.threerings.msoy.game.server.persist;

import java.sql.Timestamp;

import com.samskivert.depot.Key;
import com.samskivert.depot.PersistentRecord;
import com.samskivert.depot.annotation.Column;
import com.samskivert.depot.annotation.Id;
import com.samskivert.depot.expression.ColumnExp;

import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.server.MediaDescFactory;
import com.threerings.msoy.data.all.MediaMimeTypes;
import com.threerings.msoy.edgame.gwt.GameCode;

/**
 * Contains configuration data for a game (either the published or in-development version).
 */
public class GameCodeRecord extends PersistentRecord
{
    // AUTO-GENERATED: FIELDS START
    public static final Class<GameCodeRecord> _R = GameCodeRecord.class;
    public static final ColumnExp GAME_ID = colexp(_R, "gameId");
    public static final ColumnExp IS_DEVELOPMENT = colexp(_R, "isDevelopment");
    public static final ColumnExp CONFIG = colexp(_R, "config");
    public static final ColumnExp CLIENT_MEDIA_HASH = colexp(_R, "clientMediaHash");
    public static final ColumnExp CLIENT_MIME_TYPE = colexp(_R, "clientMimeType");
    public static final ColumnExp SERVER_MEDIA_HASH = colexp(_R, "serverMediaHash");
    public static final ColumnExp SERVER_MIME_TYPE = colexp(_R, "serverMimeType");
    public static final ColumnExp SPLASH_MEDIA_HASH = colexp(_R, "splashMediaHash");
    public static final ColumnExp SPLASH_MIME_TYPE = colexp(_R, "splashMimeType");
    public static final ColumnExp LAST_UPDATED = colexp(_R, "lastUpdated");
    // AUTO-GENERATED: FIELDS END

    /** Increment this if you make a schema impacting change to this record. */
    public static final int SCHEMA_VERSION = 2;

    /**
     * Creates a persistent record from the supplied runtime record.
     */
    public static GameCodeRecord fromGameCode (GameCode code)
    {
        GameCodeRecord record = new GameCodeRecord();
        record.gameId = code.gameId;
        record.isDevelopment = code.isDevelopment;
        record.config = code.config;
        record.clientMediaHash = HashMediaDesc.unmakeHash(code.clientMedia);
        record.clientMimeType = MediaMimeTypes.unmakeMimeType(code.clientMedia);
        record.serverMediaHash = HashMediaDesc.unmakeHash(code.serverMedia);
        record.serverMimeType = MediaMimeTypes.unmakeMimeType(code.serverMedia);
        record.splashMediaHash = HashMediaDesc.unmakeHash(code.splashMedia);
        record.splashMimeType = MediaMimeTypes.unmakeMimeType(code.splashMedia);
        return record;
    }

    /** The id of the game. */
    @Id public int gameId;

    /** Whether or not we have data for the development or published version. */
    @Id public boolean isDevelopment;

    /** The XML game configuration. */
    @Column(length=65535)
    public String config;

    /** A hash code identifying the client code media. */
    public byte[] clientMediaHash;

    /** The MIME type of the {@link #clientMediaHash} media. */
    public byte clientMimeType;

    /** A hash code identifying the server code media. */
    @Column(nullable=true)
    public byte[] serverMediaHash;

    /** The MIME type of the {@link #serverMediaHash} media. */
    public byte serverMimeType;

    /** A hash code identifying the splash media. */
    @Column(nullable=true)
    public byte[] splashMediaHash;

    /** The MIME type of the {@link #splashMediaHash} media. */
    public byte splashMimeType;

    /** The time at which this record was last updated. */
    public Timestamp lastUpdated;

    /**
     * Converts this persistent record to a runtime record.
     */
    public GameCode toGameCode ()
    {
        GameCode code = new GameCode();
        code.gameId = gameId;
        code.isDevelopment = isDevelopment;
        code.config = config;
        code.clientMedia = MediaDescFactory.make(clientMediaHash, clientMimeType, null);
        code.serverMedia = MediaDescFactory.make(serverMediaHash, serverMimeType, null);
        code.splashMedia = MediaDescFactory.make(splashMediaHash, splashMimeType, null);
        code.lastUpdated = lastUpdated.getTime();
        return code;
    }

    // AUTO-GENERATED: METHODS START
    /**
     * Create and return a primary {@link Key} to identify a {@link GameCodeRecord}
     * with the supplied key values.
     */
    public static Key<GameCodeRecord> getKey (int gameId, boolean isDevelopment)
    {
        return newKey(_R, gameId, isDevelopment);
    }

    /** Register the key fields in an order matching the getKey() factory. */
    static { registerKeyFields(GAME_ID, IS_DEVELOPMENT); }
    // AUTO-GENERATED: METHODS END
}
