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

/**
 *  The IFocusManagerComplexComponent interface defines the interface 
 *  that components that can have more than one internal focus target
 *  should implement in order to
 *  receive focus from the FocusManager.
 */
public interface IFocusManagerComplexComponent extends IFocusManagerComponent
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  hasFocusableContent
	//----------------------------------

	/**
	 *  A flag that indicates whether the component currently has internal
	 *  focusable targets
	 * 
	 */
	function get hasFocusableContent():Boolean;
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  Called by the FocusManager when the component receives focus.
	 *  The component may in turn set focus to an internal component.
	 *  The components setFocus() method will still be called when focused by
	 *  the mouse, but this method will be used when focus changes via the
	 *  keyboard
	 *
	 *  @param direction "bottom" if TAB used with SHIFT key, "top" otherwise
	 */
	function assignFocus(direction:String):void;

}

}
