//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Sprite;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.game.client.AVRGameBackend;

import com.threerings.msoy.world.data.MobInfo;

import com.threerings.util.Log;

/**
 * Displays a MOB in the world.
 */
public class MobSprite extends OccupantSprite
{
    public function MobSprite (occInfo :MobInfo)
    {
        super(occInfo);
    }

    /** Called when the AVRG related to this MOB is loaded or unloaded. */
    public function avrGameAvailable (backend :AVRGameBackend) :void
    {
        _hostBackend = backend;
        updateMobVisual();
    }

    /** Called when this MOB is removed from the room. */
    public function removed () :void
    {
        if (_hostBackend) {
            _hostBackend.mobRemoved(_id);
        }
    }

    // from OccupantSprite
    override protected function configureDisplay (
        oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        Log.getLog(this).debug("configureDisplay(" + newInfo + ")");

        _holder = new Sprite();
        setMediaObject(_holder);

        var ominfo :MobInfo = (oldInfo as MobInfo), nminfo :MobInfo = (newInfo as MobInfo);
        if (ominfo == null || ominfo.getIdent() != nminfo.getIdent()) {
            _id = nminfo.getIdent();
            updateMobVisual();
            return true;
        }
        return false;
    }

    // from OccupantSprite
    override protected function appearanceChanged () :void
    {
        if (_id != null && _hostBackend) {
            var locArray :Array = [ _loc.x, _loc.y, _loc.z ];
            _hostBackend.mobAppearanceChanged(_id, locArray, _loc.orient, isMoving(), isIdle());
        }
    }

    // refresh the visualization of this MOB
    protected function updateMobVisual () :void
    {
        // clear out the holder (should only ever be 1 child)
        while (_holder.numChildren > 0) {
            _holder.removeChildAt(0);
        }
        // if we're playing the AVRG, request an actual sprite
        if (_hostBackend) {
            var sprite :DisplayObject = _hostBackend.requestMobSprite(_id);
            Log.getLog(this).debug("updating [sprite=" + sprite + "]");
            if (sprite != null) {
                // then display it
                _holder.addChild(sprite);
            }
        }            
    }

    /** The opaque ID of the MOB, interpreted by the AVRG. */
    protected var _id :String;

    /** A container for the current visualization. */
    protected var _holder :Sprite;

    /** A pointer to our real backend; we don't have a real _backend of our own. */
    protected var _hostBackend :AVRGameBackend;
}
}
