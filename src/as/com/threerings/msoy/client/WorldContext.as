//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Stage;

import mx.core.Application;

import mx.managers.ISystemManager;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.Client;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.spot.client.SpotSceneDirector;
import com.threerings.whirled.util.WhirledContext;

import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.client.persist.RuntimeSceneRepository;

import com.threerings.msoy.game.client.GameDirector;
import com.threerings.msoy.game.client.WhirledGameContext;
import com.threerings.msoy.notify.client.NotificationDirector;
import com.threerings.msoy.world.client.MsoySceneDirector;
import com.threerings.msoy.world.client.WorldDirector;

/**
 * Defines services for the main virtual world and game clients. TODO: make GameContext?
 */
public class WorldContext extends BaseContext
    implements WhirledContext, WhirledGameContext
{
    /** Contains non-persistent properties that are set in various places and can be bound to to be
     * notified when they change. */
    public var worldProps :WorldProperties = new WorldProperties();

    public function WorldContext (client :Client)
    {
        super(client);

        _sceneDir = new MsoySceneDirector(this, _locDir, new RuntimeSceneRepository());
        _spotDir = new SpotSceneDirector(this, _locDir, _sceneDir);
        _mediaDir = new MediaDirector(this);
        _gameDir = new GameDirector(this);
        _worldDir = new WorldDirector(this);
        _notifyDir = new NotificationDirector(this);
        _memberDir = new MemberDirector(this);

        // set up the top panel
        _topPanel = new TopPanel(this);
        _controller = new MsoyController(this, _topPanel);
    }

    // from WhirledContext
    public function getSceneDirector () :SceneDirector
    {
        return _sceneDir;
    }

    // from WhirledGameContext
    public function getTopPanel () :TopPanel
    {
        return _topPanel;
    }

    // from WhirledGameContext
    public function displayLobby (gameId :int) :void
    {
        getMsoyController().handleJoinGameLobby(gameId);
    }

    /**
     * Returns our client casted to a WorldClient.
     */
    public function getWorldClient () :WorldClient
    {
        return (getClient() as WorldClient);
    }

    /**
     * Get the media director.
     */
    public function getMediaDirector () :MediaDirector
    {
        return _mediaDir;
    }

    /**
     * Get the notification director.
     */
    public function getNotificationDirector () :NotificationDirector
    {
        return _notifyDir;
    }

    /**
     * Get the GameDirector.
     */
    public function getGameDirector () :GameDirector
    {
        return _gameDir;
    }

    /**
     * Get the WorldDirector.
     */
    public function getWorldDirector () :WorldDirector
    {
        return _worldDir;
    }

    /**
     * Get the SpotSceneDirector.
     */
    public function getSpotSceneDirector () :SpotSceneDirector
    {
        return _spotDir;
    }

    /**
     * Get the MemberDirector.
     */
    public function getMemberDirector () :MemberDirector
    {
        return _memberDir;
    }

    /**
     * Get the top-level msoy controller.
     */
    public function getMsoyController () :MsoyController
    {
        return _controller;
    }

    // from BaseContext
    override public function setPlaceView (view :PlaceView) :void
    {
        _topPanel.setPlaceView(view);
    }

    // from BaseContext
    override public function clearPlaceView (view :PlaceView) :void
    {
        _topPanel.clearPlaceView(view);
    }

    override protected function createChatDirector () :ChatDirector
    {
        return new MsoyChatDirector(this);
    }

    protected var _topPanel :TopPanel;
    protected var _controller :MsoyController;

    protected var _sceneDir :SceneDirector;
    protected var _spotDir :SpotSceneDirector;
    protected var _gameDir :GameDirector;
    protected var _mediaDir :MediaDirector;
    protected var _worldDir :WorldDirector;
    protected var _notifyDir :NotificationDirector;
    protected var _memberDir :MemberDirector;
}
}
