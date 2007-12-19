////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.containers
{

import mx.core.mx_internal;

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="direction", kind="property")]

[Exclude(name="focusIn", kind="event")]
[Exclude(name="focusOut", kind="event")]

[Exclude(name="focusBlendMode", kind="style")]
[Exclude(name="focusSkin", kind="style")]
[Exclude(name="focusThickness", kind="style")]

[Exclude(name="focusInEffect", kind="effect")]
[Exclude(name="focusOutEffect", kind="effect")]

//--------------------------------------
//  Other metadata
//--------------------------------------

[IconFile("HDividedBox.png")]

/**
 *  The HDividedBox container lays out its children horizontally
 *  in the same way as HBox container, but it inserts
 *  a draggable divider in the gap between each child.
 *  A user can drag the divider to resize the area allotted to each child.
 *  The <code>&lt;mx:HDividedBox/&gt;</code> tag is the same as
 *  <code>&lt;mx:DividedBox direction="horizontal"/&gt;</code>.
 *  
 *  @mxml
 *  
 *  <p>The <code>&lt;mx:HDividedBox&gt;</code> tag inherits all of the tag 
 *  attributes of its superclass except <code>direction</code> and adds 
 *  no new tag attributes.</p>
 *  
 *  @includeExample examples/HDividedBoxExample.mxml
 *
 *  @see mx.containers.DividedBox
 *  @see mx.containers.VDividedBox
 */
public class HDividedBox extends DividedBox
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function HDividedBox()
	{
		super();
				
		mx_internal::layoutObject.direction = BoxDirection.HORIZONTAL;
	}

	//--------------------------------------------------------------------------
	//
	//  Overridden properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  direction
	//----------------------------------
	
	[Inspectable(environment="none")]	

	/**
	 *  @private
	 *  Don't allow user to change the direction
	 */
	override public function set direction(value:String):void
	{
	}
}

}
