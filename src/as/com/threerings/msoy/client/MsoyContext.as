package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.Stage;

import mx.core.Application;

import mx.managers.ISystemManager;

import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.whirled.client.SceneDirector;
import com.threerings.whirled.spot.client.SpotSceneDirector;
import com.threerings.whirled.util.WhirledContext;

import com.threerings.msoy.client.persist.SharedObjectSceneRepository;

public class MsoyContext
    implements WhirledContext
{
    public function MsoyContext (client :Client, app :Application)
    {
        _client = client;
        _app = app;

        // TODO: verify params to these constructors
        _msgmgr = new MessageManager("rsrc", (app.root as ISystemManager));
        _locdir = new LocationDirector(this);
        _chatdir = new ChatDirector(this, _msgmgr, "general");
        _scenedir = new SceneDirector(this, _locdir,
            new SharedObjectSceneRepository(), new MsoySceneFactory());
        _spotdir = new SpotSceneDirector(this, _locdir, _scenedir);

        // clean up the application, set up the top panel
        _app.removeAllChildren();
        _topPanel = new TopPanel(this);
        _app.addChild(_topPanel);
    }

    /**
     * Convenience method.
     */
    public function displayFeedback (bundle :String, message :String) :void
    {
        _chatdir.displayFeedback(bundle, message);
    }

    /**
     * Convenience method.
     */
    public function displayInfo (bundle :String, message :String) :void
    {
        _chatdir.displayInfo(bundle, message);
    }

    // documentation inherited from superinterface PresentsContext
    public function getClient () :Client
    {
        return _client;
    }

    // documentation inherited from superinterface PresentsContext
    public function getDObjectManager () :DObjectManager
    {
        return _client.getDObjectManager();
    }

    // documentation inherited from superinterface CrowdContext
    public function getLocationDirector () :LocationDirector
    {
        return _locdir;
    }

    // documentation inherited from superinterface CrowdContext
    public function getOccupantDirector () :OccupantDirector
    {
        return null; // TODO
    }

    // documentation inherited from superinterface CrowdContext
    public function getChatDirector () :ChatDirector
    {
        return _chatdir;
    }

    // documentation inherited from superinterface WhirledContext
    public function getSceneDirector () :SceneDirector
    {
        return _scenedir;
    }

    /**
     * Get the SpotSceneDirector.
     */
    public function getSpotSceneDirector () :SpotSceneDirector
    {
        return _spotdir;
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

    protected var _client :Client;

    protected var _app :Application;

    protected var _topPanel :TopPanel;

    protected var _msgmgr :MessageManager;

    protected var _locdir :LocationDirector;

    protected var _scenedir :SceneDirector;

    protected var _chatdir :ChatDirector;

    protected var _spotdir :SpotSceneDirector;
}
}
