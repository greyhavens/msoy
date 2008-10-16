//
// $Id$

package com.threerings.msoy.web.client;


/**
 * Enumerates all of our available pages.
 */
public enum Pages
{
    ACCOUNT(Tabs.ME),
    ADMINZ(Tabs.ME),
    CREATE(null), // TODO: Tabs.CREATE
    FAVORITES(Tabs.SHOP),
    GAMES(Tabs.GAMES),
    HELP(Tabs.HELP),
    LANDING(null),
    MAIL(Tabs.ME),
    ME(Tabs.ME),
    PEOPLE(Tabs.ME),
    ROOM(Tabs.WHIRLEDS),
    SHOP(Tabs.SHOP),
    STUFF(Tabs.STUFF),
    SUPPORT(Tabs.HELP),
    SWIFTLY(null), // TODO: Tabs.CREATE
    WHIRLEDS(Tabs.WHIRLEDS),
    WORLD(null);

    public String getPath () {
        return toString().toLowerCase();
    }

    public Tabs getTab () {
        return _tab;
    }

    Pages (Tabs tab) {
        _tab = tab;
    }

    protected Tabs _tab;
}
