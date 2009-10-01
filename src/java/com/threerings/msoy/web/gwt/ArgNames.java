//
// $Id$

package com.threerings.msoy.web.gwt;

/**
 * Encapsulates argument names for various whirled pages. We have the Pages enumeration, but
 * everything after that is defined hither and yon. This class is a mere starting point for
 * changing that.
 */
public class ArgNames
{
    /** Parameter or token designating the entry vector, recorded for new users. */
    public static final String VECTOR = "vec";

    /** Parameter designating the application id. */
    public static final String APP = "app";

    /** Subnav for a facebook game challenge. */
    public static final String FB_GAME_CHALLENGE = "challenge";

    /** Subnav for a facebook mochi game challenge. */
    public static final String FB_MOCHI_CHALLENGE = "mochichallenge";

    /** Pick some friends to challenge. */
    public static final String FB_CHALLENGE_PICK = "pick";

    /** Popup a dialog to publish a challenge to the feed. */
    public static final String FB_CHALLENGE_FEED = "feed";

    /**
     * Parameters that are passed into the facebook canvas callback. These generally are parsed
     * by the callback servlet and control the GWT token used for the redirect.
     */
    public enum FBParam
    {
        /** Loads a game, value is the integer id of the game. */
        GAME("game"),

        /** Loads a mochi game, value is the mochi tag. */
        MOCHI_GAME("mgame"),

        /** Posts a challenge feed story (needed because the FB request system uses a form POST
         * mechanicsm instead of a javascript callback). */
        CHALLENGE("ch"),

        /** Attaches information about the source of a click. */
        TRACKING("tr"),

        /** Attaches a tracking vector to be used when creating a new user. */
        VECTOR(ArgNames.VECTOR),
        
        APP_ID("app");

        /** The name of the parameter. */
        public String name;

        FBParam (String name) {
            this.name = name;
        }
    }

    /**
     * Returns an array of the challenge parameter name and value. The value is not needed, its
     * presence indicates the current request is part of the game challenge flow.
     */
    public static String[] fbChallengeArgs ()
    {
        return new String[] {FBParam.CHALLENGE.name, "y"};
    }
}
