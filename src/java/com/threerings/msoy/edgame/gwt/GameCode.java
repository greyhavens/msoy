//
// $Id$

package com.threerings.msoy.edgame.gwt;

import com.threerings.orth.data.MediaDesc;

import com.google.gwt.user.client.rpc.IsSerializable;

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

    /**
     * For the given  XML game configuration string, checks if the game takes place in the world.
     */
    public static boolean detectIsInWorld (String config)
    {
        return (config != null) && (config.indexOf("<avrg/>") >= 0);
    }

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

    @Override // from Object
    public String toString ()
    {
        return gameId + ", isDev=" + isDevelopment + ", config=" + config;
    }
}
