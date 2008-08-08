//
// $Id$

package client.util;

import client.shell.Args;
import client.shell.Pages;

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

    public static String gameDetail (int gameId, GameDetails tab)
    {
        return Args.compose("d", ""+gameId, tab.code());
    }

    public static void viewItem (byte type, int itemId)
    {
        Link.go(Pages.STUFF, Args.compose(""+type, "-1", ""+itemId));
    }

    public static void editItem (byte type, int itemId)
    {
        Link.go(Pages.STUFF, Args.compose("e", ""+type, ""+itemId));
    }

    public static void remixItem (byte type, int itemId)
    {
        Link.go(Pages.STUFF, Args.compose("r", ""+type, ""+itemId));
    }

    public static void remixCatalogItem (
        byte type, int itemId, int catalogId, int flowCost, int goldCost)
    {
        Link.go(Pages.STUFF, Args.compose(new String[] {
                    "r", ""+type, ""+itemId, ""+catalogId, ""+flowCost, ""+goldCost }));
    }

    public static void createItem (byte type, byte ptype, int pitemId)
    {
        Link.go(Pages.STUFF, Args.compose(new String[] { "c", ""+type, ""+ptype, ""+pitemId }));
    }
}
