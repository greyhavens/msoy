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

[IconFile("VBox.png")]

/**
 *  The VBox container lays out its children in a single vertical column.
 *  You use the <code>&lt;mx:VBox&gt;</code> tag instead of the
 *  <code>&lt;mx:Box&gt;</code> tag as a shortcut to avoid having to
 *  set the <code>direction</code> property to <code>"vertical"</code>.
 *  
 *  <p>An VBox container has the following default sizing characteristics:</p>
 *     <table class="innertable">
 *        <tr>
 *           <th>Characteristic</th>
 *           <th>Description</th>
 *        </tr>
 *        <tr>
 *           <td>Default size</td>
 *           <td>The height is large enough to hold all its children at the default or explicit height of the children, 
 *               plus any vertical gap between the children, plus the top and bottom padding of the container.
 *               The width is the default or explicit width of the widest child, plus the left and right padding of the container.
 *           </td>
 *        </tr>
 *        <tr>
 *           <td>Default padding</td>
 *           <td>0 pixels for the top, bottom, left, and right values.</td>
 *        </tr>
 *     </table>
 *
 *  @mxml
 *  
 *  <p>The <code>&lt;mx:VBox&gt;</code> tag inherits all of the tag 
 *  attributes of its superclass, except <code>direction</code>, and adds 
 *  no new tag attributes.</p></p>
 *  
 *  @includeExample examples/VBoxExample.mxml
 *
 *  @see mx.containers.Box
 *  @see mx.containers.HBox
 */
public class VBox extends Box
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
	public function VBox()
	{
		super();
		
		mx_internal::layoutObject.direction = BoxDirection.VERTICAL;
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
