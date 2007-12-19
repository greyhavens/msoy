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
import mx.core.IFlexDisplayObject;

[ExcludeClass]

/**
 *  @private
 */
public interface IPopUpManager
{
	function createPopUp(parent:DisplayObject,
			className:Class,
			modal:Boolean = false,
			childList:String = null):IFlexDisplayObject;
	function addPopUp(window:IFlexDisplayObject,
			parent:DisplayObject,
			modal:Boolean = false,
			childList:String = null):void;
	function centerPopUp(popUp:IFlexDisplayObject):void;
	function removePopUp(popUp:IFlexDisplayObject):void;
	function bringToFront(popUp:IFlexDisplayObject):void;
}

}

