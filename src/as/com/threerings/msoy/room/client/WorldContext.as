//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Stage;

import mx.core.Application;

import mx.managers.ISystemManager;

import com.threerings.util.MessageBundle;

import com.threerings.presents.client.Client;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.spot.client.SpotSceneDirector;
import com.threerings.whirled.util.WhirledContext;

import com.threerings.msoy.chat.client.MsoyChatDirector;
import com.threerings.msoy.game.client.GameDirector;
import com.threerings.msoy.notify.client.NotificationDirector;

import com.threerings.msoy.client.ControlBar;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.client.persist.RuntimeSceneRepository;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyTokenRing;

/**
 * Defines services for the World client.
 */
public class WorldContext extends MsoyContext
    implements WhirledContext
{
    /** Contains non-persistent properties that are set in various places and can be bound to to be
     * notified when they change. */
    public var worldProps :WorldProperties = new WorldProperties();

    public function WorldContext (client :WorldClient)
    {
        super(client);

        // some directors we create here,
        _mediaDir = new MediaDirector(this);
        // and some we want suppressed in StudioContext
        createWorldDirectors();
        _controller = new WorldController(this, _topPanel);
    }

    // from WhirledContext
    public function getSceneDirector () :SceneDirector
    {
        return _sceneDir;
    }

    /**
     * Convenience method.
     */
    public function getMemberObject () :MemberObject
    {
        return (_client.getClientObject() as MemberObject);
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
     * Returns the top-level world controller.
     */
    public function getWorldController () :WorldController
    {
        return _controller;
    }

    // from MsoyContext
    override public function getTokens () :MsoyTokenRing
    {
        // if we're not logged on, claim to have no privileges
        return (getMemberObject() == null) ? new MsoyTokenRing() : getMemberObject().tokens;
    }

    // from MsoyContext
    override public function getMsoyController () :MsoyController
    {
        return _controller;
    }

    // from MsoyContext
    override protected function createControlBar () :ControlBar
    {
        return new WorldControlBar(this);
    }

    // from MsoyContext
    override protected function createChatDirector () :MsoyChatDirector
    {
        return new WorldChatDirector(this);
    }

    /**
     * Create world-specific directors. Overridden in StudioContext to .. not.
     */
    protected function createWorldDirectors () :void
    {
        _sceneDir = new MsoySceneDirector(this, _locDir, new RuntimeSceneRepository());
        _spotDir = new SpotSceneDirector(this, _locDir, _sceneDir);
        _gameDir = new GameDirector(this);
        _worldDir = new WorldDirector(this);
        _notifyDir = new NotificationDirector(this);
        _memberDir = new MemberDirector(this);
    }

    protected var _controller :WorldController;

    protected var _sceneDir :SceneDirector;
    protected var _spotDir :SpotSceneDirector;
    protected var _gameDir :GameDirector;
    protected var _mediaDir :MediaDirector;
    protected var _worldDir :WorldDirector;
    protected var _notifyDir :NotificationDirector;
    protected var _memberDir :MemberDirector;
}
}
