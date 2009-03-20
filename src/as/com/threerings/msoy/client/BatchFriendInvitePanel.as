//
// $Id$

package com.threerings.msoy.client {

import com.threerings.presents.client.ConfirmAdapter;

import com.threerings.msoy.notify.data.Notification;

public class BatchFriendInvitePanel extends SelectPlayersPanel
{
    /**
     * Shows the friender popup with the given players and with game-specific text. The callback
     * will be invoked only if the dialog is shown and then closed. Returns true if the dialog is
     * open.
     */
    public static function showPostGame (
        ctx :MsoyContext, playerNames :Array /* of VizMemberName */, onClose :Function) :Boolean
    {
        var panel :BatchFriendInvitePanel = new BatchFriendInvitePanel(ctx, playerNames, "game");
        return panel.maybeOpenWithCallback(onClose);
    }

    /**
     * Shows the friender popup with the given players and with room-specific text. The callback
     * will be invoked only if the dialog is shown and then closed. Returns true if the dialog is
     * open.
     */
    public static function showRoom (
        ctx :MsoyContext, playerNames :Array /* of VizMemberName */, onClose :Function) :Boolean
    {
        var panel :BatchFriendInvitePanel = new BatchFriendInvitePanel(ctx, playerNames, "room");
        return panel.maybeOpenWithCallback(onClose);
    }

    public function BatchFriendInvitePanel (
        ctx :MsoyContext, playerNames :Array /* of VizMemberName */, mode :String)
    {
        super(ctx, playerNames);
        _mode = mode;
    }

    protected function maybeOpenWithCallback (finished :Function) :Boolean
    {
        maybeOpen();
        if (!isOpen()) {
            return false;
        }
        addCloseCallback(finished);
        return true;
    }

    override protected function okButtonClicked () :void
    {
        var msvc :MemberService = _ctx.getClient().requireService(MemberService) as MemberService;
        msvc.inviteAllToBeFriends(_ctx.getMsoyClient(), getSelectedMemberIds(),
            new ConfirmAdapter(invitesSent, invitesFailed));
    }

    override protected function getPanelTitle () :String
    {
        return Msgs.GENERAL.get("t.invite_players_title");
    }

    override protected function getTitle () :String
    {
        return Msgs.GENERAL.get(
            _mode == "game" ? "t.invite_players_game" : "t.invite_players_room",
            _playerNames.length);
    }

    override protected function getOkLabel () :String
    {
        return Msgs.GENERAL.get("b.invite_selected_players");
    }

    override protected function getCancelLabel () :String
    {
        return Msgs.GENERAL.get("b.dont_invite_selected_players");
    }

    override protected function getPrefsName () :String
    {
        return _mode == "game" ? "GameOccupantsInvite" : "RoomOccupantsInvite";
    }

    protected function invitesSent () :void
    {
        _ctx.getNotificationDirector().addGenericNotification(
            "m.invites_sent", Notification.PERSONAL);
    }

    protected function invitesFailed (cause :String) :void
    {
        _ctx.getNotificationDirector().addGenericNotification(cause, Notification.PERSONAL);
    }

    protected var _mode :String;
}

}
