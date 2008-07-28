//
// $Id$

package client.util;

import client.shell.Args;

/**
 * A place where we can encapsulate the creation of arguments that link to complex pages in
 * Whirled. We do this here so that we don't have references between otherwise unrelated classes
 * and services introduced by the fact that we want a Hyperlink from somewhere in Whirled to
 * somewhere (from a code standpoint) totally unrelated.
 */
public class NaviUtil
{
    public enum GameDetails {
        INSTRUCTIONS("i"), COMMENTS("c"), TROPHIES("t"), MYRANKINGS("mr"),
        TOPRANKINGS("tr"), METRICS("m"), LOGS("l");

        public String code () {
            return _code;
        }

        GameDetails (String code) {
            _code = code;
        }

        protected String _code;
    }        

    public static String gameDetail (int gameId, GameDetails tab)
    {
        return Args.compose("d", ""+gameId, tab.code());
    }
}
