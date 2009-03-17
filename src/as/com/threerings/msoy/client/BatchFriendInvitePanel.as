package com.threerings.msoy.client {

public class BatchFriendInvitePanel extends SelectPlayersPanel
{
    public static function showPostGame (ctx :MsoyContext, playerIds :Array) :void
    {
        var panel :BatchFriendInvitePanel = new BatchFriendInvitePanel(ctx, playerIds, "game");
        panel.maybeOpen();
    }

    public static function showRoom (ctx :MsoyContext, playerIds :Array) :void
    {
        var panel :BatchFriendInvitePanel = new BatchFriendInvitePanel(ctx, playerIds, "room");
        panel.maybeOpen();
    }

    public function BatchFriendInvitePanel (ctx :MsoyContext, playerIds :Array, mode :String)
    {
        super(ctx, playerIds);
        _mode = mode;
    }

    override protected function getPanelTitle () :String
    {
        return Msgs.GENERAL.get("t.invite_players_title");
    }

    override protected function getTitle () :String
    {
        return Msgs.GENERAL.get(
            _mode == "game" ? "t.invite_players_game" : "t.invite_players_room");
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

    protected var _mode :String;
}

}
