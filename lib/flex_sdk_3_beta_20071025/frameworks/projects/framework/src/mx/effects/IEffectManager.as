////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.effects
{

import mx.core.IUIComponent;
import mx.events.EffectEvent;

[ExcludeClass]

public interface IEffectManager
{
//  var lastEffectCreated:IEffect;

//  var effectsPlaying:Array /* of EffectNode */;

//  function getEventForEffectTrigger(effectTrigger:String):String;

//  function setStyle(styleProp:String, target:*):void;

	function suspendEventHandling():void;

	function resumeEventHandling():void;

//  function startBitmapEffect(target:IUIComponent):void;

//  function endBitmapEffect(target:IUIComponent):void

//  function startVectorEffect(target:IUIComponent):void

//  function endVectorEffect(target:IUIComponent):void

	function endEffectsForTarget(target:IUIComponent):void;

//  function effectStarted(effectInstance:IEffectInstance):void;

//  function effectFinished(effectInstance:IEffectInstance):void;

//  function eventHandler(eventObj:Event):void

//  function effectEndHandler(event:EffectEvent):void;
}

}
