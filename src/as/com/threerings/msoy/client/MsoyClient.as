//
// $Id$

package com.threerings.msoy.client {
import flash.display.Stage;
import flash.display.StageDisplayState;
import flash.events.ContextMenuEvent;
import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.external.ExternalInterface;
import flash.media.SoundMixer;
import flash.media.SoundTransform;
import flash.system.Capabilities;
import flash.system.Security;
import flash.ui.ContextMenu;
import flash.utils.getTimer;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.ValueEvent;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.client.SessionObserver;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.net.Credentials;

import com.threerings.crowd.client.CrowdClient;
import com.threerings.crowd.data.BodyObject;

import com.threerings.ui.MenuUtil;

import com.threerings.orth.notify.data.Notification;

import com.threerings.msoy.data.Address;
import com.threerings.msoy.data.Embedding;
import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.MsoyMarshaller;
import com.threerings.msoy.data.UberClientModes;
import com.threerings.msoy.data.all.CloudfrontMediaDesc;
import com.threerings.msoy.data.all.HashMediaDesc;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.item.data.all.Item_UsedAs;

/**
 * Dispatched when the client is minimized or unminimized.
 *
 * @eventType com.threerings.msoy.client.MsoyClient.MINI_WILL_CHANGE
 */
[Event(name="miniWillChange", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when the GWT page changes. The event.value will be an <code>Address</code> object or
 * null if the GWT page was closed. No attempt is made to remove redundancy so multiple identical
 * events may be sent in some circumstances.
 *
 * @eventType com.threerings.msoy.client.MsoyClient.GWT_PAGE_CHANGED
 */
[Event(name="msoy.setPage", type="com.threerings.util.ValueEvent")]

/**
 * A client shared by both our world and game incarnations.
 */
public /*abstract*/ class MsoyClient extends CrowdClient
{
    public static const log :Log = Log.getLog(MsoyClient);

    /**
     * An event dispatched when the client is minimized or unminimized.
     *
     * @eventType miniWillChange
     */
    public static const MINI_WILL_CHANGE :String = "miniWillChange";

    /**
     * An event dispatched when a web page is opened.
     *
     * @eventType msoy.pageOpened
     */
    public static const GWT_PAGE_CHANGED :String = "msoy.setPage";

    // statically reference classes we require
    CloudfrontMediaDesc;
    HashMediaDesc;
    MsoyBootstrapData;
    MsoyAuthResponseData;
    MsoyService;
    MsoyMarshaller;
    LurkerName;

    public function MsoyClient (stage :Stage)
    {
        // set this here, it is ignored in the usual decl-assign syntax (?)
        _embedding = Embedding.NONE;
        _featuredPlaceView = UberClient.isFeaturedPlaceView();
        super(null);
        _stage = stage;
        setVersion(DeploymentConfig.version);

        LoggingTargets.configureLogging(_ctx);
        log.info("Starting up", "capabilities", Capabilities.serverString,
            "preloader", (getTimer() - Preloader.preloaderStart));

        // instantiate the mechanism by which MemberObject is loaded
        _loader = new BodyLoader(this);

        // wire up our JavaScript bridge functions
        try {
            if (ExternalInterface.available) {
                configureExternalFunctions();
            }
        } catch (err :Error) {
            // nada: ExternalInterface isn't there. Oh well!
            log.warning("Unable to configure external functions.");
        }

        log.info("Detected embedding", "value", _embedding);

        // configure our starting global sound transform
        if (_featuredPlaceView) {
            SoundMixer.soundTransform = new SoundTransform(0); // muted
        } else {
            Prefs.setEmbedded(isEmbedded());
        }

        // now create our credentials and context (NOTE: we do this after we've wired up our
        // external functions which determines whether or not we're embedded)
        _creds = createStartupCreds(null);
        _ctx = createContext();

        _loader.init(_ctx);

        // wire up a listener for bridge events from the embed stub
        configureBridgeFunctions(UberClient.getApplication().loaderInfo.sharedEvents);
        // then report to the embed stub that we're ready to receive bridge events
        UberClient.getApplication().loaderInfo.sharedEvents.dispatchEvent(
            new Event(UberClientModes.CLIENT_READY, true));

        // prior to logging on to a server, set up our security policy for that server
        _loader.addClientObserver(
            new ClientAdapter(clientWillLogon, clientDidLogon, null, clientDidLogoff));

        // configure our server and port info
        setServer(getServerHost(), getServerPorts());

        // set up a context menu that blocks funnybiz on the stage
        var menu :ContextMenu = new ContextMenu();
        menu.hideBuiltInItems();
        UberClient.getApplication().contextMenu = menu;
        menu.addEventListener(ContextMenuEvent.MENU_SELECT, contextMenuWillPopUp);
    }

    public function get loader () :BodyLoader
    {
        return _loader;
    }

    // from Client
    override public function addClientObserver (observer :SessionObserver) :void
    {
        // hijack all normal client observer requests and divert them to our special loader
        _loader.addClientObserver(observer);
    }

    // from Client
    override public function removeClientObserver (observer :SessionObserver) :void
    {
        // hijack all normal client observer requests and divert them to our special loader
        _loader.removeClientObserver(observer);
    }

    public function addRealClientObserver (observer :SessionObserver) :void
    {
        super.addClientObserver(observer);
    }

    // from Client
    override public function gotBootstrap (data :BootstrapData, omgr :DObjectManager) :void
    {
        super.gotBootstrap(data, omgr);

        // see if we have a warning message to display
        var rdata :MsoyAuthResponseData = (getAuthResponseData() as MsoyAuthResponseData);
        if (rdata.warning != null) {
            new WarningDialog(_ctx, rdata.warning);
        }

        var mbData :MsoyBootstrapData = MsoyBootstrapData(data);
        if (mbData.mutedMemberIds != null) {
            _ctx.getMuteDirector().setMutedMemberIds(mbData.mutedMemberIds);
        }
    }

    // from CrowdClient
    override public function bodyOf () :BodyObject
    {
        return _loader.getBody();
    }

    /**
     * Return the Stage.
     */
    public function getStage () :Stage
    {
        return _stage;
    }

    /**
     * Find out whether this client is embedded in a non-whirled page. Note that if we are running
     * in the Whirled facebook application, this will return true, even though it is in theory
     * capabale of displaying the whole of whirled.com. To get the specific nature of the embedding,
     * use <code>getEmbedding</code>.
     */
    public function isEmbedded () :Boolean
    {
        return _embedding != Embedding.NONE;
    }

    /**
     * Gets our embedding.
     */
    public function getEmbedding () :Embedding
    {
        return _embedding;
    }

    /**
     * Requests that GWT set the window title.
     */
    public function setWindowTitle (title :String) :void
    {
        try {
            if (ExternalInterface.available && _embedding.hasGWT() && !_featuredPlaceView) {
                ExternalInterface.call("setWindowTitle", title);
            }
        } catch (err :Error) {
            log.warning("setWindowTitle failed", err);
        }
    }

    /**
     * Find out if we're currently working in mini-land or not.  Other components should be able to
     * check this value after they detect that the flash player's size has changed, to discover our
     * status in this regard.
     */
    public function isMinimized () :Boolean
    {
        return _minimized;
    }

    /**
     * Returns true if we are running in chromeless mode (no header and control bar), false if not.
     */
    public function isChromeless () :Boolean
    {
        return UberClient.isFeaturedPlaceView() ||
            (_embedding.isChromelessWhenMinimized() && _minimized);
    }

    /**
     * Notifies our JavaScript shell that the flash client should be cleared out.
     */
    public function closeClient () :void
    {
        try {
            if (ExternalInterface.available && _embedding.hasGWT() && !_featuredPlaceView) {
                ExternalInterface.call("clearClient");
            }
        } catch (err :Error) {
            log.warning("ExternalInterface.call('clearClient') failed", err);
        }
    }

    /**
     * Helper to dispatch item usage changes to GWT.
     */
    public function itemUsageChangedToGWT (itemType :int, itemId :int, usage :Item_UsedAs,
                                           loc :int) :void
    {
        // TODO: can the dispatch handle Enum types? We'd have to change it on the GWT side too
        dispatchEventToGWT("itemUsageChanged", [ itemType, itemId, usage.toByte(), loc ]);
    }

    /**
     * Dispatches an event to GWT.
     */
    public function dispatchEventToGWT (eventName :String, eventArgs :Array) :void
    {
        try {
            if (ExternalInterface.available && _embedding.hasGWT()) {
                ExternalInterface.call("triggerFlashEvent", eventName, eventArgs);
            }
        } catch (err :Error) {
            log.warning("triggerFlashEvent failed", "event", eventName, "args", eventArgs, err);
        }
    }

    /**
     * Get the int testGroup for this visitor and return it in the listener.
     */
    public function getABTestGroup (
        test :String, logEvent :Boolean, listener :InvocationService_ResultListener) :void
    {
        var msvc :MsoyService = requireService(MsoyService) as MsoyService;
        msvc.getABTestGroup(test, logEvent, listener);
    }

    /**
     * Returns visitor tracking info from the client cookies (GWT) if available.
     */
    public function getVisitorId () :String
    {
        if (!isEmbedded() && ExternalInterface.available) {
            try {
                log.debug("Querying browser cookie for visitor tracking info");
                var result :Object = ExternalInterface.call("getVisitorId");
                if (result != null) {
                    return result as String;
                }
            } catch (e :Error) {
                log.warning("ExternalInterface.call('getVisitorId') failed", e);
            }
        }
        return null;
    }

    /**
     * Returns a two element array containing [ facebookId, facebookSession ]. The array may be
     * null and the session token may be the empty string if we don't know this information.
     */
    public function getFacebookInfo () :Array
    {
        if (!isEmbedded() && ExternalInterface.available) {
            try {
                var id :String = ExternalInterface.call("getFacebookId") as String;
                var session :String = ExternalInterface.call("getFacebookSession") as String;
                return (id == null) ? null : [ id, session ];
            } catch (e :Error) {
                log.warning("ExternalInterface.call('getFacebookId') failed", e);
            }
        }
        return null;
    }

    /**
     * Returns the affiliate idq provided to the Flash client in our movie parameters. Will be 0 to
     * indicate that we have no affiliate or a positive integer.
     */
    public function getAffiliateId () :int
    {
        return int(MsoyParameters.get()["aff"]);
    }

    /**
     * Returns the entry vector configured in our Flash parameters, or null.
     */
    public function getEntryVector () :String
    {
        return MsoyParameters.get()[VisitorInfo.VECTOR_ID] as String;
    }

    /**
     * Reports that the user took an action in an A/B test.
     */
    public function trackTestAction (test :String, action :String) :void
    {
        var msvc :MsoyService = getService(MsoyService) as MsoyService;
        if (msvc != null) {
            msvc.trackTestAction(test, action);
        } else {
            log.warning("Dropping test action", "test", test, "action", action);
        }
    }

    /**
     * Any time we're about to connect to a server, this method must be called. It loads the
     * appropriate security policy file for the host in question and ensures that we don't do it
     * more than once per host (which sometimes causes weirdness).
     */
    public function willConnectToServer (hostname :String) :void
    {
        if (!_loadedPolicies[hostname]) {
            var url :String = "xmlsocket://" + hostname + ":" + DeploymentConfig.socketPolicyPort;
            log.info("Loading security policy", "url", url);
            Security.loadPolicyFile(url);
            _loadedPolicies[hostname] = true;
        }
    }

    /**
     * Called just before we logon to a server.
     */
    protected function clientWillLogon (event :ClientEvent) :void
    {
        willConnectToServer(getHostname());
    }

    /**
     * Called just after we logon to a server.
     */
    protected function clientDidLogon (event :ClientEvent) :void
    {
        // these are things we only want to do when we're really logging on
        if (event.isSwitchingServers()) {
            return;
        }

        var member :MemberObject = _ctx.getMemberObject();
        if (_featuredPlaceView || member == null) {
            return;
        }

        // don't link to groups for facebook because they have no way to return the portal
        if (!member.isPermaguest() && _embedding != Embedding.FACEBOOK) {
            // for members on the web site, see if we want to tell them about release notes
            if (Prefs.setBuildTime(DeploymentConfig.buildTime)) {
                _ctx.getNotificationDirector().addGenericNotification(
                    MessageBundle.tcompose("m.new_release", DeploymentConfig.announceGroup),
                    Notification.SYSTEM);
            }
        }
    }

    /**
     * Called after we log off.
     */
    protected function clientDidLogoff (event :ClientEvent) :void
    {
    }

    /**
     * Configure any external functions that we wish to expose to JavaScript.
     */
    protected function configureExternalFunctions () :void
    {
        _embedding = Embedding.OTHER;

        ExternalInterface.addCallback("onUnload", externalOnUnload);
        ExternalInterface.addCallback("setMinimized", externalSetMinimized);
        ExternalInterface.addCallback("isConnected", externalIsConnected);
        ExternalInterface.addCallback("setPage", externalSetPage);

        try {
            if (ExternalInterface.call("helloWhirled") as Boolean) {
                // TODO: GWT has a ClientMode, we have an Embedding, should they be the same?
                // TODO: we will also need the app id at some point
                var clientMode :String = ExternalInterface.call("getClientMode");
                switch (clientMode) {
                    case "FB_GAMES": _embedding = Embedding.FACEBOOK; break;
                    case "FB_ROOMS": _embedding = Embedding.FACEBOOK_ROOMS; break;
                    case "WHIRLED_DJ": _embedding = Embedding.WHIRLED_DJ; break;
                    default: _embedding = Embedding.NONE; break;
                }
            }
        } catch (err :Error) {
            // we are embedded but not anywhere specific, default to OTHER
        }
    }

    /**
     * Exposed to JavaScript so that it may notify us when we're leaving the page.
     */
    protected function externalOnUnload () :void
    {
        log.info("Client unloaded. Logging off.");
        logoff(false);
    }

    /**
     * Exposed to javascript so that it may let us know when we've been pushed out of the way.
     */
    protected function externalSetMinimized (minimized :Boolean) :void
    {
        dispatchEvent(new ValueEvent(MINI_WILL_CHANGE, _minimized = minimized));
        if (minimized) {
            _ctx.getMsoyController().handleSetDisplayState(StageDisplayState.NORMAL);
        }
    }

    /**
     * Exposed to javascript: are we connected?
     */
    protected function externalIsConnected () :Boolean
    {
        return _ctx.getClient().isLoggedOn();
    }

    protected function externalSetPage (pagePath :String, token :String) :void
    {
        dispatchEvent(new ValueEvent(GWT_PAGE_CHANGED,
            pagePath == null ? null : Address.fromToken(pagePath, token)));
    }

    /**
     * Wires up event listeners for any bridge events we expect to receive from the embedstub.
     */
    protected function configureBridgeFunctions (dispatcher :IEventDispatcher) :void
    {
        // nothing by default
    }

    /**
     * Creates the context we'll use with this client.
     */
    protected function createContext () :MsoyContext
    {
        return new MsoyContext(this);
    }

    /**
     * Called to process ContextMenuEvent.MENU_SELECT.
     */
    protected function contextMenuWillPopUp (event :ContextMenuEvent) :void
    {
        var menu :ContextMenu = (event.target as ContextMenu);
        var custom :Array = menu.customItems;
        custom.length = 0;

        populateContextMenu(custom);

        // HACK: putting the separator in the menu causes the item to not
        // work in linux, so we don't do it in linux.
        var useSep :Boolean = (-1 == Capabilities.os.indexOf("Linux"));

        // add the About menu item
        custom.push(MenuUtil.createCommandContextMenuItem(
                        Msgs.GENERAL.get("b.about"),
                        MsoyController.ABOUT, null, useSep));

        // then, the menu will pop up
    }

    /**
     * Called before the context (right-click) menu is shown.
     */
    protected function populateContextMenu (custom :Array) :void
    {
    }

    /**
     * Creates the credentials that will be used to log us on.
     */
    protected function createStartupCreds (token :String) :Credentials
    {
        throw new Error("abstract");
    }

    /**
     * Returns the hostname of the game server to which we should connect, or null if that is not
     * configured in our parameters.
     */
    protected static function getServerHost () :String
    {
        return MsoyParameters.get()["host"] as String;
    }

    /**
     * Returns the ports on which we should connect to the game server, first checking the movie
     * parameters, then falling back to the default in DeploymentConfig.
     */
    protected static function getServerPorts () :Array
    {
        var params :Object = MsoyParameters.get();
        return (params["port"] != null) ? [ int(params["port"]) ] : DeploymentConfig.serverPorts;
    }

    protected var _ctx :MsoyContext;
    protected var _stage :Stage;
    protected var _loader :BodyLoader;

    protected var _minimized :Boolean;
    protected var _embedding :Embedding;
    protected var _featuredPlaceView :Boolean;
    protected var _loadedPolicies :Object = new Object();

    // configure log levels
    MsoyLogConfig.init();
}
}
