////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

import flash.geom.Rectangle;

/**
 *  The IToolTip interface defines the API that tooltip-like components
 *  must implement in order to work with the ToolTipManager.
 *  The ToolTip class implements this interface.
 *
 *  @see mx.controls.ToolTip
 *  @see mx.managers.ToolTipManager
 */
public interface IToolTip extends IUIComponent
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  screen
	//----------------------------------

	/**
	 *  A Rectangle that specifies the size and position
	 *  of the base drawing surface for this tooltip.
	 */
	function get screen():Rectangle;

	//----------------------------------
	//  text
	//----------------------------------

	/**
	 *  The text that appears in the tooltip.
	 */
	function get text():String;
	
	/**
	 *  @private
	 */
	function set text(value:String):void;
}

}
