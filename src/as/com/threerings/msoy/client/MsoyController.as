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
import flash.geom.Rectangle;
import flash.system.Capabilities;
import flash.text.TextField;
import flash.ui.Keyboard;
import flash.utils.Timer;

import mx.controls.Button;
import mx.events.MenuEvent;

import com.threerings.util.ArrayUtil;
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
import com.threerings.flex.CommandMenu;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;

import com.threerings.crowd.chat.client.ChatCantStealFocus;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.ui.SliderPopup;

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
    public static const TOGGLE_CHAT_HIDE :String = "ToggleChatHide";

    /** Command to issue to toggle the chat being in a sidebar. */
    public static const TOGGLE_CHAT_SIDEBAR :String = "ToggleChatSidebar";

    /** Command to toggle the channel occupant list display */
    public static const TOGGLE_OCC_LIST :String = "ToggleOccList";

    /** Command to log us on. */
    public static const LOGON :String = "Logon";

    /** Command to edit preferences. */
    public static const CHAT_PREFS :String = "ChatPrefs";

    /** Command to display the go menu. */
    public static const POP_GO_MENU :String = "PopGoMenu";

    /** Command to display sign-up info for guests. */
    public static const SHOW_SIGN_UP :String = "ShowSignUp";

    /** Command to show an (external) URL. */
    public static const VIEW_URL :String = "ViewUrl";

    /** Command to view an item, arg is an ItemIdent. */
    public static const VIEW_ITEM :String = "ViewItem";

    /** Command to view all games */
    public static const VIEW_GAMES :String = "ViewGames";

    /** Command to display the full Whirled (used in the embedded client). */
    public static const VIEW_FULL_VERSION :String = "ViewFullVersion";

    /** Command to display the comment page for the current scene or game. */
    public static const VIEW_COMMENT_PAGE :String = "ViewCommentPage";

    /** Command to display the comment page for the current game, if any. */
    public static const VIEW_GAME_COMMENT_PAGE :String = "ViewGameCommentPage";

    /** Command to display game instructions. */
    public static const VIEW_GAME_INSTRUCTIONS :String = "ViewGameInstructions";

    /** Command to display the comment page for the current scene or game. */
    public static const VIEW_GAME_TROPHIES :String = "ViewGameTrophies";

    /** Command to display a game shop page.
     * args: [ gameId, optional: itemType, optional: catalogId ] */
    public static const VIEW_GAME_SHOP :String = "ViewGameShop";

    /** Command to go to a running game (gameId + placeOid). */
    public static const GO_GAME :String = "GoGame";

    /** Command to view a groups's page, arg is [ groupId ] */
    public static const VIEW_GROUP :String = "ViewGroup";

    /** Command to go to a group's home scene. */
    public static const GO_GROUP_HOME :String = "GoGroupHome";

    // NOTE:
    // Any commands defined in this class should be handled in this class.
    // Currently, this is not the case. Some commands are here without even an abstract or
    // empty method to handle them. Traditionally this would be bad, but since we currently
    // only have one subclass implementation, we're just going to take a shortcut and say
    // that any command defined but not handled here is "abstract". The command dispatch
    // system will log an error whenever a command is unhandled, so it should not be too hard
    // for someone to track down the issue. However, once we have subclasses other than
    // WorldController, we will want to ensure that only commands that are global are here
    // and probably also that we at least define an abstract (as best we can in actionscript)
    // method to handle the command, so that it's easier for people to see what they need
    // to implement in the subclasses.
    //

    /**
     * Creates and initializes the controller.
     */
    public function MsoyController (mctx :MsoyContext, topPanel :TopPanel)
    {
        _mctx = mctx;
        _mctx.getClient().addServiceGroup(CrowdCodes.CROWD_GROUP);
        _mctx.getClient().addClientObserver(this);
        _topPanel = topPanel;

        setControlledPanel(topPanel);
        var stage :Stage = mctx.getStage();
        stage.addEventListener(FocusEvent.FOCUS_OUT, handleUnfocus);

        _idleTimer = new Timer(ChatCodes.DEFAULT_IDLE_TIME, 1);
        _idleTimer.addEventListener(TimerEvent.TIMER, function (... ignored) :void {
            setIdle(true)
        });
        restartIdleTimer();

        // listen for location changes
        _mctx.getLocationDirector().addLocationObserver(
            new LocationAdapter(null, this.locationDidChange, null));
    }

    /**
     * @return true if this player appears to support full-screen mode.
     */
    public function supportsFullScreen () :Boolean
    {
        // TODO: this too could be cleaned up. See note in handleToggleFullscreen
        var o :Object = _mctx.getStage();
        try {
            return (undefined !== o.displayState);
        } catch (e :Error) {
        }
        return false;
    }

    /**
     * Convenience method for opening an external window and showing the specified url. This is
     * done when we want to show the user something without unloading the msoy world.
     *
     * Also, handles VIEW_URL.
     *
     * @return true on success
     */
    public function handleViewUrl (url :String, top :Boolean = false) :Boolean
    {
        if (NetUtil.navigateToURL(url, top ? "_top" : null)) {
            return true;

        } else {
            _mctx.displayFeedback(
                MsoyCodes.GENERAL_MSGS, MessageBundle.tcompose("e.no_navigate", url));

            // TODO
            // experimental: display a popup with the URL (this could be moved to handleLink()
            // if this method is altered to return a success Boolean
            new MissedURLDialog(_mctx, url);
            return false;
        }
    }

    /**
     * Create a link to a page at www.whirled.com, either for us or to share, with the
     * appropriate affiliate information tacked-on, if necessary.
     */
    public function createPageLink (page :String, forUs :Boolean) :String
    {
        const memName :MemberName = _mctx.getMyName();
        // TEMP logging
        log.info("createPageLink", "memName", memName, "params", MsoyParameters.get());
        const isGuest :Boolean = memName.isGuest();
        var affiliate :String = null;
        if (forUs && isGuest) {
            affiliate = MsoyParameters.get()["aff"] as String;

        } else if (!forUs && !isGuest) {
            affiliate = String(memName.getMemberId());
        }
        var url :String = DeploymentConfig.serverURL;
        url = url.replace(/(http:\/\/[^\/]*).*/, "$1/"); // TODO: remove?
        if (StringUtil.isBlank(affiliate)) {
            return url + "#" + page; // just a straight page visit

        } else {
            // TODO: uri encoding needed?
            return url + "welcome/" + StringUtil.trim(affiliate) + "/" + page;
        }
    }

    /**
     * Returns an array containing [ isGame, name, id ] for either our current room or our current
     * game.
     */
    public function getPlaceInfo () :Array
    {
        return [ false, null, 0 ];
    }

    /**
     * Can we "manage" the current place.
     */
    public function canManagePlace () :Boolean
    {
        return false;
    }

    /**
     * Add a function for populating the "go" menu. Adds the returned menu items.
     * signature: function () :Array;
     */
    public function addGoMenuProvider (fn :Function) :void
    {
        _goMenuProviders.push(fn);
    }

    /**
     * Remove a previously-registered "go" menu provider function.
     */
    public function removeGoMenuProvider (fn :Function) :void
    {
        ArrayUtil.removeAll(_goMenuProviders, fn);
    }

    /**
     * Update our away status.
     */
    public function setAway (nowAway :Boolean, message :String = null) :void
    {
        if (nowAway != _away) {
            _away = nowAway;
            if (nowAway && StringUtil.isBlank(message)) {
                // use a default message
                message = Msgs.GENERAL.get("m.awayDefault");
            }
            var msvc :MemberService =
                _mctx.getClient().requireService(MemberService) as MemberService;
            msvc.setAway(_mctx.getClient(), nowAway, message);

            // either way, we're not idle anymore
            _idle = false;

            // Disabled -- Bruno
            /*if (nowAway) {
                // throw up a modal dialog with a "back" button
                new BackFromAwayDialog(_mctx, message);
            }*/
        }
    }

    /**
     * Handles the POP_GO_MENU command.
     */
    public function handlePopGoMenu (trigger :Button) :void
    {
        if (_goMenu != null) {
            _goMenu.hide(); // and then will be nulled automatically
            return;
        }

        var menuData :Array = [];
        // add standard items
        populateGoMenu(menuData);

        // then, populate the menu with custom stuff from providers
        for each (var fn :Function in _goMenuProviders) {
            var array :Array = fn();
            if (array != null && array.length > 0) {
                menuData.push({ type: "separator" });
                menuData.push.apply(null, array);
            }
        }

        var r :Rectangle = trigger.getBounds(trigger.stage);
        _goMenu = CommandMenu.createMenu(menuData, _topPanel);
        _goMenu.variableRowHeight = true;
        _goMenu.popUpAt(r.left, r.bottom);
        _goMenu.addEventListener(MenuEvent.MENU_HIDE, function (... ignored) :void {
            _goMenu = null;
        });
    }

    /**
     * Handles the ABOUT command.
     */
    public function handleAbout () :void
    {
        new AboutDialog(_mctx);
    }

    /**
     * Handles the CLOSE_PLACE_VIEW command.
     */
    public function handleClosePlaceView () : void
    {
        // handled by our derived classes
    }

    /**
     * Handles the VIEW_GAMES command.
     */
    public function handleViewGames () :void
    {
        // handled by our derived classes
    }

    /**
     * Handles the MOVE_BACK command.
     */
    public function handleMoveBack (closeInsteadOfHome :Boolean = false) :void
    {
        // handled by our derived classes
    }

    /**
     * Can we move back?
     */
    public function canMoveBack () :Boolean
    {
        return false;
    }

    /**
     * Handles the TOGGLE_FULLSCREEN command.
     */
    public function handleToggleFullscreen () :void
    {
        // TODO: once things are more up to date, we can use the real
        // class and StageDisplayState for the constants
        var o :Object = _mctx.getStage();
        o.displayState = (o.displayState == "normal") ? "fullScreen" : "normal";
    }

    /**
     * Handles the TOGGLE_CHAT_HIDE command.
     */
    public function handleToggleChatHide () :void
    {
        Prefs.setShowingChatHistory(!Prefs.getShowingChatHistory());
    }

    /**
     * Handles the TOGGLE_CHAT_SIDEBAR command.
     */
    public function handleToggleChatSidebar () :void
    {
        Prefs.setSidebarChat(!Prefs.getSidebarChat());
    }

    /**
     * Handles the TOGGLE_OCC_LIST command.
     */
    public function handleToggleOccList () :void
    {
        Prefs.setShowingOccupantList(!Prefs.getShowingOccupantList());
    }

    /**
     * Handles the LOGON command.
     */
    public function handleLogon (creds :Credentials) :void
    {
        // give the client a chance to log off, then log back on
        _topPanel.callLater(function () :void {
            var client :Client = _mctx.getClient();
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
        new ChatPrefsDialog(_mctx);
    }

    /**
     * Updates our header and control bars based on our current location. This is called
     * automatically when the world location changes but must be called explicitly by the game
     * services when we enter a game as the world services don't know about that.
     */
    public function updateLocationDisplay () :void
    {
        updateTopPanel(_mctx.getTopPanel().getHeaderBar(), _mctx.getControlBar());

        if (_goMenu != null) {
            _goMenu.hide();
            // will be nulled automatically...
        }
    }

    /**
     * Requests that standard menu items be added to the supplied menu which is being popped up as
     * a result of clicking on another player (their name, or their avatar) somewhere in Whirled.
     */
    public function addMemberMenuItems (
        member :MemberName, menuItems :Array,
        addPlaceItems :Boolean = false, addAvatarItems :Boolean = false) :void
    {
        // nothing by default
    }

    /**
     * Requests that standard menu items be added to the supplied menu which is being popped up as
     * a result of clicking on a friend somewhere in Whirled.
     */
    public function addFriendMenuItems (member :MemberName, menuItems :Array) :void
    {
        // nothing by default
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
        _topPanel.setPlaceView(new DisconnectedPanel(_mctx, _logoffMessage));
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
        _logoffMessage = "m.lost_connection";
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
        if (_mctx.getMsoyClient().isEmbedded()) {
            return false;
        }
        if (_mctx.getPartner() == "facebook") {
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
            var argStr :String = null;
            var slash :int = cmd.indexOf("/");
            if (slash != -1) {
                argStr = cmd.substring(slash + 1);
                cmd = cmd.substring(0, slash);
            }
            var arg :Object = (argStr == null || argStr.indexOf("/") == -1)
                ? argStr : argStr.split(/\//);
            CommandEvent.dispatch(evt.target as IEventDispatcher, cmd, arg);

        } else {
            // A regular URL
            handleViewUrl(url);
        }
    }

    /**
     * Handles global key events.
     */
    protected function handleKeyDown (event :KeyboardEvent) :void
    {
        restartIdleTimer();

        switch (event.keyCode) {

        case Keyboard.F7:
            _mctx.getTopPanel().getHeaderBar().getChatTabs().selectedIndex--;
            break;
        case Keyboard.F8:
            _mctx.getTopPanel().getHeaderBar().getChatTabs().selectedIndex++;
            break;
        case Keyboard.F9:
            handleToggleChatHide();
            break;
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
     * Handles mouse moves on the stage.
     */
    protected function handleMouseMove (event :MouseEvent) :void
    {
        restartIdleTimer();
    }

    /**
     * Called when our location changes.
     */
    protected function locationDidChange (place :PlaceObject) :void
    {
        if (UberClient.isRegularClient()) {
            updateLocationDisplay();
        }
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
        setIdle(false);
        _idleTimer.reset();
        _idleTimer.start();
    }

    /**
     * Update our idle status.
     */
    protected function setIdle (nowIdle :Boolean) :void
    {
        // we can only update our idle status if we're not away.
        if (!_away && nowIdle != _idle) {
            _idle = nowIdle;
            var bsvc :BodyService = _mctx.getClient().getService(BodyService) as BodyService;
            // the service may be null if we're in the studio viewer, so just don't worry about it
            if (bsvc != null) {
                bsvc.setIdle(_mctx.getClient(), nowIdle);
            }
        }
    }

    /**
     * Detect the kind of unfocus that happens when the user switches tabs.
     */
    protected function handleUnfocus (event :FocusEvent) :void
    {
        if (event.target is TextField && event.relatedObject == null) {
            _mctx.getStage().addEventListener(MouseEvent.MOUSE_MOVE, handleRefocus);
        }
    }

    /**
     * Attempt to refocus the chatbox after the browser caused focus to lose.
     */
    protected function handleRefocus (event :MouseEvent) :void
    {
        _mctx.getStage().removeEventListener(MouseEvent.MOUSE_MOVE, handleRefocus);
        checkChatFocus();
    }

    /**
     * Try to assign focus to the chat entry field if it seems like we should.
     */
    protected function checkChatFocus (... ignored) :void
    {
        try {
            var focus :Object = _mctx.getStage().focus;
            if (!(focus is TextField) && !(focus is ChatCantStealFocus)) {
                ChatControl.grabFocus();
            }
        } catch (err :Error) {
            // TODO: leave this in for now
            trace(err.getStackTrace());
        }
    }

    /**
     * Populate the go menu.
     */
    protected function populateGoMenu (menuData :Array) :void
    {
        // always put "back" first
        menuData.push({ label: Msgs.GENERAL.get("b.back"), callback: handleMoveBack,
            enabled: canMoveBack() });
    }

    /** Provides access to client-side directors and services. */
    protected var _mctx :MsoyContext;

    /** The topmost panel in the msoy client. */
    protected var _topPanel :TopPanel;

    /** A special logoff message to use when we disconnect. */
    protected var _logoffMessage :String;

    /** Whether we think we're idle or not. */
    protected var _idle :Boolean = false;

    /** Whether we think we're away from the keyboard or not. */
    protected var _away :Boolean = false;

    /** A timer to watch our idleness. */
    protected var _idleTimer :Timer;

    /** The "go" menu, while it's up. */
    protected var _goMenu :CommandMenu;

    /** Functions that can be called to populate the "go" menu. */
    protected var _goMenuProviders :Array = [];

    /** The URL prefix for 'command' URLs, that post CommendEvents. */
    protected static const COMMAND_URL :String = "command://";

    private static const log :Log = Log.getLog(MsoyController);
}
}
