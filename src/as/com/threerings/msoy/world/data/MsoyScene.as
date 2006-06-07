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
     * Get the background media.
     */
    public function getBackground () :MediaData
    {
        return _msoyModel.background;
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

    public function getFurni () :Array
    {
        return _msoyModel.furnis;
    }

    // documentation inherited from interface SpotScene
    public function addPortal (portal :Portal) :void
    {
        _sdelegate.addPortal(portal);
    }

    // documentation inherited from interface SpotScene
    public function getDefaultEntrance () :Portal
    {
        return _sdelegate.getDefaultEntrance();
    }

    // documentation inherited from interface SpotScene
    public function getNextPortalId () :int
    {
        return _sdelegate.getNextPortalId();
    }

    // documentation inherited from interface SpotScene
    public function getPortal (portalId :int) :Portal
    {
        return _sdelegate.getPortal(portalId);
    }

    // documentation inherited from interface SpotScene
    public function getPortalCount () :int
    {
        return _sdelegate.getPortalCount();
    }

    // documentation inherited from interface SpotScene
    public function getPortals () :Iterator
    {
        return _sdelegate.getPortals();
    }

    // documentation inherited from interface SpotScene
    public function removePortal (portal :Portal) :void
    {
        _sdelegate.removePortal(portal);
    }

    // documentation inherited from interface SpotScene
    public function setDefaultEntrance (portal :Portal) :void
    {
        _sdelegate.setDefaultEntrance(portal);
    }

    // documentation inherited from interface Cloneable
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
