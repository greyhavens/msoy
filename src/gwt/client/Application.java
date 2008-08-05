//
// $Id$

package client;

import java.util.HashMap;
import java.util.Map;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.core.client.JavaScriptObject;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.web.client.MemberService;
import com.threerings.msoy.web.client.MemberServiceAsync;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.Invitation;
import com.threerings.msoy.web.data.SessionData;

import client.shell.Analytics;
import client.shell.Args;
import client.shell.CShell;
import client.shell.Frame;
import client.shell.Page;
import client.shell.Pages;
import client.shell.Session;
import client.shell.ShellFrameImpl;
import client.shell.TrackingCookie;
import client.shell.WorldClient;
import client.util.Link;
import client.util.MsoyCallback;
import client.util.ServiceUtil;
import client.util.events.FlashEvent;
import client.util.events.FlashEvents;

/**
 * Our main application and entry point. This dispatches a requests to the appropriate {@link
 * Page}. Some day it may also do fancy on-demand loading of JavaScript.
 */
public class Application
    implements EntryPoint, HistoryListener, Session.Observer
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // create our static page mappings
        createMappings();

        // set up our CShell singleton
        CShell.init(new ShellFrameImpl() {
            public Invitation getActiveInvitation () {
                return _activeInvite;
            }
        });

        // set up the callbackd that our flash clients can call
        configureCallbacks(this, CShell.frame);

        // register as a session observer
        Session.addObserver(this);

        // initialize our GA handler
        _analytics.init();

        // wire ourselves up to the history-based navigation mechanism
        History.addHistoryListener(this);
        _currentToken = History.getToken();

        // validate our session which will dispatch a didLogon or didLogoff
        Session.validate();
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        _currentToken = token;

        Pages page;
        try {
            page = Enum.valueOf(Pages.class, token.split("-")[0].toUpperCase());
        } catch (Exception e) {
            page = getLandingPage();
        }
        Args args = new Args();
        int dashidx = token.indexOf("-");
        if (dashidx != -1) {
            args.setToken(token.substring(dashidx+1));
        }

        CShell.log("Displaying page [page=" + page + ", args=" + args + "].");

        // do some special processing if this is an invitation link
        if (page == Pages.ME && args.get(0, "").equals("i") && CShell.isGuest()) {
            // load up the invitation information and save it
            String inviteId = args.get(1, "");
            if (_activeInvite == null || !_activeInvite.inviteId.equals(inviteId)) {
                _membersvc.getInvitation(inviteId, true, new MsoyCallback<Invitation>() {
                    public void onSuccess (Invitation invite) {
                        _activeInvite = invite;
                    }
                });
            }
            // and send them to the landing page
            Link.go(Pages.LANDING, "");
            return;
        }

        // pull the affiliate id out of the URL. it will be of the form: "aid_A_V_C", consisting
        // of three components: the affiliate ID, the entry vector ID, and the creative (ad) ID.
        int aidIdx = args.indexOf("aid");
        if (aidIdx != -1) {
            String affiliate = args.get(aidIdx + 1, "");
            String vector = args.get(aidIdx + 2, "");
            String creative = args.get(aidIdx + 3, "");

            // remove the "aid" tag and its three values
            token = Args.compose(args.remove(aidIdx, aidIdx + 4));
            args = new Args();
            args.setToken(token);

            // save our tracking info, but don't overwrite old values
            maybeCreateReferral(affiliate, vector, creative);

        } else {
            // try to load the HTTP referrer info, and manufacture a new referral from that.
            String ref = CookieUtil.get(ReferralInfo.REFERER_COOKIE);
            if (ref != null && ref.length() > 0) {
                maybeCreateReferral(ref, token, "");
            } else {
                maybeCreateReferral("", "", "");
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
                creator = _creators.get(Pages.ME);
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
        _analytics.report(args.toPath(page));
    }

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        WorldClient.didLogon(data.creds);

        // if we're on any account page, and we logon, we want to go to the me page
        if (_page != null && Pages.ACCOUNT.equals(_page.getPageId())) {
            CShell.frame.navigateTo(Pages.ME.getPath());
        } else  {
            // tell any existing page that it's being unloaded
            if (_page != null) {
                _page.onPageUnload();
                _page = null;
            }
            onHistoryChanged(_currentToken);
        }
    }

    // from interface Session.Observer
    public void didLogoff ()
    {
        // tell any existing page that it's being unloaded
        if (_page != null) {
            _page.onPageUnload();
            _page = null;
        }
        // reload the current page
        onHistoryChanged(_currentToken);
        // close the Flash client if it's open
        CShell.frame.closeClient();
    }

    /**
     * If a tracking cookie doesn't already exist, creates a brand new one
     * with the supplied referral info and a brand new tracking number.
     * Also tells the server to log this as an event.
     */
    protected void maybeCreateReferral (String affiliate, String vector, String creative)
    {
        if (! TrackingCookie.exists()) {
            ReferralInfo ref =
                ReferralInfo.makeInstance(
                    affiliate, vector, creative, ReferralInfo.makeRandomTracker());
            TrackingCookie.save(ref, false);
            _membersvc.trackReferralCreation(ref, null);
        }
    }

    protected Pages getLandingPage ()
    {
        return CShell.isGuest() ? Pages.LANDING : Pages.ME;
    }

    protected void createMappings ()
    {
        _creators.put(Pages.ACCOUNT, client.account.AccountPage.getCreator());
        _creators.put(Pages.ADMIN, client.admin.AdminPage.getCreator());
        _creators.put(Pages.GAMES, client.games.GamesPage.getCreator());
        _creators.put(Pages.HELP, client.help.HelpPage.getCreator());
        _creators.put(Pages.LANDING, client.landing.LandingPage.getCreator());
        _creators.put(Pages.MAIL, client.mail.MailPage.getCreator());
        _creators.put(Pages.ME, client.me.MePage.getCreator());
        _creators.put(Pages.PEOPLE, client.people.PeoplePage.getCreator());
        _creators.put(Pages.SHOP, client.shop.ShopPage.getCreator());
        _creators.put(Pages.STUFF, client.stuff.StuffPage.getCreator());
        _creators.put(Pages.SUPPORT, client.support.SupportPage.getCreator());
        _creators.put(Pages.SWIFTLY, client.swiftly.SwiftlyPage.getCreator());
        _creators.put(Pages.WHIRLEDS, client.whirleds.WhirledsPage.getCreator());
        _creators.put(Pages.WORLD, client.world.WorldPage.getCreator());
    }

    /**
     * Called when Flash wants us to display a page.
     */
    protected static void displayPage (String page, String args)
    {
    	try {
            Link.go(Enum.valueOf(Pages.class, page.toUpperCase()), args);
    	} catch (Exception e) {
            CShell.log("Unable to display page from Flash [page=" + page +
                       ", args=" + args + "].", e);
    	}
    }

    /**
     * Called when Flash wants us to dispatch an event.
     */
    protected static void triggerEvent (String eventName, JavaScriptObject args)
    {
        FlashEvent event = FlashEvents.createEvent(eventName, args);
        if (event != null) {
            FlashEvents.internalDispatchEvent(event);
        }
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks (Application app, Frame frame) /*-{
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
            frame.@client.shell.Frame::setTitle(Ljava/lang/String;)(title);
       };
       $wnd.displayPage = function (page, args) {
           @client.Application::displayPage(Ljava/lang/String;Ljava/lang/String;)(page, args);
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
       $wnd.triggerFlashEvent = function (eventName, args) {
           @client.Application::triggerEvent(Ljava/lang/String;Lcom/google/gwt/core/client/JavaScriptObject;)(eventName, args);
       }
    }-*/;

    protected Page _page;
    protected Invitation _activeInvite;
    protected Analytics _analytics = new Analytics();

    protected Map<Pages, Page.Creator> _creators = new HashMap<Pages, Page.Creator>();

    protected static String _currentToken = "";

    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
    protected static final MemberServiceAsync _membersvc = (MemberServiceAsync)
        ServiceUtil.bind(GWT.create(MemberService.class), MemberService.ENTRY_POINT);
}
