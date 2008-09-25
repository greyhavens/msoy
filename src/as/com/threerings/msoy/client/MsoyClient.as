//
// $Id$

package com.threerings.msoy.client {

import flash.display.Stage;
import flash.ui.ContextMenu;

import flash.events.ContextMenuEvent;
import flash.external.ExternalInterface;
import flash.system.Capabilities;
import flash.system.Security;

import flash.media.SoundMixer;
import flash.media.SoundTransform;

import com.threerings.util.Log;
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
import com.threerings.msoy.data.all.VisitorInfo;

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

    public function MsoyClient (stage :Stage)
    {
        _featuredPlaceView = UberClient.isFeaturedPlaceView();
        super(null);
        _stage = stage;
        setVersion(DeploymentConfig.version);
        
        // wire up our JavaScript bridge functions
        try {
            if (ExternalInterface.available) {
                configureExternalFunctions();
            }
        } catch (err :Error) {
            // nada: ExternalInterface isn't there. Oh well!
            log.info("Unable to configure external functions.");
        }

        _creds = createStartupCreds(null);

        if (_featuredPlaceView) {
            // mute all sound in featured place view.
            var mute :SoundTransform = new SoundTransform();
            mute.volume = 0;
            SoundMixer.soundTransform = mute;
        }

        _ctx = createContext();
        LoggingTargets.configureLogging(_ctx);

        dispatchEvent(new ValueEvent(EMBEDDED_STATE_KNOWN, _embedded));

        // allow connecting the media server if it differs from the game server
        if (DeploymentConfig.mediaURL.indexOf(DeploymentConfig.serverHost) == -1) {
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

        // ensure that the compiler includes these necessary symbols
        var c :Class;
        c = MsoyBootstrapData;
        c = MsoyAuthResponseData;
        c = LurkerName;
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
            Log.getLog(this).warning("setWindowTitle failed: " + err);
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
            log.warning("ExternalInterface.call('clearClient') failed: " + err);
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
     * Tells GWT that we were assigned a guest id.
     */
    public function gotGuestIdToGWT (guestId :int) :void
    {
        dispatchEventToGWT("gotGuestId", [ guestId ]);
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
            Log.getLog(this).warning("triggerFlashEvent failed [event=" + eventName +
                                     ", args=" + eventArgs + ", error=" + err + "].");
        }
    }

    /**
     * Get the int testGroup for this visitor and return it in the listener.
     */
    public function getABTestGroup (
        testName :String, logEvent :Boolean, listener :InvocationService_ResultListener) :void
    {
        var msvc :MemberService = requireService(MemberService) as MemberService;
        var member :MemberObject = _clobj as MemberObject;
        msvc.getABTestGroup(this, testName, logEvent, listener);
    }

    /**
     * Track a client action such as clicking a button
     */
    public function trackClientAction (actionName :String, details :String) :void
    {
        var msvc :MemberService = getService(MemberService) as MemberService;
        // Come talk to me (Ray) if you have a problem with this.
        // But this was failing if the user was still logging in, like in the featured
        // place view. So I say: fuck tracking shit if we can't track shit. I'd rather
        // not piss off the user-- in this case ME.
        if (msvc != null) {
            var member :MemberObject = _clobj as MemberObject;
            msvc.trackClientAction(this, actionName, details);
        }
    }

    /**
     * Track a test action such as clicking a button during an a/b test.  Denotes an action
     * being tracked for the duration of an a/b test only.  trackClientAction may be used instead
     * if we want to continue tracking the event after the test is over.
     */
    public function trackTestAction (actionName :String, testName :String) :void
    {
        var msvc :MemberService = requireService(MemberService) as MemberService;
        var member :MemberObject = _clobj as MemberObject;
        msvc.trackTestAction(this, actionName, testName);
    }

    /**
     * Called just before we logon to a server.
     */
    protected function clientWillLogon (event :ClientEvent) :void
    {
        var url :String = "xmlsocket://" + getHostname() + ":" + DeploymentConfig.socketPolicyPort;
        log.info("Loading security policy: " + url);
        Security.loadPolicyFile(url);
    }

    /**
     * Called just after we logon to a server.
     */
    protected function clientDidLogon (event :ClientEvent) :void
    {
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
        }

        if (_embedded && !_reportedLogon) {
            trackClientAction("embeddedLogon", null);
            _reportedLogon = true;
        }
    }

    // TEMP: quickHack. I'm not sure if switching servers makes you log in again,
    // and I can't test that now before this release...
    protected var _reportedLogon :Boolean;

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
//                        MsoyController.TOGGLE_FULLSCREEN, null, false,
//                        _wctx.getMsoyController().supportsFullScreen()));

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
                log.info("ExternalInterface.call('getVisitorId') failed", "error", e);
            }
        }
        return null;
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
        return (params["port"] != null) ?
            [ int(parseInt(params["port"])) ] : DeploymentConfig.serverPorts;
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
