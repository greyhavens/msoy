//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.parlor.client.GameReadyObserver;
import com.threerings.parlor.game.client.GameController;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.item.data.all.Game;

import com.threerings.msoy.game.data.GameCodes;
import com.threerings.msoy.game.data.MsoyGameDefinition;
import com.threerings.msoy.game.data.WorldGameConfig;

/**
 * A director that manages game related bits.
 */
public class GameDirector extends BasicDirector
    implements AttributeChangeListener, Subscriber, GameReadyObserver
{
    public static const log :Log = Log.getLog(GameDirector);

    public function GameDirector (ctx :WorldContext)
    {
        super(ctx);
        _mctx = ctx;

        // handle gameReady so that we can enter games in a browser history friendly manner
        ctx.getParlorDirector().addGameReadyObserver(this);

        // let the compiler know that these must be compiled into the client
        var c :Class = MsoyGameDefinition;
    }

    /**
     * Called by the LobbyController when we join or create a game table. We need to keep this
     * around so that we can enter the game properly if/when we end up in an actual game.
     */
    public function setMatchingGame (game :Game) :void
    {
        _matchingGame = game;
    }

    // from interface AttributeChangeListener
    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (event.getName() == MemberObject.WORLD_GAME_OID) {
            updateWorldGame();
        }
    }

    // from interface Subscriber
    public function objectAvailable (obj :DObject) :void
    {
        if (obj.getOid() != _worldGameOid) {
            // we changed our minds!
            _mctx.getDObjectManager().unsubscribeFromObject(obj.getOid(), this);
            return;

        } else if (obj == _worldGameObj) {
            // already subscribed
            return;
        }

        _worldGameObj = (obj as GameObject);
        // the config is set in the memberobject simultaneously with the oid.  so if the oid is
        // up-to-date, we can trust the config as well
        var cfg :WorldGameConfig = (_mctx.getMemberObject().worldGameCfg as WorldGameConfig);
        _worldGameCtrl = (cfg.createController() as GameController);
        _worldGameCtrl.init(_mctx, cfg);
        _worldGameCtrl.willEnterPlace(_worldGameObj);
    }

    // from interface Subscriber
    public function requestFailed (oid :int, cause :ObjectAccessError) :void
    {
        log.warning("Failed to subscribe to world game object [oid=" + oid +
                    ", cause=" + cause + "].");
        _worldGameOid = 0;
    }

    // from GameReadyObserver
    public function receivedGameReady (gameOid :int) :Boolean
    {
        // let the scene director know that we're leaving our current scene
        _mctx.getTopPanel().clearTableDisplay();
        if (_matchingGame == null) {
            log.warning("Got game ready but we were never in a table? [oid=" + gameOid + "].");
        } else {
            // route our entry to the game through GWT so that we can handle non-Flash games
            _mctx.getMsoyController().handleGoGame(_matchingGame.itemId, gameOid);
        }
        return true;
    }

    override protected function clientObjectUpdated (client :Client) :void
    {
        // listen for changes to the in-world game oid
        updateWorldGame();
        client.getClientObject().addListener(this);
    }

    /**
     * Called to create, remove, or change the in-world game.
     */
    protected function updateWorldGame () :void
    {
        var noid :int = _mctx.getMemberObject().worldGameOid;
        if (noid == _worldGameOid) {
            return;
        }
        if (_worldGameOid != 0) {
            if (_worldGameCtrl != null) {
                _worldGameCtrl.didLeavePlace(_worldGameObj);
                _worldGameCtrl = null;
            }
            _mctx.getDObjectManager().unsubscribeFromObject(_worldGameOid, this);
            _worldGameObj = null;
        }
        _worldGameOid = noid;
        if (_worldGameOid != 0) {
            _mctx.getDObjectManager().subscribeToObject(_worldGameOid, this);
        }
    }

    /**
     * A convenience method to display feedback using the game bundle.
     */
    protected function displayFeedback (msg :String) :void
    {
        _mctx.displayFeedback(GameCodes.GAME_BUNDLE, msg);
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.GAME_GROUP);
    }

    /** A casted ref to the msoy context. */
    protected var _mctx :WorldContext;

    /** Tracks the game id of the last game of which we joined a table. We need to remember this
     * because by the time we get around to entering that game, we no longer have this info. */
    protected var _matchingGame :Game;

    /** The oid of the world game object to which we are subscribed or are subscribing to. */
    protected var _worldGameOid :int;

    /** The current world game object. */
    protected var _worldGameObj :GameObject;

    /** The controller for the current world game. */
    protected var _worldGameCtrl :GameController;
}
}
