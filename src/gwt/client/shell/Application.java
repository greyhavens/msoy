//
// $Id$

package client.shell;

import java.util.HashMap;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.ReferralInfo;

import com.threerings.msoy.web.client.DeploymentConfig;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebIdent;

import client.util.Link;
import client.util.ServiceUtil;

/**
 * Our main application and entry point. This dispatches a requests to the appropriate {@link
 * Page}. Some day it may also do fancy on-demand loading of JavaScript.
 */
public class Application
    implements EntryPoint, HistoryListener
{
    /** Our active invitation if we landed at Whirled from an invite, null otherwise (for use if
     * and when we create an account). */
    public static Invitation activeInvite;

    /**
     * Replace the current page with the one specified.
     */
    public static void replace (String page, String args)
    {
        History.back();
        Link.go(page, args);
    }

    /**
     * Configures our current history token (normally this is done automatically as the user
     * navigates, but sometimes we want to override the current token). This does not take any
     * action based on the token, but the token will be used if the user subsequently logs in or
     * out.
     */
    public static void setCurrentToken (String token)
    {
        _currentToken = token;
    }

    /**
     * When the client logs onto the Whirled as a guest, they let us know what their id is so that
     * if the guest creates an account we can transfer anything they earned as a guest to their
     * newly created account. This is also called if a player attempts to play a game without
     * having first logged into the server.
     */
    public static void setGuestId (int guestId)
    {
        if (CShell.getMemberId() > 0) {
            CShell.log("Warning: got guest id but appear to be logged in? " +
                       "[memberId=" + CShell.getMemberId() + ", guestId=" + guestId + "].");
        } else {
            CShell.ident = new WebIdent();
            CShell.ident.memberId = guestId;
            // TODO: the code that knows how to do this is in MsoyCredentials which is not
            // accessible to GWT currently for unrelated technical reasons
            CShell.ident.token = "G" + guestId;
        }
    }

    /**
     * Returns a partner identifier when we're running in partner cobrand mode, null when we're
     * running in the full Whirled environment.
     */
    public static native String getPartner () /*-{
        return $doc.whirledPartner;
    }-*/;

    /**
     * Returns a reference to the status panel.
     */
    public StatusPanel getStatusPanel ()
    {
        return _status;
    }

    /**
     * Reports a page view event to our analytics engine.
     */
    public void reportEvent (String path)
    {
        _analytics.report(path);
    }

    /**
     * Called when the player logs on (or when our session is validated).
     */
    public void didLogon (SessionData data)
    {
        CShell.creds = data.creds;
        CShell.ident = new WebIdent(data.creds.getMemberId(), data.creds.token);
        _status.didLogon(data);
        WorldClient.didLogon(data.creds);
        Frame.didLogon();

        if (_page != null) {
            _page.didLogon(data.creds);
        } else if (_currentToken != null) {
            onHistoryChanged(_currentToken);
        }
    }

    /**
     * Called when the player logs off.
     */
    public void didLogoff ()
    {
        CShell.creds = null;
        CShell.ident = null;
        _status.didLogoff();
        Frame.didLogoff();

        if (_page == null) {
            // we can now load our starting page
            onHistoryChanged(_currentToken);
        } else {
            Frame.closeClient(false);
            _page.didLogoff();
        }
    }

    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // create our static page mappings (we can't load classes by name in wacky JavaScript land
        // so we have to hardcode the mappings)
        createMappings();

        // initialize our top-level context references
        initContext();

        // set up the callbackd that our flash clients can call
        configureCallbacks(this);

        // create our status panel and initialize the frame
        _status = new StatusPanel(this);
        Frame.init();

        // initialize our GA handler
        _analytics.init();

        // wire ourselves up to the history-based navigation mechanism
        History.addHistoryListener(this);
        _currentToken = History.getToken();

        // validate our session before considering ourselves logged on
        validateSession(CookieUtil.get("creds"));
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        _currentToken = token;

        String page = (token == null || token.equals("")) ? Page.ME : token;
        Args args = new Args();
        int dashidx = token.indexOf("-");
        if (dashidx != -1) {
            page = token.substring(0, dashidx);
            args.setToken(token.substring(dashidx+1));
        }

        // TEMP: migrate old style invites to new style
        if ("invite".equals(page)) {
            token = Args.compose("i", args.get(0, ""));
            args = new Args();
            args.setToken(token);
            page = Page.ME;
        } else if ("optout".equals(page) || "resetpw".equals(page)) {
            token = Args.compose(page, args.get(0, ""), args.get(1, ""));
            args = new Args();
            args.setToken(token);
            page = Page.ACCOUNT;
        } else if ((Page.WORLD.equals(page) || "whirled".equals(page)) &&
                   args.get(0, "").equals("i")) {
            page = Page.ME;
        }
        // END TEMP

        CShell.log("Displaying page [page=" + page + ", args=" + args + "].");

        // pull the affiliate id out of the URL. it will be of the form: "aid_A_V_C", consisting
        //   of three components: the affiliate ID, the entry vector ID, and the creative (ad) ID.
        int aidIdx = args.indexOf("aid");
        int lastIdx = aidIdx + 3;
        if (aidIdx != -1 && args.getArgCount() > lastIdx) {
            String affiliate = args.get(aidIdx + 1, "");
            String vector = args.get(aidIdx + 2, "");
            String creative = args.get(aidIdx + 3, "");

            // remove the "aid" tag and its three values
            token = Args.compose(args.remove(aidIdx, aidIdx + 4));
            args = new Args();
            args.setToken(token);

            // save our tracking info, but don't overwrite old values
            ReferralInfo ref = new ReferralInfo(
                affiliate, vector, creative, ReferralInfo.makeRandomTracker());
            TrackingCookie.save(ref, false);

        } else {
            if (! TrackingCookie.contains()) {
                TrackingCookie.save(ReferralInfo.makeInstance(
                    "", "", "", ReferralInfo.makeRandomTracker()), false);
            }
        }

        // replace the page if necessary
        if (_page == null || !_page.getPageId().equals(page)) {
            // tell any existing page that it's being unloaded
            if (_page != null) {
                _page.onPageUnload();
                _page = null;
            }

            // locate the creator for this page
            Page.Creator creator = _creators.get(page);
            if (creator == null) {
                CShell.log("Page unknown, redirecting to me [page=" + page + "].");
                creator = _creators.get(Page.ME);
                args = new Args();
            }

            // create the entry point and fire it up
            _page = creator.createPage();
            _page.init();
            _page.onPageLoad();

            // tell the page about its arguments
            _page.onHistoryChanged(args);

        } else {
            _page.onHistoryChanged(args);
        }

        // convert the page to GA format and report it to Google Analytics
        reportEvent(args.toPath(page));
    }

    protected void initContext ()
    {
        CShell.app = this;
    }

    /**
     * Makes sure that our credentials are still valid.
     */
    protected void validateSession (String token)
    {
        if (token != null) {
            _usersvc.validateSession(DeploymentConfig.version, token, 1,
                                     new AsyncCallback<SessionData>() {
                public void onSuccess (SessionData data) {
                    if (data == null) {
                        didLogoff();
                    } else {
                        didLogon(data);
                    }
                }
                public void onFailure (Throwable t) {
                    didLogoff();
                }
            });
        } else {
            didLogoff();
        }
    }

    /**
     * Called when a web page component wants to request a chat channel opened in the Flash
     * client, of the given type and name.
     */
    protected boolean openChannelRequest (int type, String name, int id)
    {
        return openChannelNative(type, name, id);
    }

    protected void createMappings ()
    {
        _creators.put(Page.ACCOUNT, client.account.index.getCreator());
        _creators.put(Page.ADMIN, client.admin.index.getCreator());
        _creators.put(Page.GAMES, client.games.index.getCreator());
        _creators.put(Page.HELP, client.help.index.getCreator());
        _creators.put(Page.MAIL, client.mail.index.getCreator());
        _creators.put(Page.ME, client.me.index.getCreator());
        _creators.put(Page.PEOPLE, client.people.index.getCreator());
        _creators.put(Page.SHOP, client.shop.index.getCreator());
        _creators.put(Page.STUFF, client.stuff.index.getCreator());
        _creators.put(Page.SUPPORT, client.support.index.getCreator());
        _creators.put(Page.SWIFTLY, client.swiftly.index.getCreator());
        _creators.put(Page.WHIRLEDS, client.whirleds.index.getCreator());
        _creators.put(Page.WORLD, client.world.index.getCreator());
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks (Application app) /*-{
       $wnd.openChannel = function (type, name, id) {
           app.@client.shell.Application::openChannelRequest(ILjava/lang/String;I)(type, name, id);
       };
       $wnd.onunload = function (event) {
           var client = $doc.getElementById("asclient");
           if (client) {
               client.onUnload();
           }
           return true;
       };
       $wnd.helloWhirled = function () {
            return true;
       };
       $wnd.setWindowTitle = function (title) {
            @client.shell.Frame::setTitle(Ljava/lang/String;)(title);
       };
       $wnd.displayPage = function (page, args) {
           @client.util.Link::go(Ljava/lang/String;Ljava/lang/String;)(page, args);
       };
       $wnd.setGuestId = function (guestId) {
           @client.shell.Application::setGuestId(I)(guestId);
       };
       $wnd.getReferral = function () {
           return @client.shell.TrackingCookie::getAsObject()();
       };
       $wnd.setReferral = function (ref) {
           @client.shell.TrackingCookie::saveAsObject(Ljava/lang/Object;Z)(ref, true);
       };
       $wnd.toggleClientHeight = function () {
           @client.util.FlashClients::toggleClientHeight()();
       }
    }-*/;

    /**
     * The native complement to openChannel.
     */
    protected static native boolean openChannelNative (int type, String name, int id) /*-{
        var client = $doc.getElementById("asclient");
        if (client) {
            client.openChannel(type, name, id);
            return true;
        }
        return false;
    }-*/;

    protected Page _page;
    protected HashMap<String, Page.Creator> _creators = new HashMap<String, Page.Creator>();
    protected Analytics _analytics = new Analytics();

    protected StatusPanel _status;

    protected static String _currentToken = "";

    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
