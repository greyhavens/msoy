//
// $Id$

package com.threerings.msoy.web.gwt;

import com.threerings.msoy.data.all.DeploymentConfig;

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
    LANDACC(null, "account"), // account with no header
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
    ISSUES(Tabs.GROUPS),
    WORLD(null);

    /**
     * Extracts and returns the page from the supplied history token.
     */
    public static Pages fromHistory (String historyToken)
    {
        int didx = historyToken.indexOf("-");
        String pstr = (didx == -1) ? historyToken : historyToken.substring(0, didx);
        return valueOf(pstr.toUpperCase());
    }

    /**
     * Creates a link to the specified page and arguments. The link will contain the server URL and
     * will use the safer <code>http://www.whirled.com/go/args</code> form. Thus this should be
     * used for URLs that are going to be sent off outside of Whirled. If that is not desired, use
     * {@link #makeLink}.
     */
    public static String makeURL (Pages page, String args)
    {
        return DeploymentConfig.serverURL + "go/" + makeToken(page, args);
    }

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
     * Creates a url that is compatible with mail readers and when followed will make the follower
     * an affiliate of the given member and then redirect to the given page with the given args.
     * The member id may be zero, in which case the link will only redirect with no affiliation.
     */
    public static String makeAffiliateURL (int memberId, Pages page, String args)
    {
        return DeploymentConfig.serverURL + "welcome/" + memberId + "/" + makeToken(page, args);
    }

    /**
     * Returns the path for this page that is used in URLs.
     */
    public String getPath ()
    {
        return (_path == null) ? toString().toLowerCase() : _path;
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

    Pages (Tabs tab, String path) {
        _tab = tab;
        _path = path;
    }

    protected Tabs _tab;
    protected String _path;
}
