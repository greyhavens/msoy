//
// $Id$

package client.game;

import com.threerings.msoy.web.client.GameServiceAsync;

import client.shell.ShellContext;

/**
 * Extends {@link ShellContext} and provides game-specific services.
 */
public class GameContext extends ShellContext
{
    /** Provides game-related services. */
    public GameServiceAsync gamesvc;

    /** Messages used by the game interfaces. */
    public GameMessages msgs;
}
