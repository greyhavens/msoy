//
// $Id$

package com.threerings.msoy.facebook.gwt;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

/**
 * Represents a Whirled or Mochi game on Facebook.
 */
public class FacebookGame
{
    /**
     * Checks if the given game spec designates a Whirled game.
     */
    public static boolean isWhirledGame (String spec)
    {
        return spec.startsWith(WHIRLED);
    }

    /**
     * Checks if the given game spec designates a Mochi game.
     */
    public static boolean isMochiGame (String spec)
    {
        return spec.startsWith(MOCHI);
    }

    /**
     * Returns the Whirled game id designated by the given game spec.
     */
    public static int getWhirledGameId (String spec)
    {
        return Integer.parseInt(spec.substring(WHIRLED.length()));
    }

    /**
     * Returns the Mochi game tag designated by the given game spec.
     */
    public static String getMochiGameTag (String spec)
    {
        return spec.substring(MOCHI.length());
    }

    /** The page to go to to play this game. */
    public Pages playPage;

    /** The token arguments to use to play this game. */
    public Args playArgs;

    /** The token arguments (on #facebook) to use to issue a challenge for this game. (Further
     * arguments are appended to designate the mode or phase of the challenge. */
    public Args challengeArgs;

    /** The arguments to append to the canvas URL to play (or view the details of) this game. */
    public String canvasArgs;

    /** The unified id of this game (used as gameSpec parameter to servlets). */
    public String id;

    /**
     * Creates a new Whirled Facebook game.
     */
    public FacebookGame (int gameId)
    {
        playPage = Pages.WORLD;
        playArgs = Args.compose("game", "p", gameId);
        id = WHIRLED + gameId;
        challengeArgs = Args.compose(ArgNames.FB_GAME_CHALLENGE, gameId);
        canvasArgs = "game=" + gameId;
    }

    /**
     * Creates a new Mochi Facebook game.
     */
    public FacebookGame (String mochiTag)
    {
        playPage = Pages.GAMES;
        playArgs = Args.compose("mochi", mochiTag);
        id = MOCHI + mochiTag;
        challengeArgs = Args.compose(ArgNames.FB_MOCHI_CHALLENGE, mochiTag);
        canvasArgs = "mgame=" + mochiTag;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof FacebookGame) && id.equals(((FacebookGame)other).id); 
    }

    @Override // from Object
    public int hashCode ()
    {
        return id.hashCode(); 
    }

    protected static final String WHIRLED = "w:";
    protected static final String MOCHI = "m:";
}
