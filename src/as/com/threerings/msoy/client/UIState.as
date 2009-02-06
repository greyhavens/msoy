//
// $Id$

package com.threerings.msoy.client {

import flash.events.EventDispatcher;

/**
 * Records the various flags of what state the UI is in. When values change, events are dispatched
 * to listeners.
 */
public class UIState extends EventDispatcher
{
    /**
     * Are we in game mode?
     */
    public function get inGame () :Boolean
    {
        return _inGame;
    }

    /**
     * If in game mode, are we multiplayer?
     */
    public function get multiplayer () :Boolean
    {
        return _multiplayer;
    }

    /**
     * Are we embedded?
     */
    public function get embedded () :Boolean
    {
        return _embedded;
    }

    /**
     * Are we in a game lobby?
     */
    public function get inLobby () :Boolean
    {
        return _inLobby;
    }

    /**
     * Are we in a room?
     */
    public function get inRoom () :Boolean
    {
        return _inRoom;
    }

    /**
     * Are we in an avr game?
     */
    public function get inAVRGame () :Boolean
    {
        return _inAVRGame;
    }

    /**
     * Should the chat controls be shown?
     */
    public function get showChat () :Boolean
    {
        if (embedded) {
            return inGame && multiplayer;
        }
        return true;
    }

    /**
     * Called to tell us we are now in game mode and whether the game has more than one player.
     */
    public function setInGame (inGame :Boolean, multiplayer :Boolean) :void
    {
        _inGame = inGame;
        _multiplayer = multiplayer;
        dispatch();
    }

    /**
     * Called to tell us whether we are in embedded mode.
     */
    public function setEmbedded (embedded :Boolean) :void
    {
        _embedded = embedded;
        dispatch();
    }

    /**
     * Called to tell us we are in a game lobby.
     */
    public function setInLobby (inlobby :Boolean) :void
    {
        _inLobby = inLobby;
        dispatch();
    }

    /**
     * Called to tell us we are in a room.
     */
    public function setInRoom (inRoom :Boolean) :void
    {
        _inRoom = inRoom;
        dispatch();
    }

    /**
     * Called to tell us we are in an AVR game.
     */
    public function setInAVRGame (inAVRGame :Boolean) :void
    {
        _inAVRGame = inAVRGame;
        dispatch();
    }

    protected function dispatch () :void
    {
        dispatchEvent(new UIStateChangeEvent());
    }

    protected var _inGame :Boolean;
    protected var _multiplayer :Boolean;
    protected var _embedded :Boolean;
    protected var _inLobby :Boolean;
    protected var _inRoom :Boolean;
    protected var _inAVRGame :Boolean;
}
}
