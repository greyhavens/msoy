//
// $Id$

package com.threerings.msoy.facebook.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.web.gwt.ArgNames;
import com.threerings.msoy.web.gwt.Args;
import com.threerings.msoy.web.gwt.Pages;

/**
 * Represents a Whirled or Mochi game on Facebook.
 */
public class FacebookGame
    implements IsSerializable
{
    public enum Type
        implements IsSerializable
    {
        WHIRLED,
        MOCHI
    }

    /** The type of game. */
    public Type type;

    /**
     * Creates a new facebook game for deserializing.
     */
    public FacebookGame ()
    {
    }

    /**
     * Creates a new Whirled Facebook game.
     */
    public FacebookGame (int gameId)
    {
        type = Type.WHIRLED;
        _id = String.valueOf(gameId);
    }

    /**
     * Creates a new Mochi Facebook game.
     */
    public FacebookGame (String mochiTag)
    {
        type = Type.MOCHI;
        _id = mochiTag;
    }

    /**
     * Gets the integral (Whirled) game id.
     */
    public int getIntId ()
    {
        return Integer.parseInt(_id);
    }

    /**
     * Gets the string (Mochi) game id.
     */
    public String getStringId ()
    {
        return _id;
    }

    /**
     * Gets the page on which this game may be played.
     */
    public Pages getPlayPage ()
    {
        return choose(Pages.WORLD, Pages.GAMES);
    }

    /**
     * Gets the arguments to pass to the play page in order to play this game.
     */
    public Args getPlayArgs ()
    {
        return choose(Args.compose("game", "p", _id), Args.compose("mochi", _id));
    }

    /**
     * Gets the arguments to pass to the FACEBOOK page in order to initiate a game challenge.
     * @return
     */
    public Args getChallengeArgs ()
    {
        return Args.compose(choose(ArgNames.FB_GAME_CHALLENGE, ArgNames.FB_MOCHI_CHALLENGE), _id);
    }

    /**
     * Gets the CGI parameters to append to the canvas (application) url in order to get to this
     * game.
     */
    public String[] getCanvasArgs ()
    {
        return new String[] {choose(ArgNames.FB_PARAM_GAME, ArgNames.FB_PARAM_MOCHI_GAME), _id};
    }

    /**
     * Returns one of the arguments depending on the type of this game.
     */
    protected <T> T choose (T whirled, T mochi)
    {
        return type == Type.WHIRLED ? whirled : mochi;
    }

    @Override // from Object
    public boolean equals (Object other)
    {
        return (other instanceof FacebookGame) && type.equals(((FacebookGame)other).type) &&
            _id.equals(((FacebookGame)other)._id);
    }

    @Override // from Object
    public int hashCode ()
    {
        return type.hashCode() + _id.hashCode(); 
    }

    /** The id of the game. */ 
    protected String _id;
}
