package com.threerings.msoy.world.data {

import com.threerings.util.Iterator;
import com.threerings.util.Cloneable;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneImpl;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotScene;
import com.threerings.whirled.spot.data.SpotSceneImpl;
import com.threerings.whirled.spot.data.SpotSceneModel;

import com.threerings.msoy.data.MediaData;

public class MsoyScene extends SceneImpl
    implements SpotScene, Cloneable
{
    public function MsoyScene (model :MsoySceneModel, config :PlaceConfig)
    {
        super(model, config);
        _msoyModel = model;
        _sdelegate = new SpotSceneImpl(SpotSceneModel.getSceneModel(model));
    }

    /**
     * Returns the scene type.
     */
    public function getType () :String
    {
        return _msoyModel.type;
    }

    /**
     * Returns the pixel width of the scene.
     */
    public function getWidth () :int
    {
        return _msoyModel.width;
    }

    /**
     * Get the background media.
     */
    public function getBackground () :MediaData
    {
        return _msoyModel.background;
    }

    /**
     * Get the background music.
     */
    public function getMusic () :MediaData
    {
        return _msoyModel.music;
    }

    /**
     * Add a new piece of furniture to this scene.
     */
    public function addFurni (furn :FurniData) :void
    {
        _msoyModel.addFurni(furn);
    }

    /**
     * Remove a piece of furniture from this scene.
     */
    public function removeFurni (furn :FurniData) :void
    {
        _msoyModel.removeFurni(furn);
    }

    /**
     * Get all the furniture currently in the scene.
     */
    public function getFurni () :Array
    {
        return _msoyModel.furnis;
    }

    // from SpotScene
    public function addPortal (portal :Portal) :void
    {
        _sdelegate.addPortal(portal);
    }

    // from SpotScene
    public function getDefaultEntrance () :Portal
    {
        return _sdelegate.getDefaultEntrance();
    }

    // from SpotScene
    public function getNextPortalId () :int
    {
        return _sdelegate.getNextPortalId();
    }

    // from SpotScene
    public function getPortal (portalId :int) :Portal
    {
        return _sdelegate.getPortal(portalId);
    }

    // from SpotScene
    public function getPortalCount () :int
    {
        return _sdelegate.getPortalCount();
    }

    // from SpotScene
    public function getPortals () :Iterator
    {
        return _sdelegate.getPortals();
    }

    // from SpotScene
    public function removePortal (portal :Portal) :void
    {
        _sdelegate.removePortal(portal);
    }

    // from SpotScene
    public function setDefaultEntrance (portal :Portal) :void
    {
        _sdelegate.setDefaultEntrance(portal);
    }

    override public function updateReceived (update :SceneUpdate) :void
    {
        super.updateReceived(update);

        // inform our spot delegate of possible changes
        _sdelegate.updateReceived();
    }

    // from Cloneable
    public function clone () :Object
    {
        return new MsoyScene(_msoyModel.clone() as MsoySceneModel, _config);
    }

    /** A reference to our scene model. */
    protected var _msoyModel :MsoySceneModel;

    /** Our spot scene delegate. */
    protected var _sdelegate :SpotSceneImpl;
}
}
