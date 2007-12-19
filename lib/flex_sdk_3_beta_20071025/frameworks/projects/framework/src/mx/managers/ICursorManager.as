////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.managers
{

[ExcludeClass]

import mx.core.IUIComponent;

/**
 *  @private
 */
public interface ICursorManager
{
	function get currentCursorID():int;
	function set currentCursorID(value:int):void;
    function get currentCursorXOffset():Number
	function set currentCursorXOffset(value:Number):void;
    function get currentCursorYOffset():Number
	function set currentCursorYOffset(value:Number):void;

	function showCursor():void;
	function hideCursor():void;
	function setCursor(cursorClass:Class, priority:int = 2,
			xOffset:Number = 0, yOffset:Number = 0, setter:IUIComponent = null):int;
	function removeCursor(cursorID:int):void;
	function removeAllCursors():void;
	function setBusyCursor():void;
	function removeBusyCursor():void; 

	function registerToUseBusyCursor(source:Object):void;
	function unRegisterToUseBusyCursor(source:Object):void;
}

}

