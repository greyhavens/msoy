//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;
import flash.display.Sprite;

import com.threerings.crowd.data.OccupantInfo;

import com.threerings.msoy.avrg.client.AVRGameBackend;
import com.threerings.msoy.world.client.WorldContext;

import com.threerings.msoy.room.data.MobInfo;

import com.threerings.util.Log;

/**
 * Displays a MOB in the world.
 */
public class MobSprite extends OccupantSprite
{
    public function MobSprite (
        ctx :WorldContext, occInfo :MobInfo, extraInfo :Object, backend :AVRGameBackend)
    {
        super(ctx, occInfo, extraInfo);

        _holder = new Sprite();
        setMediaObject(_holder);

        _hostBackend = backend;
        updateMobVisual();
    }

    /** Return this MOB's unique identifier. */
    public function getMobIdent () :String
    {
        return MobInfo(_occInfo).getIdent();
    }

    /** Return the identifier of the game for this MOB. */
    public function getMobGameId () :int
    {
        return MobInfo(_occInfo).getGameId();
    }

    /** Called when this MOB is removed from the room. */
    public function removed () :void
    {
        if (_hostBackend) {
            _hostBackend.mobRemoved(getMobIdent())
        }
    }

    // from MsoySprite
    override public function capturesMouse () :Boolean
    {
        // MOB clicks are exclusively the business of the AVRG/MOB code
        return true;
    }

    // from OccupantSprite
    override protected function configureDisplay (
        oldInfo :OccupantInfo, newInfo :OccupantInfo) :Boolean
    {
        Log.getLog(this).debug(
            "configureDisplay [newInfo=" + newInfo + ", oldInfo=" + oldInfo + "]");

        if (_hostBackend == null) {
            // if we're not playing, we don't care
            return false;
        }

        var ominfo :MobInfo = (oldInfo as MobInfo), nminfo :MobInfo = (newInfo as MobInfo);
        if (ominfo == null || ominfo.getGameId() != nminfo.getGameId() ||
            ominfo.getIdent() != nminfo.getIdent()) {
            updateMobVisual();
            return true;
        }
        return false;
    }

    // from OccupantSprite
    override protected function appearanceChanged () :void
    {
        if (getMobIdent() != null && _hostBackend != null) {
            var locArray :Array = [ _loc.x, _loc.y, _loc.z ];
            _hostBackend.mobAppearanceChanged(
                getMobIdent(), locArray, _loc.orient, isMoving(), isIdle());
        }
    }

    // refresh the visualization of this MOB
    protected function updateMobVisual () :void
    {
        // clear out the holder (should only ever be 1 child, but better safe than sorry)
        while (_holder.numChildren > 0) {
            _holder.removeChildAt(0);
        }
        // if we're playing the AVRG, request an actual sprite
        if (_hostBackend) {
            var sprite :DisplayObject = _hostBackend.requestMobSprite(getMobIdent());
            Log.getLog(this).debug("updating [sprite=" + sprite + "]");
            if (sprite != null) {
                // then display it
                _holder.addChild(sprite);
            }
            return;
        }
        Log.getLog(MobSprite).debug("Ignoring MOB update, we're not playing...");
    }

    /** A container for the current visualization. */
    protected var _holder :Sprite;

    /** A pointer to our real backend; we don't have a real _backend of our own. */
    protected var _hostBackend :AVRGameBackend;
}
}
