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
     * Extracts and returns the page from the supplied history token. Does not return null.
     * @throws IllegalArgumentException if the token is not formatted correctly or the page
     * component does not exactly match a page name
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
    public String makeURL (Object... args)
    {
        return makeGoURL("go", -1, args);
    }

    /**
     * Creates a link to the specified page and arguments. The link will start with /# and will not
     * be prefixed by the server URL. Prepend DeploymentConfig.serverURL if that is desired.
     */
    public String makeLink (Object... args)
    {
        return "/#" + makeToken(args);
    }

    /**
     * Creates a link token for the specified page and arguments. This token can be passed to
     * History.newItem.
     */
    public String makeToken (Object... args)
    {
        String token = getPath();
        String atok = Args.compose(args).toToken();
        return (atok.length() == 0) ? token : (token + "-" + atok);
    }

    /**
     * Creates a url that is compatible with mail readers and when followed will make the follower
     * an affiliate of the given member and then redirect to the given page with the given args.
     * The member id may be zero, in which case the link will only redirect with no affiliation.
     */
    public String makeAffiliateURL (int memberId, Object... args)
    {
        return makeGoURL("welcome", memberId, args);
    }

    /**
     * Creates a url that is compatible with mail readers and when followed will make the follower
     * an affiliate of the given member and then redirect to the given page with the given args.
     * If the follower eventually registers, a friend request will automatically be sent to the
     * given member. The member id may be zero, in which case the link will only redirect with no
     * affiliation or friending.
     */
    public String makeFriendURL (int memberId, Object... args)
    {
        return makeGoURL("friend", memberId, args);
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

    protected String makeGoURL (String servlet, int memberId, Object... args)
    {
        String token = makeToken(args);
        String url = DeploymentConfig.serverURL + servlet;
        if (memberId != -1) {
            url += "/" + memberId;
        }
        if (this != LANDING || args.length > 0) {
            url += "/" + token;
        }
        return url;
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
