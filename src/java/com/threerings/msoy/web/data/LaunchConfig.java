//
// $Id$

package com.threerings.msoy.web.data;

import com.google.gwt.user.client.rpc.IsSerializable;

/**
 * Contains all the information needed to launch a particular game.
 */
public class LaunchConfig
    implements IsSerializable
{
    /** A constant used to denote lobbied Flash games. */
    public static final int FLASH_LOBBIED = 0;

    /** A constant used to denote single player Flash games. */
    public static final int FLASH_SOLO = 1;

    /** A constant used to denote lobbied Java games. */
    public static final int JAVA_LOBBIED = 2;

    /** A constant used to denote single player Java games. */
    public static final int JAVA_SOLO = 3;

    /** The unique identifier for the game in question. */
    public int gameId;

    /** The type of this game (see above constants). */
    public int type;

    /** The display name of this game. */
    public String name;

    /** The URL for the game client media (SWF or JAR). */
    public String gameMediaURL;

    /** The server to which the game should connect (if this is a multiplayer
     * game). */
    public String server;

    /** The port on which the game should connect to the server (if this is a
     * multiplayer game). */
    public int port;
}
