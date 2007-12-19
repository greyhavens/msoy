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

/**
 *  The IFocusManagerGroup interface defines the interface that 
 *  any component must implement if it is grouped in sets,
 *  where only one member of the set can be selected at any given time.
 *  For example, a RadioButton implements IFocusManagerGroup
 *  because a set of RadioButtons in the same group 
 *  can only have one RadioButton selected at any one time,
 *  and the FocusManager will make sure not to give focus to the RadioButtons
 *  that are not selected in response to moving focus via the Tab key.
 */
public interface IFocusManagerGroup
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  groupName
	//----------------------------------

	/**
	 *	The name of the group of controls to which the control belongs.
	 */
	function get groupName():String;

	/**
	 *  @private
	 */
	function set groupName(value:String):void;

	//----------------------------------
	//  selected
	//----------------------------------

	/**
	 *	A flag that indicates whether this control is selected.
	 */
	function get selected():Boolean;

	/**
	 *  @private
	 */
	function set selected(value:Boolean):void;
}

}
