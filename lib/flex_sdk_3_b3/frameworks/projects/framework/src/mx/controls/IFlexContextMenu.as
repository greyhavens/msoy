////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls
{

/**
 *  The IFlexContextMenu interface defines the interface for a 
 *  Flex context menus.  
 *
 *  @see mx.core.UIComponent#flexContextMenu
 */
public interface IFlexContextMenu
{
	import flash.display.InteractiveObject;

	/**
	 *  Sets the context menu of an InteractiveObject.  This will do 
	 *  all the necessary steps to add ourselves as the context 
	 *  menu for this InteractiveObject, such as adding listeners, etc..
	 * 
	 *  @param component InteractiveObject to set context menu on
	 */ 
	function setContextMenu(component:InteractiveObject):void;
	
	/**
	 *  Unsets the context menu of a InteractiveObject.  This will do 
	 *  all the necessary steps to remove ourselves as the context 
	 *  menu for this InteractiveObject, such as removing listeners, etc..
	 * 
	 *  @param component InteractiveObject to unset context menu on
	 */ 
	function unsetContextMenu(component:InteractiveObject):void;

}

}
