package com.threerings.msoy.game.client {

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.BasicDirector;

import com.threerings.parlor.client.Invitation;
import com.threerings.parlor.client.InvitationHandler;
import com.threerings.parlor.client.InvitationResponseObserver;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MemberName;

import com.threerings.msoy.game.data.GameCodes;

/**
 * A director that manages invitations and game starting.
 */
public class GameDirector extends BasicDirector
    implements InvitationHandler, InvitationResponseObserver
{
    public function GameDirector (ctx :MsoyContext)
    {
        super(ctx);
        _mctx = ctx;

        ctx.getParlorDirector().setInvitationHandler(this);
    }

    /**
     * Called to pop-up a panel to configure an invitation to the specified
     * player.
     */
    public function configureInvite (invitee :MemberName) :void
    {
        new InvitePanel(_mctx, invitee);
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

    /**
     * A convenience method to display feedback using the game bundle.
     */
    protected function displayFeedback (msg :String) :void
    {
        _mctx.displayFeedback(GameCodes.GAME_BUNDLE, msg);
    }

    /** A casted ref to the msoy context. */
    protected var _mctx :MsoyContext;

    /** The invitation we're currently processing. */
    protected var _invitation :Invitation;
}
}
