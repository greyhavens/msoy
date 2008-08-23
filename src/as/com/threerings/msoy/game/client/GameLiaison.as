//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObjectContainer;
import flash.display.SimpleButton;
import flash.display.Sprite;

import flash.events.Event;
import flash.events.MouseEvent;
import flash.events.TimerEvent;

import flash.net.URLLoader;
import flash.net.URLRequest;

import flash.system.Security;
import flash.text.TextField;
import flash.utils.Timer;

import caurina.transitions.Tweener;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;

import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;

import com.threerings.util.Log;
import com.threerings.util.MultiLoader;

import com.whirled.game.data.WhirledGameObject;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.MsoyGameCredentials;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * Handles all the fiddly bits relating to connecting to a separate server to match-make and
 * play a game. This class is subclassed by LobbyGameLiaison, which handles lobbied games
 * that take over the view, and AVRGameLiaison, which handles in-world games.
 */
public class GameLiaison
    implements MsoyGameService_LocationListener, ClientObserver, MessageListener
{
    public static const log :Log = Log.getLog(GameLiaison);

    public function GameLiaison (wctx :WorldContext, gameId :int)
    {
        _wctx = wctx;
        _gameId = gameId;

        // create our custom context which we'll use to connect to lobby/game servers
        _gctx = new LiaisonGameContext(wctx);
        _gctx.getClient().addClientObserver(this);
    }

    public function getGameContext () :GameContext
    {
        return _gctx;
    }

    /**
     * Starts this game liaison. If the game host and port are supplied, they will be used
     * immediately, otherwise the liaison will first ask its world server to locate the game in
     * question.
     */
    public function start (ghost :String = null, gport :int = 0) :void
    {
        if (ghost != null && gport != 0) {
            gameLocated(ghost, gport);

            // we may be doing a pre-logon go but were passed all the necessary information in our
            // Flash parameters, in which case we need to let the world client know that it's cool
            // to log in now
            if (!_wctx.getClient().isLoggedOn()) {
                _wctx.getClient().logon();
            }

        } else if (_wctx.getClient().isLoggedOn()) {
            log.info("Resolving location of game [id=" + _gameId + "].");
            var mgsvc :MsoyGameService =
                (_wctx.getClient().requireService(MsoyGameService) as MsoyGameService);
            mgsvc.locateGame(_wctx.getClient(), gameId, this);

        } else {
            log.info("Resolving location of game via HTTP [id=" + _gameId + "].");
            var loader :URLLoader = new URLLoader();
            loader.addEventListener(Event.COMPLETE, function () :void {
                loader.removeEventListener(Event.COMPLETE, arguments.callee);
                var bits :Array = (loader.data as String).split(":");
                var guestId :int = int(bits[4]);
                var guestToken :String = MsoyCredentials.GUEST_SESSION_PREFIX + guestId;
                var guestName :MemberName = new MemberName("Guest" + (-guestId), guestId);

                // start connecting to the game server first
                var gcreds :MsoyGameCredentials  =
                    (_gctx.getClient().getCredentials() as MsoyGameCredentials);
                gcreds.sessionToken = MsoyCredentials.GUEST_SESSION_PREFIX + guestId;
                if (gcreds.getUsername() == null) {
                    gcreds.setUsername(new MemberName("Guest" + (-guestId), guestId));
                }
                gameLocated(bits[0], int(bits[1]));

                log.info("Got group world server info, logging on [server=" + bits[2] +
                    ", port=" + bits[3] + "].");

                // now configure the world client to connect to the server that hosts our group
                // room so that when we get into the lobby, we'll be ready to roll
                _wctx.getClient().setServer(bits[2], [ int(bits[3]) ]);
                var wcreds :MsoyCredentials =
                    (_wctx.getClient().getCredentials() as MsoyCredentials);
                wcreds.sessionToken = guestToken;
                if (wcreds.getUsername() == null) {
                    wcreds.setUsername(guestName);
                }
                _wctx.getClient().logon();
            });
            // TODO: add listeners for failure events? give feedback on failure?
            loader.load(new URLRequest(DeploymentConfig.serverURL + "embed/g" + _gameId));
        }
    }

    /**
     * Returns the ID of the game we're match-making for.
     */
    public function get gameId () :int
    {
        return _gameId;
    }

    /**
     * Shuts down this liaison, disconnecting from the game server if we have not already.
     */
    public function shutdown () :void
    {
        _gctx.getClient().logoff(false);
    }

    // from interface MsoyGameService_LocationListener
    public function gameLocated (hostname :String, port :int) :void
    {
        log.info("Got server for " + _gameId + " [host=" + hostname + ", port=" + port + "].");
        _gctx.getClient().setServer(hostname, [ port ]);
        _gctx.getClient().setVersion(DeploymentConfig.version);
        _gctx.getClient().logon();
    }

    // from interface MsoyGameService_LocationListener
    public function requestFailed (cause :String) :void
    {
        _wctx.displayFeedback(MsoyCodes.GAME_MSGS, cause);
    }

    // from interface ClientObserver
    public function clientWillLogon (event :ClientEvent) :void
    {
        var url :String = "xmlsocket://" + _gctx.getClient().getHostname() + ":" +
            DeploymentConfig.socketPolicyPort;
        log.info("Loading security policy: " + url);
        Security.loadPolicyFile(url);
    }

    // from interface ClientObserver
    public function clientDidLogon (event :ClientEvent) :void
    {
        // listen for message events on our player object
        _gctx.getPlayerObject().addListener(this);
    }

    // from interface ClientObserver
    public function clientFailedToLogon (event :ClientEvent) :void
    {
        // TODO: something fancier?
        _wctx.displayFeedback(MsoyCodes.GAME_MSGS, "e.internal_error");
        clientDidClear(null); // abandon ship
    }

    // from interface ClientObserver
    public function clientObjectDidChange (event :ClientEvent) :void
    {
        // nada
    }

    // from interface ClientObserver
    public function clientConnectionFailed (event :ClientEvent) :void
    {
        // TODO: report an error message to the user?
        log.info("Lost connection to game server [cause=" + event.getCause() + "].");
        // we'll get a didLogoff in a second where the GameDirector will shut us down
    }

    // from interface ClientObserver
    public function clientWillLogoff (event :ClientEvent) :void
    {
        // nada
    }

    // from interface ClientObserver
    public function clientDidLogoff (event :ClientEvent) :void
    {
        log.info("Logged off of game server [id=" + _gameId + "].");
        var player :PlayerObject = _gctx.getPlayerObject();
        if (player) {
            player.removeListener(this);
        }
    }

    // from interface ClientObserver
    public function clientDidClear (event :ClientEvent) :void
    {
        // tell the game director that we're audi
        _wctx.getGameDirector().liaisonCleared(this);
    }

    // from interface MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        const name :String = event.getName();
        const args :Array = event.getArgs();
        if (name == MsoyGameCodes.TROPHY_AWARDED || name == MsoyGameCodes.PRIZE_AWARDED) {
            _wctx.displayAward(args[0]);

        } else if (name == WhirledGameObject.COINS_AWARDED_MESSAGE) {
            const coins :int = int(args[0]);
            const forReal :Boolean = Boolean(args[2]);
            const hasCookie :Boolean = Boolean(args[3]);
            const gamePanel :MsoyGamePanel = getMsoyGamePanel();
            if (forReal && _gctx.getPlayerObject().isGuest() &&
                    (!MsoyGamePanel.USE_GAMEOVER_POPUP || gamePanel.getController().isParty())) {
                // if a guest earns flow, we want to show them the "please register" dialog
                displayGuestFlowEarnage(coins, hasCookie);
            }
            // and always pass the buck to the panel, even if it doesn't care..
            gamePanel.displayGameOverCoinAward(forReal, coins, hasCookie);
        }
    }

    protected function getMsoyGamePanel () :MsoyGamePanel
    {
        return (_wctx.getTopPanel().getPlaceView() as MsoyGamePanel);
    }

    /**
     * Display a slide-down panel to guests in situations in which they otherwise
     * wouldn't see the standard GameOverPanel.
     */
    protected function displayGuestFlowEarnage (amount :int, hasCookie :Boolean) :void
    {
        if (_guestFlowPanel == null) {
            _guestFlowPanel = LOADING;
            _flowPanelAutoDismiss = new Timer(15000, 1);
            _flowPanelAutoDismiss.addEventListener(TimerEvent.TIMER, clearGuestFlow);
            MultiLoader.getContents(
                GUEST_FLOW_PANEL, function (result :DisplayObjectContainer) :void {
                _guestFlowPanel = result;
                displayGuestFlowEarnage(amount, hasCookie);
            });

        } else if (_guestFlowPanel == LOADING || _guestFlowPanel.stage != null) {
            return; // we're loading it or already showing it

        } else {
            var field :TextField = (_guestFlowPanel.getChildByName("youearned") as TextField);
            field.text = Msgs.GAME.get("l.guest_flow_title", ""+amount);
            field = (_guestFlowPanel.getChildByName("ifyousign") as TextField);
            field.text = Msgs.GAME.get(hasCookie ? "l.guest_flowprog_note" : "l.guest_flow_note");

            var later :SimpleButton = (_guestFlowPanel.getChildByName("Later") as SimpleButton);
            later.addEventListener(MouseEvent.CLICK, clearGuestFlow);

            var signUp :SimpleButton = (_guestFlowPanel.getChildByName("SignUp") as SimpleButton);
            signUp.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
                _wctx.getWorldController().handleShowSignUp();
                clearGuestFlow();
            });

            // slide the panel onto the screen, and wait for a click
            _wctx.getTopPanel().getPlaceContainer().addOverlay(
                _guestFlowPanel, PlaceBox.LAYER_TRANSIENT);
            _guestFlowPanel.x = 150;
            _guestFlowPanel.y = -_guestFlowPanel.height;
            Tweener.addTween(_guestFlowPanel, { y: 0, time: 0.75, transition: EASING_OUT });
            _flowPanelAutoDismiss.start();
        }
    }

    protected function clearGuestFlow (... ignored) :void
    {
        _flowPanelAutoDismiss.reset();
        Tweener.addTween(_guestFlowPanel,
            {y: -_guestFlowPanel.height, time: 0.75, transition: EASING_IN,
                onComplete: function () :void {
                    _wctx.getTopPanel().getPlaceContainer().removeOverlay(_guestFlowPanel);
                }
            });
    }

    /** Provides access to main client services. */
    protected var _wctx :WorldContext;

    /** A separate context that connects to the game server. */
    protected var _gctx :GameContext;

    /** The id of the game with which we're dealing. */
    protected var _gameId :int;

    /** The "guest earned flow" popup. */
    protected var _guestFlowPanel :DisplayObjectContainer;

    /** Automatically dismisses the flow panel. */
    protected var _flowPanelAutoDismiss :Timer;

    /** Used to note that we're loading an embedded SWF. */
    protected static const LOADING :Sprite = new Sprite();

    /** The Tweener easing functions used for our award and guest coin displays */
    protected static const EASING_OUT :String = "easeoutbounce";
    protected static const EASING_IN :String = "easeoutcubic";

    [Embed(source="../../../../../../../rsrc/media/award_panel.swf",
           mimeType="application/octet-stream")]
    protected static const AWARD_PANEL :Class;

    [Embed(source="../../../../../../../rsrc/media/guest_flow_panel.swf",
           mimeType="application/octet-stream")]
    protected static const GUEST_FLOW_PANEL :Class;
}
}
