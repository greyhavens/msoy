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

import mx.core.IFlexDisplayObject;

/**
 *  Components that implement IToolTipManagerClient can have tooltips and must 
 *  have a toolTip getter/setter.
 *  The ToolTipManager class manages showing and hiding the 
 *  tooltip on behalf of any component which is an IToolTipManagerClient.
 * 
 *  @see mx.controls.ToolTip
 *  @see mx.managers.ToolTipManager
 *  @see mx.core.IToolTip
 */
public interface IToolTipManagerClient extends IFlexDisplayObject
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  toolTip
	//----------------------------------

	/**
	 *  The text of this component's tooltip.
	 */
	function get toolTip():String;
	
	/**
	 *  @private
	 */
	function set toolTip(value:String):void;

}

}
