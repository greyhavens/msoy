package com.threerings.msoy.ui {

import flash.events.Event;

import mx.core.IUIComponent;
import mx.core.mx_internal;

import mx.effects.EffectManager;
import mx.effects.effectClasses.TweenEffectInstance;

import mx.events.MoveEvent;

import com.threerings.msoy.world.data.MsoyLocation;

import com.threerings.msoy.client.RoomView; // TODO: same package

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

        tween = mx_internal::createTween(this, [ src.x, src.y, src.z ],
            [ dest.x, dest.y, dest.z ], duration);

        // TODO: clipping and edgemetrics stuff omitted?

        onTweenUpdate(tween.mx_internal::getCurrentValue(0));

        // TODO: style stuff omitted?
    }

    // documentation inherited
    override public function onTweenUpdate (value :Object) :void
    {
        // we don't use IUIComponent.move() on our target, so we
        // do not need to suspend EffectManager event handling.

        var coords :Array = (value as Array);
        var parent :RoomView = (target.parent as RoomView);
        parent.setLocation(IUIComponent(target), coords);
    }

    // documentation inherited
    override public function onTweenEnd (value :Object) :void
    {
        EffectManager.mx_internal::endVectorEffect(IUIComponent(target));

        // TODO: style, clipping omitted?

        super.onTweenEnd(value);
    }
}
}
