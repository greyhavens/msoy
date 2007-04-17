package com.threerings.msoy.game.client {

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.dobj.ObjectAccessError;
import com.threerings.presents.dobj.Subscriber;

import com.threerings.parlor.client.Invitation;
import com.threerings.parlor.client.InvitationHandler;
import com.threerings.parlor.client.InvitationResponseObserver;

import com.threerings.parlor.client.GameReadyObserver;
import com.threerings.parlor.game.client.GameController;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.data.GameObject;

import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.game.data.GameCodes;
import com.threerings.msoy.game.data.WorldGameConfig;

/**
 * A director that manages invitations and game starting.
 */

// TODO: All this Invitation stuff needs to be ripped out.
// In metasoy you will not invite players to a game configuration (do you?)
// you instead invite to the lobby... etc.
// 
public class GameDirector extends BasicDirector
    implements InvitationHandler, InvitationResponseObserver, AttributeChangeListener, Subscriber,
               GameReadyObserver
{
    public static const log :Log = Log.getLog(GameDirector);

    public function GameDirector (ctx :WorldContext)
    {
        super(ctx);
        _mctx = ctx;

        ctx.getParlorDirector().setInvitationHandler(this);

        // handle gameReady so that we can enter games in a browser history friendly manner
        ctx.getParlorDirector().addGameReadyObserver(this);
    }

    /**
     * Called to pop-up a panel to configure an invitation to the specified
     * player.
     */
    public function configureInvite (invitee :MemberName) :void
    {
        displayFeedback("Hopefully nothing is actually calling this code");
//        new InvitePanel(_mctx, invitee);
    }

    /**
     * Send an invitation to the specified player, managing the handling
     * of all responses.
     */
    public function sendInvite (invitee :MemberName, config :GameConfig) :void
    {
        _invitation = _mctx.getParlorDirector().invite(invitee, config, this);
    }

    // from InvitationHandler
    public function invitationReceived (invite :Invitation) :void
    {
        displayFeedback(
            MessageBundle.tcompose("m.invite_received", invite.opponent));
        // TODO: an ahoy panel of sorts?
        invite.accept();
    }

    // from InvitationHandler
    public function invitationCancelled (invite :Invitation) :void
    {
        displayFeedback(
            MessageBundle.tcompose("m.invite_cancelled", invite.opponent));
    }

    // from InvitationResponseObserver
    public function invitationAccepted (invite :Invitation) :void
    {
        _invitation = null;
        displayFeedback(
            MessageBundle.tcompose("m.invite_accepted", invite.opponent));
    }

    // from InvitationResponseObserver
    public function invitationRefused (invite :Invitation, msg :String) :void
    {
        displayFeedback(
            MessageBundle.tcompose("m.invite_refused", invite.opponent));
    }

    // from InvitationResponseObserver
    public function invitationCountered (
            invite :Invitation, config :GameConfig) :void
    {
        // TODO ??
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
        // the config is set in the memberobject simultaneously with the oid.
        // so if the oid is up-to-date, we can trust the config as well
        var cfg :WorldGameConfig = _mctx.getMemberObject().worldGameCfg;
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
        _mctx.getSceneDirector().didLeaveScene();
        _mctx.getMsoyController().handleGoLocation(gameOid);
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

    /** The invitation we're currently processing. */
    protected var _invitation :Invitation;

    /** The oid of the world game object to which we are subscribed or are subscribing to. */
    protected var _worldGameOid :int;

    /** The current world game object. */
    protected var _worldGameObj :GameObject;

    /** The controller for the current world game. */
    protected var _worldGameCtrl :GameController;
}
}
