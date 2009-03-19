//
// $Id$

package com.threerings.msoy.client {

import com.threerings.io.TypedArray;

import com.threerings.presents.client.ConfirmAdapter;

import com.threerings.msoy.notify.data.Notification;

public class BatchFriendInvitePanel extends SelectPlayersPanel
{
    public static function showPostGame (
        ctx :MsoyContext, playerNames :Array /* of VizMemberName */) :void
    {
        var panel :BatchFriendInvitePanel = new BatchFriendInvitePanel(ctx, playerNames, "game");
        panel.maybeOpen();
    }

    public static function showRoom (
        ctx :MsoyContext, playerNames :Array /* of VizMemberName */) :void
    {
        var panel :BatchFriendInvitePanel = new BatchFriendInvitePanel(ctx, playerNames, "room");
        panel.maybeOpen();
    }

    public function BatchFriendInvitePanel (
        ctx :MsoyContext, playerNames :Array /* of VizMemberName */, mode :String)
    {
        super(ctx, playerNames);
        _mode = mode;
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
