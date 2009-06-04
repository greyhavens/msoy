//
// $Id$

package com.threerings.msoy.data.all {

import com.threerings.io.ObjectInputStream;
import com.threerings.io.ObjectOutputStream;
import com.threerings.io.Streamable;

/**
 * Contains all the information needed to launch a particular game.
 */
public class LaunchConfig
    implements Streamable
{
    /** A constant used to denote in-world Flash games. */
    public static const FLASH_IN_WORLD :int = 0;

    /** A constant used to denote lobbied Flash games. */
    public static const FLASH_LOBBIED :int = 1;

    /** A constant used to denote single player Flash games. */
    public static const FLASH_SOLO :int = 2;

    /** A constant used to denote Java games lobbied in Flash. */
    public static const JAVA_FLASH_LOBBIED :int = 3;

    /** A constant used to denote Java games lobbied themselves (in Java). */
    public static const JAVA_SELF_LOBBIED :int = 4;

    /** A constant used to denote single player Java games. */
    public static const JAVA_SOLO :int = 5;

    /** The unique identifier for the game in question. */
    public var gameId :int;

    /** The type of this game (see above constants). */
    public var type :int;

    /** The display name of this game. */
    public var name :String;

    /** The path (relative to the resource URL) for the game client media (SWF or JAR). */
    public var clientMediaPath :String;

    /** The game server to which the game should connect. */
    public var gameServer :String;

    /** The port on which the game should connect to the game server. */
    public var gamePort :int;

    /** The world server that is hosting this game's main group room. */
    public var groupServer :String;

    /** The port on which the game should connect to the world server. */
    public var groupPort :int;

    /** The scene to which the client should go if this is an in-world game. */
    public var sceneId :int;

    /** The port on which the game should connect to the server for HTTP requests (used by Java
     * which must connect back to the game server for its game jar file). */
    public var httpPort :int;

    /** If true, the (Java) game requires the signed client that includes the LWJGL libraries. */
    public var lwjgl :Boolean;

    // from interface Streamable
    public function readObject (ins :ObjectInputStream) :void
    {
        gameId = ins.readInt();
        type = ins.readInt();
        name = ins.readField() as String;
        clientMediaPath = ins.readField() as String;
        gameServer = ins.readField() as String;
        gamePort = ins.readInt();
        groupServer = ins.readField() as String;
        groupPort = ins.readInt();
        sceneId = ins.readInt();
        httpPort = ins.readInt();
        lwjgl = ins.readBoolean();
    }

    // from interface Streamable
    public function writeObject (out :ObjectOutputStream) :void
    {
        throw new Error();
    }    
}
