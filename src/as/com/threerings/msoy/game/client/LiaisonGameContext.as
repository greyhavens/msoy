//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.ChatFilter;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyCredentials;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.game.data.MsoyGameCredentials;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.world.client.WorldContext;
import com.threerings.parlor.client.ParlorDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

/**
 * Provides context for games running in the World client via a liaison.
 */
public class LiaisonGameContext
    implements GameContext
{
    public function LiaisonGameContext (wctx :WorldContext)
    {
        _wctx = wctx;

        // set up our client with our world credentials
        var wcreds :MsoyCredentials = (wctx.getClient().getCredentials() as MsoyCredentials);
        // if we are a guest and have an assigned member name, pass it along to the game server so
        // that it will show us the same guest name that we had on the server
        var name :MemberName = null;
        if (_wctx.getMemberObject() != null && _wctx.getMemberObject().isGuest()) {
            name = _wctx.getMemberObject().memberName;
        }
        var gcreds :MsoyGameCredentials = new MsoyGameCredentials(name);
        gcreds.sessionToken = wcreds.sessionToken;
        _client = new Client(gcreds);
        _client.addServiceGroup(MsoyCodes.GAME_GROUP);

        // create our directors
        _locDtr = new LocationDirector(this);
        _chatDtr = new GameChatDirector(this);
        _parDtr = new ParlorDirector(this);
        // use all the same chat filters for games
        for each (var filter :ChatFilter in wctx.getMsoyChatDirector().getChatFilters()) {
            _chatDtr.addChatFilter(filter);
        }
    }

    // from PresentsContext
    public function getClient () :Client
    {
        return _client;
    }

    // from PresentsContext
    public function getDObjectManager () :DObjectManager
    {
        return _client.getDObjectManager();
    }

    // from CrowdContext
    public function getLocationDirector () :LocationDirector
    {
        return _locDtr;
    }

    // from CrowdContext
    public function getOccupantDirector () :OccupantDirector
    {
        return null; // NOT USED
    }

    // from CrowdContext
    public function getChatDirector () :ChatDirector
    {
        return _chatDtr;
    }

    // from CrowdContext
    public function setPlaceView (view :PlaceView) :void
    {
        _wctx.setPlaceView(view);
    }

    // from CrowdContext
    public function clearPlaceView (view :PlaceView) :void
    {
        _wctx.clearPlaceView(view);
    }

    // from ParlorContext
    public function getParlorDirector () :ParlorDirector
    {
        return _parDtr;
    }

    // from GameContext
    public function getMsoyContext () :MsoyContext
    {
        return _wctx;
    }

    // from GameContext
    public function getPlayerObject () :PlayerObject
    {
        return (_client.getClientObject() as PlayerObject);
    }

    // from GameContext
    public function backToWhirled (showLobby :Boolean) :void
    {
        _wctx.getGameDirector().backToWhirled(showLobby);
    }

    // from GameContext
    public function showGameInstructions () :void
    {
        _wctx.getWorldController().displayPage("games", "d_" + _wctx.getGameDirector().getGameId());
    }

    // from GameContext
    public function showGameShop (itemType :int, catalogId :int = 0) :void
    {
        _wctx.getWorldController().handleViewGameShop(
            _wctx.getGameDirector().getGameId(), itemType, catalogId);
    }

    // from GameContext
    public function getOnlineFriends () :Array
    {
        return _wctx.getMemberObject().friends.toArray().filter(
            function (friend :FriendEntry, index :int, array :Array) :Boolean {
                return friend.online;
            });
    }

    protected var _wctx :WorldContext;
    protected var _client :Client;
    protected var _locDtr :LocationDirector;
    protected var _chatDtr :ChatDirector;
    protected var _parDtr :ParlorDirector;
}
}
