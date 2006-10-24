package com.threerings.msoy.world.data {

import flash.errors.IllegalOperationError;

import com.threerings.util.Iterator;
import com.threerings.util.Cloneable;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.data.MemberObject;

import com.threerings.whirled.data.Scene;
import com.threerings.whirled.data.SceneImpl;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotScene;

import com.threerings.msoy.item.web.MediaDesc;

public class MsoyScene extends SceneImpl
    implements SpotScene, Cloneable
{
    public function MsoyScene (model :MsoySceneModel, config :PlaceConfig)
    {
        super(model, config);
        _msoyModel = model;
    }

    /**
     * Can the specified member edit this scene?
     */
    public function canEdit (member :MemberObject) :Boolean
    {
        return member.getTokens().isAdmin() ||
            (!member.isGuest() && (getOwnerId() == member.getMemberId()));
    }

    /**
     * Returns the scene type.
     */
    public function getType () :int
    {
        return _msoyModel.type;
    }

    /**
     * Returns the owner id.
     */
    public function getOwnerId () :int
    {
        return _msoyModel.ownerId;
    }

    /**
     * Returns the "pixel" depth of the scene.
     */
    public function getDepth () :int
    {
        return _msoyModel.depth;
    }

    /**
     * Returns the pixel width of the scene.
     */
    public function getWidth () :int
    {
        return _msoyModel.width;
    }

    /**
     * Get the height of the horizon, expressed as a floating
     * point number between 0 and 1. (1 == horizon at top of screen)
     */
    public function getHorizon () :Number
    {
        return _msoyModel.horizon;
    }

    /**
     * Retrieve the first background furni data that specifies music
     */
    public function getMusic () :MediaDesc
    {
        for each (var furni :FurniData in getFurni()) {
            if (furni.actionType == FurniData.BACKGROUND &&
                    furni.media.isAudio()) {
                return furni.media;
            }
        }
        return null;
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

    /**
     * Get the next available furniture id.
     */
    public function getNextFurniId () :int
    {
        return _msoyModel.getNextFurniId();
    }

    // from SpotScene
    public function addPortal (portal :Portal) :void
    {
        throw new IllegalOperationError();
    }

    // from SpotScene
    public function getDefaultEntrance () :Portal
    {
        return _msoyModel.getDefaultEntrance();
    }

    // from SpotScene
    public function getNextPortalId () :int
    {
        // since portals are just furni...
        return _msoyModel.getNextFurniId();
    }

    // from SpotScene
    public function getPortal (portalId :int) :Portal
    {
        return _msoyModel.getPortal(portalId);
    }

    // from SpotScene
    public function getPortalCount () :int
    {
        return _msoyModel.getPortalCount();
    }

    // from SpotScene
    public function getPortals () :Iterator
    {
        return _msoyModel.getPortals();
    }

    // from SpotScene
    public function removePortal (portal :Portal) :void
    {
        throw new IllegalOperationError();
    }

    // from SpotScene
    public function setDefaultEntrance (portal :Portal) :void
    {
        throw new IllegalOperationError();
    }

    // from Cloneable
    public function clone () :Object
    {
        return new MsoyScene(_msoyModel.clone() as MsoySceneModel, _config);
    }

    /** A reference to our scene model. */
    protected var _msoyModel :MsoySceneModel;
}
}
