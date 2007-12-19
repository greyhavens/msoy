////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.effects
{

import flash.events.Event;
import mx.effects.effectClasses.PropertyChanges;

/**
 *  @private
 */
public interface IEffectInstance
{
	function get className():String;
	function get duration():Number;
	function set duration(value:Number):void;
	function get effect():Effect;;
	function set effect(value:Effect):void;
	function get playheadTime():Number ;
	function get propertyChanges():PropertyChanges;
	function set propertyChanges(value:PropertyChanges):void;
	function get repeatCount():int;
	function set repeatCount(value:int):void;
	function get repeatDelay():int;
	function set repeatDelay(value:int):void;
	function get startDelay():int;
	function set startDelay(value:int):void;
	function get suspendBackgroundProcessing():Boolean;
	function set suspendBackgroundProcessing(value:Boolean):void;
	function get target():Object;
	function set target(value:Object):void;
	function get triggerEvent():Event;
	function set triggerEvent(value:Event):void;

	function initEffect(event:Event):void;
	function startEffect():void;
	function play():void;
	function pause():void;
	function resume():void;
	function reverse():void;
	function end():void;
	function finishEffect():void;
	function finishRepeat():void;
}

}

