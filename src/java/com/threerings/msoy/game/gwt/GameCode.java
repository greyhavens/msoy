//
// $Id$

package com.threerings.msoy.game.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.data.all.MediaDesc;

/**
 * Contains the code and XML configuration for a game.
 */
public class GameCode
    implements IsSerializable
{
    /** Identifies the server code media. */
    public static final String SERVER_CODE_MEDIA = "scode";

    /** Identifies the game splash media. */
    public static final String SPLASH_MEDIA = "splash";

    /** This game's unique identifier. */
    public int gameId;

    /** Whether this data is for the development or published version. */
    public boolean isDevelopment;

    /** The XML game configuration. */
    public String config;

    /** The game's client code media. */
    public MediaDesc clientMedia;

    /** The game's server code media. */
    public MediaDesc serverMedia;

    /** The game's splash screen media (shown when loading). */
    public MediaDesc splashMedia;

    /** The time at which this code was last updated. */
    public long lastUpdated;
}
