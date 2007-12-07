//
// $Id$

package com.threerings.msoy.game.client {

import flash.display.Stage;
import flash.display.StageQuality;

import com.threerings.util.Log;

import com.threerings.presents.net.Credentials;

import com.threerings.parlor.data.ParlorMarshaller;

import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.data.all.ItemPack;
import com.threerings.msoy.item.data.all.LevelPack;

import com.threerings.msoy.game.data.MsoyGameCredentials;

import mx.resources.ResourceBundle;

/**
 * A simple client that displays a lobby and plays games.
 */
public class GameClient extends MsoyClient
{
    public function GameClient (stage :Stage)
    {
        super(stage);

        // TODO: should we do things differently for games
        stage.quality = StageQuality.MEDIUM;

        // make sure we're running a sufficiently new version of Flash
        if (_wctx.getTopPanel().verifyFlashVersion()) {
            logon(); // now logon
        }

        addServiceGroup(MsoyCodes.GAME_GROUP);
    }

    // from MsoyClient
    override public function fuckingCompiler () :void
    {
        super.fuckingCompiler();
        var c :Class;
        c = Game;
        c = ItemPack;
        c = LevelPack;
        c = ParlorMarshaller;

        // these cause bundles to be compiled in.
        [ResourceBundle("general")]
        [ResourceBundle("game")]
        [ResourceBundle("ezgame")]
        var rb :ResourceBundle;
    }

    // from MsoyClient
    override protected function createContext () :MsoyContext
    {
        return (_gctx = new GameContextImpl(this));
    }

    // from MsoyClient
    override protected function configureExternalFunctions () :void
    {
        super.configureExternalFunctions();

        // TODO
    }

    // from MsoyClient
    override protected function createStartupCreds (token :String) :Credentials
    {
        var params :Object = _stage.loaderInfo.parameters;
        var creds :MsoyGameCredentials = new MsoyGameCredentials();
        creds.sessionToken = (token == null) ? params["token"] : token;
        return creds;
    }

    protected var _gctx :GameContext;

    private static const log :Log = Log.getLog(GameClient);
}
}

import com.threerings.parlor.client.ParlorDirector;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.client.GameContext;

class GameContextImpl extends MsoyContext
    implements GameContext
{
    public function GameContextImpl (client :GameClient)
    {
        super(client);

        // create our directors
//         _chatDtr = new GameChatDirector(this);
        _parDtr = new ParlorDirector(this);
    }

//     // from CrowdContext
//     public function getChatDirector () :ChatDirector
//     {
//         return _chatDtr;
//     }

    // from ParlorContext
    public function getParlorDirector () :ParlorDirector
    {
        return _parDtr;
    }

    // from GameContext
    public function getMsoyContext () :MsoyContext
    {
        return this;
    }

    // from GameContext
    public function getPlayerObject () :PlayerObject
    {
        return (_client.getClientObject() as PlayerObject);
    }

    // from GameContext
    public function backToWhirled (showLobby :Boolean) :void
    {
        throw new Error("TODO");
    }

    // from GameContext
    public function getOnlineFriends () :Array
    {
        return []; // TODO
    }

    protected var _parDtr :ParlorDirector;
}
}
