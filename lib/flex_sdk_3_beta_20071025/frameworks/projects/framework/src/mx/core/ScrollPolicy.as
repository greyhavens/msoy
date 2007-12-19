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

/**
 *  Values for the <code>horizontalScrollPolicy</code> and
 *  <code>verticalScrollPolicy</code> properties
 *  of the Container and ScrollControlBase classes.
 *
 *  @see mx.core.Container
 *  @see mx.core.ScrollControlBase
 */
public final class ScrollPolicy
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

	/**
	 *  Show the scrollbar if the children exceed the owner's dimension.
	 *  The size of the owner is not adjusted to account
	 *  for the scrollbars when they appear, so this may cause the
	 *  scrollbar to obscure the contents of the control or container.
	 */
	public static const AUTO:String = "auto";

	/**
	 *  Never show the scrollbar.
	 */
	public static const OFF:String = "off";
	
	/**
	 *  Always show the scrollbar.
	 *  The size of the scrollbar is automatically added to the size
	 *  of the owner's contents to determine the size of the owner
	 *  if explicit sizes are not specified.
	 */
	public static const ON:String = "on";
}

}
