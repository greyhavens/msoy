////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.managers
{

import flash.display.DisplayObject;

import mx.core.IToolTip;
import mx.core.IUIComponent;
import mx.effects.Effect;

[ExcludeClass]

/**
 *  @private
 */
public interface IToolTipManager
{
	function createToolTip(text:String, x:Number, y:Number,
			   errorTipBorderStyle:String = null,
			   context:IUIComponent = null):IToolTip;
	function destroyToolTip(toolTip:IToolTip):void;

	function get currentTarget():DisplayObject;
	function set currentTarget(value:DisplayObject):void;
	function get currentToolTip():IToolTip;
	function set currentToolTip(value:IToolTip):void;
	function get enabled():Boolean;
	function set enabled(value:Boolean):void;
	function get hideDelay():Number;
	function set hideDelay(value:Number):void;
	function get hideEffect():Effect;
	function set hideEffect(value:Effect):void;
	function get scrubDelay():Number;
	function set scrubDelay(value:Number):void;
	function get showDelay():Number;
	function set showDelay(value:Number):void;
	function get showEffect():Effect;
	function set showEffect(value:Effect):void;
	function get toolTipClass():Class;
	function set toolTipClass(value:Class):void;

	function registerToolTip(target:DisplayObject, toolTip:String):void;
	function registerErrorString(target:DisplayObject,
			errorString:String):void;
	function sizeTip(toolTip:IToolTip):void;
}

}

