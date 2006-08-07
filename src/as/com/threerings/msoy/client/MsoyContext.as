package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;

import mx.core.Application;

import mx.managers.ISystemManager;

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.parlor.client.ParlorDirector;
import com.threerings.parlor.util.ParlorContext;

import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.spot.client.SpotSceneDirector;
import com.threerings.whirled.util.WhirledContext;

import com.threerings.msoy.client.persist.SharedObjectSceneRepository;
import com.threerings.msoy.data.MemberObject;

public class MsoyContext
    implements WhirledContext, ParlorContext
{
    public function MsoyContext (client :Client, app :Application)
    {
        _client = client;
        _app = app;

        _msgMgr = new MessageManager((app.root as ISystemManager));
        _locDir = new LocationDirector(this);
        _chatDir = new ChatDirector(this, _msgMgr, "general");
        _sceneRepo = new SharedObjectSceneRepository()
        _sceneDir = new SceneDirector(this, _locDir, _sceneRepo,
            new MsoySceneFactory());
        _spotDir = new SpotSceneDirector(this, _locDir, _sceneDir);
        _mediaDir = new MediaDirector(this);
        _parlorDir = new ParlorDirector(this);

        // set up the top panel
        _topPanel = new TopPanel(this, _app);
        _controller = new MsoyController(this, _topPanel);
    }

    /**
     * Convenience method.
     */
    public function displayFeedback (bundle :String, message :String) :void
    {
        _chatDir.displayFeedback(bundle, message);
    }

    /**
     * Convenience method.
     */
    public function displayInfo (bundle :String, message :String) :void
    {
        _chatDir.displayInfo(bundle, message);
    }

    // from PresentsContext
    public function getClient () :Client
    {
        return _client;
    }

    /**
     * Convenience method.
     */
    public function getClientObject () :MemberObject
    {
        return (_client.getClientObject() as MemberObject);
    }

    // from PresentsContext
    public function getDObjectManager () :DObjectManager
    {
        return _client.getDObjectManager();
    }

    // from CrowdContext
    public function getLocationDirector () :LocationDirector
    {
        return _locDir;
    }

    // from CrowdContext
    public function getOccupantDirector () :OccupantDirector
    {
        return null; // TODO
    }

    // from CrowdContext
    public function getChatDirector () :ChatDirector
    {
        return _chatDir;
    }

    // from WhirledContext
    public function getSceneDirector () :SceneDirector
    {
        return _sceneDir;
    }

    // from ParlorContext
    public function getParlorDirector () :ParlorDirector
    {
        return _parlorDir;
    }

    /**
     * Get the SpotSceneDirector.
     */
    public function getSpotSceneDirector () :SpotSceneDirector
    {
        return _spotDir;
    }

    /**
     * Get the media director.
     */
    public function getMediaDirector () :MediaDirector
    {
        return _mediaDir;
    }

    /**
     * Get the message manager.
     */
    public function getMessageManager () :MessageManager
    {
        return _msgMgr;
    }

    // documentation inherited from superinterface CrowdContext
    public function setPlaceView (view :PlaceView) :void
    {
        _topPanel.setPlaceView(view);
    }

    // documentation inherited from superinterface CrowdContext
    public function clearPlaceView (view :PlaceView) :void
    {
        _topPanel.clearPlaceView(view);
    }

    /**
     * Convenience method to translate a key using the general bundle.
     */
    public function xlate (key :String, ... args) :String
    {
        args.unshift(key);
        var mb :MessageBundle = _msgMgr.getBundle("general");
        return mb.get.call(mb, args);
    }

    public function TEMPClearSceneCache () :void
    {
        _sceneRepo.TEMPClearSceneCache();
    }

    protected var _client :Client;

    protected var _app :Application;

    protected var _topPanel :TopPanel;

    protected var _controller :MsoyController;

    protected var _msgMgr :MessageManager;

    protected var _locDir :LocationDirector;

    protected var _sceneDir :SceneDirector;

    protected var _chatDir :ChatDirector;

    protected var _spotDir :SpotSceneDirector;

    protected var _mediaDir :MediaDirector;

    protected var _parlorDir :ParlorDirector;

    protected var _sceneRepo :SharedObjectSceneRepository;
}
}
