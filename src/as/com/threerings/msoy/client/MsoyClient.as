//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;
import flash.ui.ContextMenu;

import flash.events.ContextMenuEvent;
import flash.external.ExternalInterface;
import flash.system.Capabilities;
import flash.system.Security;

import flash.media.SoundMixer;
import flash.media.SoundTransform;

import mx.core.Application;

import com.threerings.util.Log;
import com.threerings.util.Name;
import com.threerings.util.ResultAdapter;
import com.threerings.util.StringUtil;
import com.threerings.util.ValueEvent;

import com.threerings.flash.MenuUtil;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;

import com.threerings.presents.dobj.DObjectManager;

import com.threerings.presents.net.Credentials;
import com.threerings.presents.net.BootstrapData;

import com.threerings.presents.data.TimeBaseMarshaller;
import com.threerings.crowd.data.BodyMarshaller;
import com.threerings.crowd.data.LocationMarshaller;
import com.threerings.crowd.chat.data.ChatMarshaller;

import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.chat.data.ChatChannel;

import com.threerings.msoy.data.LurkerName;
import com.threerings.msoy.data.MemberMarshaller;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.MsoyBootstrapData;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.SceneBookmarkEntry;

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
public /*abstract*/ class MsoyClient extends Client
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
        super(null, stage);
        setVersion(DeploymentConfig.version);
        _creds = createStartupCreds(null);

        var params :Object = stage.loaderInfo.parameters;
        _featuredPlaceView = params["featuredPlace"] != null;
        if (_featuredPlaceView) {
            // mute all sound in featured place view.
            var mute :SoundTransform = new SoundTransform();
            mute.volume = 0;
            SoundMixer.soundTransform = mute;
        }

        _ctx = createContext();
        LoggingTargets.configureLogging(_ctx);

        // wire up our JavaScript bridge functions
        try {
            if (ExternalInterface.available) {
                configureExternalFunctions();
            }
        } catch (err :Error) {
            _embedded = true;
            // nada: ExternalInterface isn't there. Oh well!
            log.info("Unable to configure external functions.");
        }

        // allow connecting the media server if it differs from the game server
        if (DeploymentConfig.mediaURL.indexOf(DeploymentConfig.serverHost) == -1) {
            Security.loadPolicyFile(DeploymentConfig.mediaURL + "crossdomain.xml");
        }

        // prior to logging on to a server, set up our security policy for that server
        addClientObserver(new ClientAdapter(clientWillLogon));

        // configure our server and port info
        setServer(getServerHost(stage), getServerPorts(stage));

        // set up a context menu that blocks funnybiz on the stage
        var menu :ContextMenu = new ContextMenu();
        menu.hideBuiltInItems();
        Application.application.contextMenu = menu;
        menu.addEventListener(ContextMenuEvent.MENU_SELECT, contextMenuWillPopUp);
    }

    public function fuckingCompiler () :void
    {
        var i :int = TimeBaseMarshaller.GET_TIME_OID;
        i = LocationMarshaller.LEAVE_PLACE;
        i = BodyMarshaller.SET_IDLE;
        i = ChatMarshaller.AWAY;

        var c :Class;
        c = MsoyBootstrapData;
        c = MsoyAuthResponseData;
        c = MemberMarshaller;
        c = SceneBookmarkEntry;
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
     * Find out whether this client is embedded in a non-whirled page.
     */
    public function isEmbedded () :Boolean
    {
        return _embedded;
    }

    /**
     * Find out whether this client is being used as a featured place view.
     */
    public function isFeaturedPlaceView () :Boolean
    {
        return _featuredPlaceView;
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
     * Notifies our JavaScript shell that the flash client should be made full size.
     */
    public function restoreClient () :void
    {
        try {
            if (ExternalInterface.available && !_embedded && !_featuredPlaceView) {
                ExternalInterface.call("restoreClient");
            }
        } catch (err :Error) {
            log.warning("ExternalInterface.call('restoreClient') failed: " + err);
        }
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
        dispatchEventToGWT("ItemUsageChanged", [ itemType, itemId, usage, loc ]);
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
            Log.getLog(this).warning("triggerFlashEvent failed: " + err);
        }
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
     * Configure any external functions that we wish to expose to JavaScript.
     */
    protected function configureExternalFunctions () :void
    {
        ExternalInterface.addCallback("onUnload", externalOnUnload);
        ExternalInterface.addCallback("setMinimized", externalSetMinimized);

        try {
            _embedded = !(ExternalInterface.call("helloWhirled") as Boolean);
        } catch (err :Error) {
            _embedded = true;
        }
        dispatchEvent(new ValueEvent(EMBEDDED_STATE_KNOWN, _embedded));
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
     * Returns the hostname of the game server to which we should connect, or null if that is not
     * configured in our parameters.
     */
    protected static function getServerHost (stage :Stage) :String
    {
        var params :Object = stage.loaderInfo.parameters;
        return (params["host"] != null) ? String(params["host"]) : null;
    }

    /**
     * Returns the ports on which we should connect to the game server, first checking the movie
     * parameters, then falling back to the default in DeploymentConfig.
     */
    protected static function getServerPorts (stage :Stage) :Array
    {
        var params :Object = stage.loaderInfo.parameters;
        return (params["port"] != null) ?
            [ int(parseInt(params["port"])) ] : DeploymentConfig.serverPorts;
    }

    protected var _ctx :MsoyContext;

    protected var _minimized :Boolean;
    protected var _embedded :Boolean;
    protected var _featuredPlaceView :Boolean;
}
}
