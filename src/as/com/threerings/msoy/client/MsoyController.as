//
// $Id$

package com.threerings.msoy.client {

import flash.events.IEventDispatcher;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.events.TextEvent;
import flash.events.TimerEvent;

import flash.display.Stage;

import flash.external.ExternalInterface;
import flash.system.Capabilities;
import flash.text.TextField;
import flash.ui.Keyboard;

import flash.net.URLRequest;
import flash.utils.Timer;
import flash.utils.getTimer; // function import

import com.threerings.util.CommandEvent;
import com.threerings.util.Controller;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.NetUtil;
import com.threerings.util.StringUtil;

import com.threerings.presents.net.Credentials;

import com.threerings.crowd.chat.data.ChatCodes;
import com.threerings.crowd.client.BodyService;
import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.CrowdCodes;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.flex.ChatControl;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;

import com.threerings.crowd.chat.client.ChatCantStealFocus;

import com.threerings.msoy.game.client.MsoyGamePanel;

import mx.controls.Button;

public class MsoyController extends Controller
    implements ClientObserver
{
    /** Command to show the 'about' dialog. */
    public static const ABOUT :String = "About";

    /** Command to close the current place view. */
    public static const CLOSE_PLACE_VIEW :String = "ClosePlaceView";

    /** Command to move back to the previous location. */
    public static const MOVE_BACK :String = "MoveBack";

    /** Command to view the app in full-screen mode. */
    public static const TOGGLE_FULLSCREEN :String = "ToggleFullscreen";

    /** Command to issue to toggle the chat display. */
    public static const TOGGLE_CHAT :String = "ToggleChat";

    /** Command to log us on. */
    public static const LOGON :String = "Logon";

    /** Command to edit preferences. */
    public static const CHAT_PREFS :String = "ChatPrefs";

    /** Command to display a volume slider. */
    public static const POP_VOLUME :String = "PopVolume";

    /** Command to display the notificaitons popup. */
    public static const POPUP_NOTIFICATIONS :String = "PopupNotifications";

    /** Command to display the chat channel menu. */
    public static const POP_CHANNEL_MENU :String = "PopChannelMenu";

    /** Opens up a new toolbar and a new room editor. */
    public static const ROOM_EDIT :String = "RoomEdit";

    /** Takes a room snapshot. */
    public static const SNAPSHOT :String = "Snapshot";

    /** Command to view a game, arg is [ gameId ] */
    public static const VIEW_GAME :String = "ViewGame";

    /** Command to view an item, arg is [ itemTypeId, itemId ] */
    public static const VIEW_ITEM :String = "ViewItem";

    /** Command to go to a running game (gameId + placeOid). */
    public static const GO_GAME :String = "GoGame";

    /**
     * Creates and initializes the controller.
     */
    public function MsoyController (ctx :MsoyContext, topPanel :TopPanel)
    {
        _ctx = ctx;
        _ctx.getClient().addServiceGroup(CrowdCodes.CROWD_GROUP);
        _ctx.getClient().addClientObserver(this);
        _topPanel = topPanel;

        var stage :Stage = ctx.getStage();
        setControlledPanel(stage);
        stage.addEventListener(FocusEvent.FOCUS_OUT, handleUnfocus);

        _idleTimer = new Timer(ChatCodes.DEFAULT_IDLE_TIME, 1);
        _idleTimer.addEventListener(TimerEvent.TIMER, function (... ignored) :void {
            setIdle(true)
        });
        restartIdleTimer();

        // listen for location changes
        _ctx.getLocationDirector().addLocationObserver(
            new LocationAdapter(null, this.locationChanged, null));
    }

    /**
     * @return true if this player appears to support full-screen mode.
     */
    public function supportsFullScreen () :Boolean
    {
        // TODO: this too could be cleaned up. See note in handleToggleFullscreen
        var o :Object = _ctx.getStage();
        try {
            return (undefined !== o.displayState);
        } catch (e :Error) {
        }
        return false;
    }

    /**
     * Convenience method for opening an external window and showing the specified url. This is
     * done when we want to show the user something without unloading the msoy world.
     */
    public function showExternalURL (url :String) :void
    {
        if (!NetUtil.navigateToURL(url, false)) {
            _ctx.displayFeedback(null, MessageBundle.tcompose("e.no_navigate", url));
        }
    }

    /**
     * Force the user to be idle immediately, and give them the specified number of seconds to move
     * the mouse and such before we automatically de-idle them.
     */
    public function forceIdle (secondsOfLeeway :int) :void
    {
        _idleOverrideStamp = (secondsOfLeeway * 1000) + getTimer();
        setIdle(true);
    }

    /**
     * Returns a string that can be used to embed the current flash client.
     */
    public function getSceneIdString () :String
    {
        return "";
    }

    /**
     * Handles the ABOUT command.
     */
    public function handleAbout () :void
    {
        new AboutDialog(_ctx);
    }

    /**
     * Handles the CLOSE_PLACE_VIEW command.
     */
    public function handleClosePlaceView () : void
    {
        // handled by our derived classes
    }

    /**
     * Handles the MOVE_BACK command.
     */
    public function handleMoveBack () :void
    {
        // handled by our derived classes
    }

    /**
     * Handles the TOGGLE_FULLSCREEN command.
     */
    public function handleToggleFullscreen () :void
    {
        // TODO: once things are more up to date, we can use the real
        // class and StageDisplayState for the constants
        var o :Object = _ctx.getStage();
        o.displayState = (o.displayState == "normal") ? "fullScreen" : "normal";
    }

    /**
     * Handles the TOGGLE_CHAT command.
     */
    public function handleToggleChat () :void
    {
        Prefs.setShowingChatHistory(!Prefs.getShowingChatHistory());
    }

    /**
     * Handles the LOGON command.
     */
    public function handleLogon (creds :Credentials) :void
    {
        // give the client a chance to log off, then log back on
        _topPanel.callLater(function () :void {
            var client :Client = _ctx.getClient();
            log.info("Logging on [creds=" + creds + ", version=" + DeploymentConfig.version + "].");
            client.setCredentials(creds);
            client.logon();
        });
    }

    /**
     * Handles CHAT_PREFS.
     */
    public function handleChatPrefs () :void
    {
        new ChatPrefsDialog(_ctx);
    }
    
    /**
     * Handle the POP_VOLUME command.
     */
    public function handlePopVolume (trigger :Button) :void
    {
        if (VolumePopup.popupExists()) {
            VolumePopup.destroyCurrentInstance();
        } else {
            var popup :VolumePopup = new VolumePopup(trigger);
            popup.show();
        }
    }

    /**
     * Figure out where we should be going, and go there.
     */
    public function goToPlace (params :Object) :void
    {
        // handled by our derived classes
    }

    // from ClientObserver
    public function clientWillLogon (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidLogon (event :ClientEvent) :void
    {
      // nada
    }

    // from ClientObserver
    public function clientObjectDidChange (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidLogoff (event :ClientEvent) :void
    {
        _topPanel.clearLeftPanel(null);
        _topPanel.clearBottomPanel(null);
        _topPanel.setPlaceView(new DisconnectedPanel(_ctx, _logoffMessage));
        _logoffMessage = null;
    }

    // from ClientObserver
    public function clientFailedToLogon (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientConnectionFailed (event :ClientEvent) :void
    {
        _logoffMessage = Msgs.GENERAL.get("m.lost_connection");
    }

    // from ClientObserver
    public function clientWillLogoff (event :ClientEvent) :void
    {
        // nada
    }

    // from ClientObserver
    public function clientDidClear (event :ClientEvent) :void
    {
        // nada
    }

    /**
     * Return true if we are running in the GWT application shell, false otherwise.
     */
    protected function inGWTApp () :Boolean
    {
        var pt :String = Capabilities.playerType;
        if (pt == "StandAlone" || pt == "External") {
            return false;
        }
        if (_ctx.getMsoyClient().isEmbedded()) {
            return false;
        }
        if (_ctx.getPartner() == "facebook") {
            return false;
        }
        return true;
    }

    override protected function setControlledPanel (panel :IEventDispatcher) :void
    {
        // in addition to listening for command events, let's listen
        // for LINK events and handle them all here.
        if (_controlledPanel != null) {
            _controlledPanel.removeEventListener(TextEvent.LINK, handleLink);
            _controlledPanel.removeEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown, true);
            _controlledPanel.removeEventListener(MouseEvent.MOUSE_MOVE, handleMouseMove);
        }
        super.setControlledPanel(panel);
        if (_controlledPanel != null) {
            _controlledPanel.addEventListener(TextEvent.LINK, handleLink);
            _controlledPanel.addEventListener(KeyboardEvent.KEY_DOWN, handleKeyDown, true);
            _controlledPanel.addEventListener(MouseEvent.MOUSE_MOVE, handleMouseMove);
        }
    }

    /**
     * Handles a TextEvent.LINK event.
     */
    protected function handleLink (evt :TextEvent) :void
    {
        var url :String = evt.text;
        if (StringUtil.startsWith(url, COMMAND_URL)) {
            var cmd :String = url.substring(COMMAND_URL.length);
            var arg :String = null;
            var slash :int = cmd.indexOf("/");
            if (slash != -1) {
                arg = cmd.substring(slash + 1);
                cmd = cmd.substring(0, slash);
            }
            if (arg == null || arg.indexOf("/") == -1) {
                CommandEvent.dispatch(evt.target as IEventDispatcher, cmd, arg);
            } else {
                CommandEvent.dispatch(evt.target as IEventDispatcher, cmd, arg.split(/\//));
            }

        } else {
            // A regular URL
            showExternalURL(url);
        }
    }

    /**
     * Handles global key events.
     */
    protected function handleKeyDown (event :KeyboardEvent) :void
    {
        restartIdleTimer();

        switch (event.keyCode) {
        // TODO: not F7
        case Keyboard.F7:
            handleToggleChat();
            break;
        case Keyboard.LEFT:
            if (event.ctrlKey) {
                _ctx.getTopPanel().getHeaderBar().getChatTabs().selectedIndex--;
            }
            break;
        case Keyboard.RIGHT:
            if (event.ctrlKey) {
                _ctx.getTopPanel().getHeaderBar().getChatTabs().selectedIndex++;
            }
        }

        // We check every keyboard event, see if it's a "word" character,
        // and then if it's not going somewhere reasonable, route it to chat.
        var c :int = event.charCode;
        if (c != 0 && !event.ctrlKey && !event.altKey &&
                // these are the ascii values for '/', a -> z,  A -> Z
                (c == 47 || (c >= 97 && c <= 122) || (c >= 65 && c <= 90))) {
            checkChatFocus();
        }
    }

    /**
     * Handles mouse movement on the stage.
     */
    protected function handleMouseMove (event :MouseEvent) :void
    {
        restartIdleTimer();
    }

    /**
     * Called when our location changes.
     */
    protected function locationChanged (place :PlaceObject) :void
    {
        updateTopPanel(_ctx.getTopPanel().getHeaderBar(), _ctx.getTopPanel().getControlBar());
    }

    /**
     * Called when our location changes so that we can update our top panel bits.
     */
    protected function updateTopPanel (headerBar :HeaderBar, controlBar :ControlBar) :void
    {
        // handled by our derived classes
    }

    /**
     * Called when we've detected user activity, like mouse movement or key presses.
     */
    protected function restartIdleTimer () :void
    {
        // see if we want to honor this request..
        if (_idleOverrideStamp != 0) {
            if (getTimer() < _idleOverrideStamp) {
                return;
            } else {
                _idleOverrideStamp = 0;
            }
        }

        setIdle(false);
        _idleTimer.reset();
        _idleTimer.start();
    }

    /**
     * Update our idle status.
     */
    protected function setIdle (nowIdle :Boolean) :void
    {
        if (nowIdle != _idle) {
            _idle = nowIdle;
            var bsvc :BodyService = _ctx.getClient().requireService(BodyService) as BodyService;
            bsvc.setIdle(_ctx.getClient(), nowIdle);
        }
    }

    /**
     * Detect the kind of unfocus that happens when the user switches tabs.
     */
    protected function handleUnfocus (event :FocusEvent) :void
    {
        if (event.target is TextField && event.relatedObject == null) {
            _ctx.getStage().addEventListener(MouseEvent.MOUSE_MOVE, handleRefocus);
        }
    }

    /**
     * Attempt to refocus the chatbox after the browser caused focus to lose.
     */
    protected function handleRefocus (event :MouseEvent) :void
    {
        _ctx.getStage().removeEventListener(MouseEvent.MOUSE_MOVE, handleRefocus);
        checkChatFocus();
    }

    /**
     * Try to assign focus to the chat entry field if it seems like we should.
     */
    protected function checkChatFocus (... ignored) :void
    {
        try {
            var focus :Object = _ctx.getStage().focus;
            if (!(focus is TextField) && !(focus is ChatCantStealFocus)) {
                ChatControl.grabFocus();
            }
        } catch (err :Error) {
            // TODO: leave this in for now
            trace(err.getStackTrace());
        }
    }

    /** Provides access to client-side directors and services. */
    protected var _ctx :MsoyContext;

    /** The topmost panel in the msoy client. */
    protected var _topPanel :TopPanel;

    /** A special logoff message to use when we disconnect. */
    protected var _logoffMessage :String;

    /** Whether we think we're idle or not. */
    protected var _idle :Boolean = false;

    /** A timer to watch our idleness. */
    protected var _idleTimer :Timer;

    /** A timestamp (from flash.utils.getTimer()) before which we ignore non-idling behavior. */
    protected var _idleOverrideStamp :Number = 0;

    /** The URL prefix for 'command' URLs, that post CommendEvents. */
    protected static const COMMAND_URL :String = "command://";

    private static const log :Log = Log.getLog(MsoyController);
}
}
