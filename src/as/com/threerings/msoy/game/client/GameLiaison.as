//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.DisplayObjectContainer;
import flash.display.Loader;
import flash.display.LoaderInfo;
import flash.display.MovieClip;
import flash.display.SimpleButton;
import flash.display.Sprite;
import flash.system.Security;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.text.TextField;

import mx.core.Container;

import com.whirled.game.data.WhirledGameObject;

import com.threerings.flash.path.Path;
import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.MultiLoader;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.client.ClientObserver;
import com.threerings.presents.client.ResultWrapper;
import com.threerings.presents.dobj.MessageEvent;
import com.threerings.presents.dobj.MessageListener;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.ui.MsoyMediaContainer;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.MediaDesc;
import com.threerings.msoy.item.data.all.TrophySource;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.game.data.MsoyGameCodes;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.data.all.Trophy;

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

    /**
     * Starts this game liaison. If the game host and port are supplied, they will be used
     * immediately, otherwise the liaison will first ask its world server to locate the game in
     * question.
     */
    public function start (ghost :String = null, gport :int = 0) :void
    {
        if (ghost != null && gport != 0) {
            gameLocated(ghost, gport);
        } else {
            log.info("Resolving location of game [id=" + _gameId + "].");
            var mgsvc :MsoyGameService =
                (_wctx.getClient().requireService(MsoyGameService) as MsoyGameService);
            mgsvc.locateGame(_wctx.getClient(), gameId, this);
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
        // remove any trophy panel we might have lying around
        if (_awardPanel != null && _awardPanel.parent != null) {
            _wctx.getTopPanel().getPlaceContainer().removeOverlay(_awardPanel);
            // if the path completes after this, it will generate a warning, but in "theory" it
            // should stop receiving onEnterFrame when it's removed from the hierarchy
        }
        // tell the game director that we're audi
        _wctx.getGameDirector().liaisonCleared(this);
    }

    // from interface MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        const name :String = event.getName();
        if (name == MsoyGameCodes.TROPHY_AWARDED || name == MsoyGameCodes.PRIZE_AWARDED) {
            _pendingAwards.push(event.getArgs()[0]);
            checkPendingAwards();

        } else if (name == WhirledGameObject.COINS_AWARDED_MESSAGE &&
                _gctx.getPlayerObject().isGuest()) {
            // if a guest earns flow, we want to show them the "please register" dialog
            displayGuestFlowEarnage(int(event.getArgs()[0]));
        }
    }

    protected function checkPendingAwards () :void
    {
        // if we haven't yet loaded our trophy panel, do that
        if (_awardPanel == null) {
            _awardPanel = LOADING;
            MultiLoader.getContents(AWARD_PANEL, function (result :DisplayObjectContainer) :void {
                _awardPanel = result;
                checkPendingAwards();
            });

        } else if (_awardPanel == LOADING || _awardPanel.stage != null ||
                   _pendingAwards.length == 0) {
            // we're loading the award panel or it's being used or we're done

        } else {
            // otherwise pop the next award from the list and display it
            displayAward(_pendingAwards.pop());
        }
    }

    protected function displayAward (award :Object) :void
    {
        var feedback :String, name :String, title :String;
        var media :MediaDesc;
        if (award is Trophy) {
            var trophy :Trophy = (award as Trophy);
            feedback = MessageBundle.tcompose("m.trophy_earned", trophy.name);
            name = trophy.name;
            title = "m.trophy_title";
            media = trophy.trophyMedia;

        } else if (award is Item) {
            var item :Item = (award as Item);
            feedback = MessageBundle.tcompose("m.prize_earned", item.name);
            name = item.name;
            title = "m.prize_title";
            media = item.getThumbnailMedia();

        } else {
            log.warning("Requested to display unknown award " + award + ".");
            checkPendingAwards();
            return;
        }

        // display a chat message reporting their award
        _gctx.getChatDirector().displayFeedback(MsoyCodes.GAME_MSGS, feedback);

        // configure the award display panel with the award info
        (_awardPanel.getChildByName("statement") as TextField).text =
            _wctx.xlate(MsoyCodes.GAME_MSGS, title);
        (_awardPanel.getChildByName("trophy_name") as TextField).text = name;
        var clip :MovieClip = (_awardPanel.getChildByName("trophy") as MovieClip);
        while (clip.numChildren > 0) { // remove any old trophy image or the sample
            clip.removeChildAt(0);
        }
        var image :MsoyMediaContainer = new MsoyMediaContainer(media);
        clip.addChild(image);

        // wait for the award image to load
        var linfo :LoaderInfo = (image.getMedia() as Loader).contentLoaderInfo;
        linfo.addEventListener(Event.COMPLETE, function (event :Event) :void {
            // center the award image
            image.x -= image.getContentWidth()/2;
            image.y -= image.getContentHeight()/2;
            // then slide the award panel onto the screen, pause for a sec, then back off
            var container :PlaceBox = _wctx.getTopPanel().getPlaceContainer();
            var path :Path = Path.connect(
                Path.move(_awardPanel, 250, -_awardPanel.height, 250, 0, 500),
                Path.delay(3000), // TODO: play a sound when this path starts
                Path.move(_awardPanel, 250, 0, 250, -_awardPanel.height, 500));
            path.setOnComplete(function (path :Path) :void {
                container.removeOverlay(_awardPanel);
                checkPendingAwards();
            });
            path.start();
            container.addOverlay(_awardPanel, PlaceBox.LAYER_TRANSIENT);
        });
    }

    protected function displayGuestFlowEarnage (amount :int) :void
    {
        if (_guestFlowPanel == null) {
            MultiLoader.getContents(
                GUEST_FLOW_PANEL, function (result :DisplayObjectContainer) :void {
                _guestFlowPanel = result;
                displayGuestFlowEarnage(amount);
            });

        } else if (_guestFlowPanel == LOADING || _guestFlowPanel.stage != null) {
            return; // we're loading it or already showing it

        } else {
            var field :TextField = (_guestFlowPanel.getChildByName("youearned") as TextField);
            field.text = Msgs.GAME.get("l.guest_flow_title", ""+amount);
            field = (_guestFlowPanel.getChildByName("ifyousign") as TextField);
            field.text = Msgs.GAME.get("l.guest_flow_note");

            var later :SimpleButton = (_guestFlowPanel.getChildByName("Later") as SimpleButton);
            later.addEventListener(MouseEvent.CLICK, clearGuestFlow);

            var signUp :SimpleButton = (_guestFlowPanel.getChildByName("SignUp") as SimpleButton);
            signUp.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
                _wctx.getWorldController().handleShowSignUp();
                clearGuestFlow();
            });

            // slide the panel onto the screen, and wait for a click
            var path :Path = Path.move(_guestFlowPanel, 150, -_guestFlowPanel.height, 150, 0, 500);
            path.start();
            _wctx.getTopPanel().getPlaceContainer().addOverlay(
                _guestFlowPanel, PlaceBox.LAYER_TRANSIENT);
        }
    }

    protected function clearGuestFlow (event :MouseEvent = null) :void
    {
        var path :Path = Path.move(_guestFlowPanel, 150, 0, 150, -_guestFlowPanel.height, 500);
        path.setOnComplete(function (path :Path) :void {
            _wctx.getTopPanel().getPlaceContainer().removeOverlay(_guestFlowPanel);
        });
        path.start();
    }

    /** Provides access to main client services. */
    protected var _wctx :WorldContext;

    /** A separate context that connects to the game server. */
    protected var _gctx :GameContext;

    /** The id of the game with which we're dealing. */
    protected var _gameId :int;

    /** The "guest earned flow" popup. */
    protected var _guestFlowPanel :DisplayObjectContainer;

    /** The award display movie. */
    protected var _awardPanel :DisplayObjectContainer;

    /** Awards waiting to be displayed. Either Trophy or Item. */
    protected var _pendingAwards :Array = [];

    /** Used to note that we're loading an embedded SWF. */
    protected const LOADING :Sprite = new Sprite();

    [Embed(source="../../../../../../../rsrc/media/award_panel.swf",
           mimeType="application/octet-stream")]
    protected static const AWARD_PANEL :Class;

    [Embed(source="../../../../../../../rsrc/media/guest_flow_panel.swf",
           mimeType="application/octet-stream")]
    protected static const GUEST_FLOW_PANEL :Class;
}
}
