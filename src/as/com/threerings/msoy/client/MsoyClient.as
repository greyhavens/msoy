//
// $Id$

package com.threerings.msoy.client {

import flash.display.Stage;
import flash.ui.ContextMenu;

import flash.events.ContextMenuEvent;
import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.external.ExternalInterface;
import flash.system.Capabilities;
import flash.system.Security;

import flash.media.SoundMixer;
import flash.media.SoundTransform;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MenuUtil;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.InvocationService_ResultListener;

import com.threerings.presents.dobj.DObjectManager;

import com.threerings.presents.net.Credentials;
import com.threerings.presents.net.BootstrapData;

import com.threerings.crowd.client.CrowdClient;

import com.threerings.msoy.client.MsoyLogConfig;

import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.UberClientModes;
import com.threerings.msoy.data.all.VisitorInfo;

import com.threerings.msoy.notify.data.Notification;

/**
 * Dispatched when the client is minimized or unminimized.
 *
 * @eventType com.threerings.msoy.client.MsoyClient.MINI_WILL_CHANGE
 */
[Event(name="miniWillChange", type="com.threerings.util.ValueEvent")]

/**
 * Dispatched when the client is known to be either embedded or not. This happens shortly after the
 * client is initialized.
 *
 * @eventType com.threerings.msoy.client.MsoyClient.EMBEDDED_STATE_KNOWN
 */
[Event(name="embeddedStateKnown", type="com.threerings.util.ValueEvent")]

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
     * An event dispatched when we learn whether or not the client is embedded.
     *
     * @eventType clientEmbedded
     */
    public static const EMBEDDED_STATE_KNOWN :String = "clientEmbedded";

    // statically reference classes we require
    MsoyBootstrapData;
    MsoyAuthResponseData;
    LurkerName;

    public function MsoyClient (stage :Stage)
    {
        _featuredPlaceView = UberClient.isFeaturedPlaceView();
        super(null);
        _stage = stage;
        setVersion(DeploymentConfig.version);

        LoggingTargets.configureLogging(_ctx);
        log.info("starting up", "capabilities", Capabilities.serverString);

        // first things first: create our credentials and context
        _creds = createStartupCreds(null);
        _ctx = createContext();

        // wire up a listener for bridge events from the embed stub
        configureBridgeFunctions(UberClient.getApplication().loaderInfo.sharedEvents);
        // then report to the embed stub that we're ready to receive bridge events
        UberClient.getApplication().loaderInfo.sharedEvents.dispatchEvent(
            new Event(UberClientModes.CLIENT_READY, true));

        // wire up our JavaScript bridge functions
        try {
            if (ExternalInterface.available) {
                configureExternalFunctions();
            }
        } catch (err :Error) {
            // nada: ExternalInterface isn't there. Oh well!
            log.info("Unable to configure external functions.");
        }

        // configure our starting global sound transform
        if (_featuredPlaceView) {
            SoundMixer.soundTransform = new SoundTransform(0); // muted
        } else {
            Prefs.setEmbedded(_embedded);
        }
        // after we've created our context, dispatch the status of whether we're embedded
        // AND: this must be done _after_ we set Prefs.setEmbedded().
        dispatchEvent(new ValueEvent(EMBEDDED_STATE_KNOWN, _embedded));

        // allow connecting the media server if it differs from the game server
        if ((Security.sandboxType != Security.LOCAL_WITH_FILE) &&
                (DeploymentConfig.mediaURL.indexOf(DeploymentConfig.serverHost) == -1)) {
            Security.loadPolicyFile(DeploymentConfig.mediaURL + "crossdomain.xml");
        }

        // prior to logging on to a server, set up our security policy for that server
        addClientObserver(
            new ClientAdapter(clientWillLogon, clientDidLogon, null, clientDidLogoff));

        // configure our server and port info
        setServer(getServerHost(), getServerPorts());

        // set up a context menu that blocks funnybiz on the stage
        var menu :ContextMenu = new ContextMenu();
        menu.hideBuiltInItems();
        UberClient.getApplication().contextMenu = menu;
        menu.addEventListener(ContextMenuEvent.MENU_SELECT, contextMenuWillPopUp);
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
    }

    /**
     * Return the Stage.
     */
    public function getStage () :Stage
    {
        return _stage;
    }

    /**
     * Find out whether this client is embedded in a non-whirled page.
     */
    public function isEmbedded () :Boolean
    {
        return _embedded;
    }

    /**
     * Requests that GWT set the window title.
     */
    public function setWindowTitle (title :String) :void
    {
        try {
            if (ExternalInterface.available && !_embedded && !_featuredPlaceView) {
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
     * Notifies our JavaScript shell that the flash client should be cleared out.
     */
    public function closeClient () :void
    {
        try {
            if (ExternalInterface.available && !_embedded && !_featuredPlaceView) {
                ExternalInterface.call("clearClient");
            }
        } catch (err :Error) {
            log.warning("ExternalInterface.call('clearClient') failed", err);
        }
    }

    /**
     * Helper to dispatch item usage changes to GWT.
     */
    public function itemUsageChangedToGWT (itemType :int, itemId :int, usage :int, loc :int) :void
    {
        dispatchEventToGWT("itemUsageChanged", [ itemType, itemId, usage, loc ]);
    }

    /**
     * Dispatches an event to GWT.
     */
    public function dispatchEventToGWT (eventName :String, eventArgs :Array) :void
    {
        try {
            if (ExternalInterface.available && !_embedded) {
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
        testName :String, logEvent :Boolean, listener :InvocationService_ResultListener) :void
    {
        var msvc :MemberService = requireService(MemberService) as MemberService;
        msvc.getABTestGroup(this, testName, logEvent, listener);
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
                log.info("ExternalInterface.call('getVisitorId') failed", e);
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
     * Track a client action such as clicking a button
     */
    public function trackClientAction (actionName :String, details :String) :void
    {
        var msvc :MemberService = getService(MemberService) as MemberService;
        if (msvc != null) {
            msvc.trackClientAction(this, actionName, details);
        } else {
            log.warning("Dropping client action", "name", actionName, "details", details);
        }
    }

    /**
     * Track a test action such as clicking a button during an a/b test.  Denotes an action
     * being tracked for the duration of an a/b test only.  trackClientAction may be used instead
     * if we want to continue tracking the event after the test is over.
     */
    public function trackTestAction (actionName :String, testName :String) :void
    {
        var msvc :MemberService = getService(MemberService) as MemberService;
        if (msvc != null) {
            msvc.trackTestAction(this, actionName, testName);
        } else {
            log.warning("Dropping test action", "name", actionName, "test", testName);
        }
    }

    /**
     * Called just before we logon to a server.
     */
    protected function clientWillLogon (event :ClientEvent) :void
    {
        var url :String = "xmlsocket://" + getHostname() + ":" + DeploymentConfig.socketPolicyPort;
        log.info("Loading security policy", "url", url);
        Security.loadPolicyFile(url);
    }

    /**
     * Called just after we logon to a server.
     */
    protected function clientDidLogon (event :ClientEvent) :void
    {
        // some things we only want to do when we're really logging on
        if (!event.getClient().isSwitchingServers()) {
            // now that we logged on, we might have gotten a different referral info back
            // from the server. so clobber whatever we have, and tell the GWT wrapper
            // to clobber its info as well.
            var member :MemberObject = _clobj as MemberObject;
            if (_featuredPlaceView || member == null) {
                return;
            }

            if (_embedded) {
                var params :Object = MsoyParameters.get();
                var vector :String = params[VisitorInfo.VECTOR_ID];
                if (vector != null && vector.length > 0) {
                    var msvc :MemberService = requireService(MemberService) as MemberService;
                    msvc.trackVectorAssociation(this, vector);
                }

            } else if (!member.isPermaguest()) {
                // for members on the web site, see if we want to tell them about release notes
                if (Prefs.setBuildTime(DeploymentConfig.buildTime)) {
                    _ctx.getNotificationDirector().addGenericNotification(
                        MessageBundle.tcompose("m.new_release", DeploymentConfig.announceGroup),
                        Notification.SYSTEM);
                }
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
        ExternalInterface.addCallback("onUnload", externalOnUnload);
        ExternalInterface.addCallback("setMinimized", externalSetMinimized);
        ExternalInterface.addCallback("isConnected", externalIsConnected);

        try {
            _embedded = !(ExternalInterface.call("helloWhirled") as Boolean);
        } catch (err :Error) {
            // we default _embedded to true now.
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
    }

    /**
     * Exposed to javascript: are we connected?
     */
    protected function externalIsConnected () :Boolean
    {
        return _ctx.getClient().isLoggedOn();
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

//        custom.push(MenuUtil.createControllerMenuItem(
//                        Msgs.GENERAL.get("b.toggle_fullscreen"),
//                        MsoyController.TOGGLE_FULLSCREEN, null, false));

        populateContextMenu(custom);

        // HACK: putting the separator in the menu causes the item to not
        // work in linux, so we don't do it in linux.
        var useSep :Boolean = (-1 == Capabilities.os.indexOf("Linux"));

        // add the About menu item
        custom.push(MenuUtil.createControllerMenuItem(
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

    protected var _minimized :Boolean;
    protected var _embedded :Boolean = true; // default to true until proven false
    protected var _featuredPlaceView :Boolean;

    // configure log levels
    MsoyLogConfig.init();
}
}
