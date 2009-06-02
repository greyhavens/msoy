//
// $Id$

package com.threerings.msoy.web.gwt;

/**
 * A place where we can encapsulate the creation of arguments that link to complex pages in Whirled
 * that are required on the server and on the client. We do this here so that we don't have
 * references between otherwise unrelated classes and services introduced by the fact that we want
 * a Hyperlink from somewhere in Whirled to somewhere (from a code standpoint) totally unrelated.
 * See also NaviUtil in src/gwt. 
 */
public class SharedNaviUtil
{
    public enum GameDetails {
        INSTRUCTIONS("i"), COMMENTS("c"), TROPHIES("t"), MYRANKINGS("mr"),
        TOPRANKINGS("tr"), METRICS("m"), LOGS("l");

        public String code () {
            return _code;
        }

        public Args args (int gameId)
        {
            return Args.compose("d", gameId, code());
        }

        GameDetails (String code) {
            _code = code;
        }

        /**
         * Look up a GameDetails by its code.
         */
        public static GameDetails getByCode (String code)
        {
            // we could store these in a map, but why bother?
            for (GameDetails detail : values()) {
                if (detail.code().equals(code)) {
                    return detail;
                }
            }
            return GameDetails.INSTRUCTIONS;
        }

        protected String _code;
    }        
}
