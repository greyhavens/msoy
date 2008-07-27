//
// $Id$

package client.games;

import com.threerings.msoy.game.gwt.GameServiceAsync;

import client.shell.CShell;

/**
 * Extends {@link CShell} and provides game-specific services.
 */
public class CGames extends CShell
{
    /** Provides game-related services. */
    public static GameServiceAsync gamesvc;

    /** Messages used by the games interfaces. */
    public static GamesMessages msgs;
}
