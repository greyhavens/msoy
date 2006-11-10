package com.threerings.msoy.world.client {

import mx.effects.EffectInstance;
import mx.effects.TweenEffect;
import mx.effects.easing.Linear;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;

/**
 * An effect that moves a component around in a RoomView using 3d locations.
 */
public class SceneMove extends TweenEffect
{
    /** The properties affected by this effect. */
    private static const AFFECTED_PROPERTIES :Array = [ "x", "y" ];

    /** Our source location. */
    public var src :MsoyLocation;

    /** Our destination location. */
    public var dest :MsoyLocation;

    /**
     * Accessor for valid property, used by our instances.
     */
    public function isValid () :Boolean
    {
        return _valid;
    }

    /**
     * Class constructor
     */
    public function SceneMove (
        target :MsoySprite, scene :MsoyScene, src :MsoyLocation,
        dest :MsoyLocation)
    {
        super(target);
        instanceClass = SceneMoveInstance;

        // set up standard bits
        easingFunction = Linear.easeNone; // set up a linear easing

        this.src = src;
        this.dest = dest;

        // TODO move/define magic numbers?
        var dx :Number = scene.getWidth() * (dest.x - src.x);
        var dy :Number = 400 * (dest.y - src.y);
        var dz :Number = scene.getDepth() * (dest.z - src.z);
        this.duration = 2 * Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    /**
     * Cancel the move, without jumping to the end.
     */
    public function cancel () :void
    {
        _valid = false;
        end();
    }

    // documentation inherited
    override public function getAffectedProperties () :Array
    {
        return AFFECTED_PROPERTIES;
    }

    // documentation inherited
    override protected function initInstance (instance :EffectInstance) :void
    {
        super.initInstance(instance);

        var i :SceneMoveInstance = (instance as SceneMoveInstance);
        i.src = src;
        i.dest = dest;
    }

    /** Whether we've been cancelled or not. */
    protected var _valid :Boolean = true;
}
}
