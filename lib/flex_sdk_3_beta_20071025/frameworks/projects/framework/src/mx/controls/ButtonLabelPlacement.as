////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls
{

/**
 *  The ButtonLabelPlacement class defines the constants for the allowed values 
 *  of the <code>labelPlacement</code>
 *  property of a Button, CheckBox, LinkButton, or RadioButton control.
 *
 *  @see mx.controls.Button
 *  @see mx.controls.CheckBox
 *  @see mx.controls.LinkButton
 *  @see mx.controls.RadioButton
 */
public final class ButtonLabelPlacement
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

	/**
	 *  Specifies that the label appears below the icon.
	 */
	public static const BOTTOM:String = "bottom";
	
	/**
	 *  Specifies that the label appears to the left of the icon.
	 */
	public static const LEFT:String = "left";
	
	/**
	 *  Specifies that the label appears to the right of the icon.
	 */
	public static const RIGHT:String = "right";
	
	/**
	 *  Specifies that the label appears above the icon.
	 */
	public static const TOP:String = "top";
}

}
