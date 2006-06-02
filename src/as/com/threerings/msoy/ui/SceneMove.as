package com.threerings.msoy.ui {

import mx.effects.EffectInstance;
import mx.effects.TweenEffect;
import mx.effects.easing.Linear;

import com.threerings.msoy.world.data.MsoyLocation;

/**
 * An effect that moves a component around in a RoomView using 3d locations.
 */
public class SceneMove extends TweenEffect
{
    /** The properties affected by this effect. */
    private static const AFFECTED_PROPERTIES :Array = [ "x", "y", "zoom" ];

    /**
     * Class constructor
     */
    public function SceneMove (target :Object = null)
    {
        super(target);
        instanceClass = SceneMoveInstance;

        // set up standard bits
        easingFunction = Linear.easeNone; // set up a linear easing
    }

    /** Our source location. */
    public var src :MsoyLocation;

    /** Our destination location. */
    public var dest :MsoyLocation;

    // documentation inherited
    override public function getAffectedProperties () :Array
    {
        return AFFECTED_PROPERTIES;
    }

    // documentation inherited
    override protected function initInstance (instance :EffectInstance) :void
    {
        duration = 60 * src.distance(dest); // TODO

        super.initInstance(instance);

        var i :SceneMoveInstance = (instance as SceneMoveInstance);
        i.src = src;
        i.dest = dest;
    }
}
}
