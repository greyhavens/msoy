//
// $Id$

package com.threerings.msoy.room.data {

import flash.errors.IllegalOperationError;

import com.threerings.util.Iterator;
import com.threerings.util.Cloneable;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.msoy.data.MemberObject;

import com.threerings.whirled.data.SceneImpl;

import com.threerings.whirled.spot.data.Portal;
import com.threerings.whirled.spot.data.SpotScene;

import com.threerings.msoy.item.data.all.Decor;

public class MsoyScene extends SceneImpl
    implements SpotScene, Cloneable
{
    public function MsoyScene (model :MsoySceneModel, config :PlaceConfig)
    {
        super(model, config);
        _msoyModel = model;
    }

    /**
     * Does the specified member have management rights in this room?
     */
    public function canManage (member :MemberObject, where :String = null) :Boolean
    {
        var hasRights :Boolean;
        switch (_msoyModel.ownerType) {
        case MsoySceneModel.OWNER_TYPE_MEMBER:
            hasRights = (_msoyModel.ownerId == member.getMemberId());
            break;

        case MsoySceneModel.OWNER_TYPE_GROUP:
            hasRights = member.isGroupManager(_msoyModel.ownerId);
            break;

        default:
            hasRights = false;
            break;
        }

        if (!hasRights && member.tokens.isSupport()) {
            // no need to log anything here on the client, just grant the access
            return true;
        }

        return hasRights;
    }

    /**
     * Returns the scene type.
     */
    public function getSceneType () :int
    {
        return _msoyModel.decor.type;
    }

    /**
     * Returns the "pixel" depth of the scene.
     */
    public function getDepth () :int
    {
        return _msoyModel.decor.depth;
    }

    /**
     * Returns the pixel width of the scene.
     */
    public function getWidth () :int
    {
        return _msoyModel.decor.width;
    }

    public function getHeight () :int
    {
        return _msoyModel.decor.height;
    }

    /**
     * Get the height of the horizon, expressed as a floating
     * point number between 0 and 1. (1 == horizon at top of screen)
     */
    public function getHorizon () :Number
    {
        return _msoyModel.decor.horizon;
    }

    /**
     * Retrieve the room entrance.
     */
    public function getEntrance () :MsoyLocation
    {
        return _msoyModel.entrance;
    }
    
    /**
     * Retrieve the room decor.
     */
    public function getDecor () :Decor
    {
        return _msoyModel.decor;
    }

    /**
     * Retrieve the scene background audio.
     */
    public function getAudioData () :AudioData
    {
        return _msoyModel.audioData;
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
    public function getNextFurniId (aboveId :int) :int
    {
        return _msoyModel.getNextFurniId(aboveId);
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
        throw new IllegalOperationError();
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
