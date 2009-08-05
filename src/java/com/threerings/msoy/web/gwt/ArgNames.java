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
    /** Determines the overall embedding of the web application. */
    public static final String EMBEDDING = "emb";

    /** Subnav for a facebook game challenge. */
    public static final String FB_GAME_CHALLENGE = "challenge";

    /** Subnav for a facebook mochi game challenge. */
    public static final String FB_MOCHI_CHALLENGE = "mochichallenge";

    /** Challenge friends. */
    public static final String FB_CHALLENGE_FRIENDS = "friends";

    /** Challenge friends that are app users. */
    public static final String FB_CHALLENGE_APP_FRIENDS = "appfriends";

    /** Pick some friends to challenge. */
    public static final String FB_CHALLENGE_PICK = "pick";

    /**
     * Value constants and utility functions for the embedding argument.
     */
    public static class Embedding
    {
        /** The facebook application. */
        public static final String FACEBOOK = "fb";

        /**
         * Remove and return the embedding string from the given arguments.
         */
        public static String extract (Args args)
        {
            return args.extractParameter(EMBEDDING);
        }

        /**
         * Combine the embedding value for inclusion in a token.
         */
        public static Args compose (String embedding)
        {
            return Args.compose(EMBEDDING, embedding);
        }
    }
}
