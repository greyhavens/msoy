//
// $Id$

package client.frame;

import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.History;
import com.google.gwt.user.client.HistoryListener;
import com.google.gwt.user.client.rpc.AsyncCallback;

import com.threerings.gwt.util.CookieUtil;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.ReferralInfo;
import com.threerings.msoy.web.client.WebUserService;
import com.threerings.msoy.web.client.WebUserServiceAsync;
import com.threerings.msoy.web.data.SessionData;
import com.threerings.msoy.web.data.WebIdent;

import client.shell.Args;
import client.shell.CShell;
import client.shell.Frame;
import client.shell.Session;
import client.shell.TrackingCookie;
import client.util.ServiceUtil;

/**
 * Handles the outer shell of the Whirled web application. Loads pages into an iframe and also
 * handles displaying the Flash client.
 */
public class FrameEntryPoint
    implements EntryPoint, HistoryListener, Session.Observer
{
    // from interface EntryPoint
    public void onModuleLoad ()
    {
        // initialize the frame
        CShell.frame = new Frame();

        // listen for logon/logoff
        Session.addObserver(this);

        // set up the callbackd that our flash clients can call
        configureCallbacks(this, CShell.frame);

        // validate our session before considering ourselves logged on
        validateSession(CookieUtil.get("creds"));

        // wire ourselves up to the history-based navigation mechanism
        History.addHistoryListener(this);
        _currentToken = History.getToken();
    }

    // from interface HistoryListener
    public void onHistoryChanged (String token)
    {
        _currentToken = token;

        String page = (token == null || token.equals("")) ? "me" /* TODO */ : token;
        Args args = new Args();
        int dashidx = token.indexOf("-");
        if (dashidx != -1) {
            page = token.substring(0, dashidx);
            args.setToken(token.substring(dashidx+1));
        }

        CShell.log("Displaying page [page=" + page + ", args=" + args + "].");

        // pull the affiliate id out of the URL. it will be of the form: "aid_A_V_C", consisting of
        // three components: the affiliate ID, the entry vector ID, and the creative (ad) ID.
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
            if (!TrackingCookie.exists()) {
                String tracker = ReferralInfo.makeRandomTracker();
                TrackingCookie.save(ReferralInfo.makeInstance("", "", "", tracker), false);
            }
        }

        // recreate the page token which we'll pass through to the page (or if it's being loaded
        // for the first time, it will request in a moment with a call to getPageToken)
        _pageToken = Args.compose(args.splice(0));

        // replace the page if necessary
        if (_pageId == null || !_pageId.equals(page)) {
            // TODO: set our Page iframe to this page
            _pageId = page;
        } else {
            // TODO: pass our arguments through to our iframed page
        }

//         // convert the page to GA format and report it to Google Analytics
//         _analytics.report(args.toPath(page));
    }

    // from interface Session.Observer
    public void didLogon (SessionData data)
    {
        CShell.creds = data.creds;
        CShell.ident = new WebIdent(data.creds.getMemberId(), data.creds.token);
        // WorldClient.didLogon(data.creds);

        if (_pageId != null) {
            // TODO: tell our page that we just logged on
        } else if (_currentToken != null && !data.justCreated) {
            onHistoryChanged(_currentToken);
        }

        // TEMP: show a header
        CShell.frame.setHeaderVisible(true);
    }

    // from interface Session.Observer
    public void didLogoff ()
    {
        CShell.creds = null;
        CShell.ident = null;

        if (_pageId == null) {
            // we can now load our starting page
            onHistoryChanged(_currentToken);
        } else {
            CShell.frame.closeClient(false);
            // TODO: tell our page that we just logged off
        }

        // TEMP: show a header
        CShell.frame.setHeaderVisible(true);
    }

    /**
     * Makes sure that our credentials are still valid.
     */
    protected void validateSession (String token)
    {
        if (token == null) {
            Session.didLogoff();
            return;
        }
        AsyncCallback<SessionData> onValidate = new AsyncCallback<SessionData>() {
            public void onSuccess (SessionData data) {
                if (data == null) {
                    Session.didLogoff();
                } else {
                    Session.didLogon(data);
                }
            }
            public void onFailure (Throwable t) {
                Session.didLogoff();
            }
        };
        _usersvc.validateSession(DeploymentConfig.version, token, 1, onValidate);
    }

    protected String getPageToken ()
    {
        return _pageToken;
    }

    /**
     * Configures top-level functions that can be called by Flash.
     */
    protected static native void configureCallbacks (FrameEntryPoint entry, Frame frame) /*-{
       $wnd.onunload = function (event) {
           var client = $doc.getElementById("asclient");
           if (client) {
               client.onUnload();
           }
           return true;
       };
       $wnd.getPageToken = function () {
           return entry.@client.frame.FrameEntryPoint::getPageToken()();
       };
       $wnd.helloWhirled = function () {
            return true;
       };
       $wnd.setWindowTitle = function (title) {
            frame.@client.shell.Frame::setTitle(Ljava/lang/String;)(title);
       };
       $wnd.displayPage = function (page, args) {
           @client.util.Link::go(Ljava/lang/String;Ljava/lang/String;)(page, args);
       };
       $wnd.setGuestId = function (guestId) {
           @client.shell.CShell::setGuestId(I)(guestId);
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

    protected String _currentToken = "";
    protected String _pageToken = "";
    protected String _pageId;

    protected static final WebUserServiceAsync _usersvc = (WebUserServiceAsync)
        ServiceUtil.bind(GWT.create(WebUserService.class), WebUserService.ENTRY_POINT);
}
