//
// $Id$

package client.games;

import com.threerings.msoy.web.client.GameServiceAsync;

import client.shell.CShell;

/**
 * Extends {@link CShell} and provides game-specific services.
 */
public class CGame extends CShell
{
    /** Provides game-related services. */
    public static GameServiceAsync gamesvc;

    /** Messages used by the game interfaces. */
    public static GameMessages msgs;
}
