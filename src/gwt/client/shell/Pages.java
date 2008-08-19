//
// $Id$

package client.shell;

/**
 * Enumerates all of our available pages.
 */
public enum Pages
{
    ACCOUNT(Frame.Tabs.ME),
    ADMINZ(Frame.Tabs.ME),
    CREATE(null), // TODO: Frame.Tabs.CREATE
    FAVORITES(Frame.Tabs.SHOP),
    GAMES(Frame.Tabs.GAMES),
    HELP(Frame.Tabs.HELP),
    LANDING(null),
    MAIL(Frame.Tabs.ME),
    ME(Frame.Tabs.ME),
    PEOPLE(Frame.Tabs.FRIENDS),
    ROOM(Frame.Tabs.WHIRLEDS),
    SHOP(Frame.Tabs.SHOP),
    STUFF(Frame.Tabs.ME),
    SUPPORT(Frame.Tabs.HELP),
    SWIFTLY(null), // TODO: Frame.Tabs.CREATE
    WHIRLEDS(Frame.Tabs.WHIRLEDS),
    WORLD(null);

    public String getPath () {
        return toString().toLowerCase();
    }

    public Frame.Tabs getTab () {
        return _tab;
    }

    Pages (Frame.Tabs tab) {
        _tab = tab;
    }

    protected Frame.Tabs _tab;
}
