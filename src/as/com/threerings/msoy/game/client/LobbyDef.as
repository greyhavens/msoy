//
// $Id$

package com.threerings.msoy.game.client {

import com.threerings.msoy.game.data.LobbyCodes;

/**
 * Data structure that holds lobby configuration. 
 */
public class LobbyDef
{
    /** Default value for just lobbying, without automatically starting a game. */ 
    public static const LOBBY_ONLY :LobbyDef = new LobbyDef(false, LobbyCodes.PLAY_NOW_IF_SINGLE);
    
    /** Default definition that will automatically start the game. */
    public static const PLAY_NOW :LobbyDef = new LobbyDef(true, LobbyCodes.PLAY_NOW_IF_SINGLE);

    /**
     * Creates a new lobby definition.
     * 
     * @param playNow If true, the server will attempt to start a game right away
     * @param playNowMode One of the {@link LobbyCodes} constants, defines what kind of a game
     *        should be started. Only meaningful if playNow is true.
     * @param multiplayerLobby Specifies whether lobby display should include just 
     *        multiplayer options (when available), or the defaults.
     */ 
    public function LobbyDef (
        playNow :Boolean, playNowMode :int, multiplayerLobby :Boolean = false)
    {
        this._playNow = playNow; 
        this._playNowMode = playNowMode;
        this._multiplayerLobby = multiplayerLobby;
    }

    /** Returns true if the server is supposed to start the game automatically. */
    public function get playNow () :Boolean
    {
        return _playNow;
    } 
    
    /** Returns one of the {@link LobbyCodes} constants, telling the server how to auto-start. */ 
    public function get playNowMode () :int
    {
        return _playNowMode;
    }
    
    /** When we display a lobby, should it be multiplayer only, rather than default? */
    public function get multiplayerLobby () :Boolean
    {
        return _multiplayerLobby;
    }
    
    protected var _playNow :Boolean;
    protected var _playNowMode :int; // of LobbyCodes
    protected var _multiplayerLobby :Boolean;
}
}
