package com.threerings.msoy.world.client {

import flash.events.Event;

import mx.core.IUIComponent;
import mx.core.mx_internal;

import mx.effects.EffectManager;
import mx.effects.effectClasses.TweenEffectInstance;

import mx.events.MoveEvent;

import com.threerings.msoy.world.data.MsoyLocation;

public class SceneMoveInstance extends TweenEffectInstance
{
    public function SceneMoveInstance (target :Object)
    {
        super(target);
    }

    public var src :MsoyLocation;
    public var dest :MsoyLocation;

    // documentation inherited
    override public function play () :void
    {
        super.play();

        EffectManager.mx_internal::startVectorEffect(IUIComponent(target));

        tween = createTween(
            this,
            [ src.x, src.y, src.z, src.orient ],
            [ dest.x, dest.y, dest.z, dest.orient ],
            duration);

        // TODO: clipping and edgemetrics stuff omitted?

        // immediately update with the first coords
        onTweenUpdate(tween.mx_internal::getCurrentValue(0));

        // TODO: style stuff omitted?
    }

    // documentation inherited
    override public function onTweenUpdate (value :Object) :void
    {
        // we don't use IUIComponent.move() on our target, so we
        // do not need to suspend EffectManager event handling.

        if (SceneMove(effect).isValid()) {
            MsoySprite(target).setLocation(value);
        }
    }

    // documentation inherited
    override public function onTweenEnd (value :Object) :void
    {
        EffectManager.mx_internal::endVectorEffect(IUIComponent(target));

        // TODO: style, clipping omitted?

        // will update with current coords
        super.onTweenEnd(value);

        if (SceneMove(effect).isValid()) {
            MsoySprite(target).moveCompleted(value[3]);
        }
    }
}
}
