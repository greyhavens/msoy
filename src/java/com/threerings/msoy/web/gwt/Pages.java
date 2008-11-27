//
// $Id$

package com.threerings.msoy.web.gwt;

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
    ROOMS(Tabs.ROOMS),
    SHOP(Tabs.SHOP),
    STUFF(Tabs.STUFF),
    SUPPORT(Tabs.HELP),
    SWIFTLY(null), // TODO: Tabs.CREATE
    GROUPS(Tabs.GROUPS),
    WORLD(null);

    /**
     * Creates a link to the specified page and arguments. The link will start with /# and will not
     * be prefixed by the server URL. Prepend DeploymentConfig.serverURL if that is desired.
     */
    public static String makeLink (Pages page, String args)
    {
        return "/#" + makeToken(page, args);
    }

    /**
     * Creates a link token for the specified page and arguments. This token can be passed to
     * History.newItem.
     */
    public static String makeToken (Pages page, String args)
    {
        String token = (page == null) ? "" : page.getPath();
        if (args != null && args.length() > 0) {
            token = token + "-" + args;
        }
        return token;
    }

    /**
     * Returns the path for this page that is used in URLs.
     */
    public String getPath ()
    {
        return toString().toLowerCase();
    }

    /**
     * Returns the tab that should be shown as selected when displaying this page.
     */
    public Tabs getTab ()
    {
        return _tab;
    }

    Pages (Tabs tab) {
        _tab = tab;
    }

    protected Tabs _tab;
}
