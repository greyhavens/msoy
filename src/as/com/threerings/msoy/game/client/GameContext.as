//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.parlor.client.ParlorDirector;
import com.threerings.parlor.util.ParlorContext;

import com.threerings.msoy.client.TopPanel;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyCredentials;

import com.threerings.msoy.game.data.MsoyGameCredentials;
import com.threerings.msoy.game.data.PlayerObject;

/**
 * Provides access to our various game services. Game services are run via a separate client that's
 * connected to a separate game server.
 */
public class GameContext
    implements ParlorContext
{
    public function GameContext (ctx :WorldContext)
    {
        _wctx = ctx;

        // set up our client with our world credentials
        var wcreds :MsoyCredentials = (ctx.getClient().getCredentials() as MsoyCredentials);
        var gcreds :MsoyGameCredentials = new MsoyGameCredentials();
        gcreds.sessionToken = wcreds.sessionToken;
        _client = new Client(gcreds, ctx.getStage());
        _client.addServiceGroup(MsoyCodes.GAME_GROUP);

        // create our directors
        _locDtr = new LocationDirector(this);
        _chatDtr = new GameChatDirector(this);
        _parDtr = new ParlorDirector(this);
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
        // leave our current scene as we're about to display the game
        _wctx.getLocationDirector().leavePlace();
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

    /**
     * Returns a reference to the WorldContext.
     */

    public function getWorldContext () :WorldContext
    {
        return _wctx;
    }

    /**
     * Returns a reference to the top-level UI container.
     */
    public function getTopPanel () :TopPanel
    {
        return _wctx.getTopPanel();
    }

    /**
     * Displays the lobby for the specified game.
     */
    public function displayLobby (gameId :int) :void
    {
        _wctx.getMsoyController().handleJoinGameLobby(gameId);
    }

    /**
     * Returns the message manager which can be used to translate things.
     */
    public function getMessageManager () :MessageManager
    {
        return _wctx.getMessageManager();
    }

    /**
     * Convenience method.
     */
    public function getPlayerObject () :PlayerObject
    {
        return (_client.getClientObject() as PlayerObject);
    }

    protected var _wctx :WorldContext;
    protected var _client :Client;
    protected var _locDtr :LocationDirector;
    protected var _chatDtr :ChatDirector;
    protected var _parDtr :ParlorDirector;
}
}
