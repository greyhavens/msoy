//
// $Id$

package client.facebook;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

/**
 * Represents a Whirled or Mochi game on Facebook.
 */
public class FacebookGame
{
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
        id = "w:" + gameId;
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
        id = "m:" + mochiTag;
        challengeArgs = Args.compose(ArgNames.FB_MOCHI_CHALLENGE, mochiTag);
        canvasArgs = "mgame=" + mochiTag;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof FacebookGame) && id.equals(((FacebookGame)other).id); 
    }
}
