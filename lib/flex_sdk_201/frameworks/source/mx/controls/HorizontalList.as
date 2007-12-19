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

import mx.controls.listClasses.TileBase;
import mx.controls.listClasses.TileBaseDirection;
import mx.core.mx_internal;
import mx.core.ScrollPolicy;

use namespace mx_internal;

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="direction", kind="property")]
[Exclude(name="maxColumns", kind="property")]
[Exclude(name="maxHorizontalScrollPosition", kind="property")]
[Exclude(name="maxRows", kind="property")]
[Exclude(name="maxVerticalScrollPosition", kind="property")]
[Exclude(name="variableRowHeight", kind="property")]

//--------------------------------------
//  Other metadata
//--------------------------------------

[DefaultBindingProperty(source="selectedItem", destination="dataProvider")]

[DefaultProperty("dataProvider")]

[DefaultTriggerEvent("change")]

[IconFile("HorizontalList.png")]

/**
 *  The HorizontalList control displays a horizontal list of items. 
 *  If there are more items than can be displayed at once, it
 *  can display a horizontal scroll bar so the user can access
 *  all items in the list.
 *
 *  @mxml
 *
 *  <p>The <code>&lt;mx:HorizontalList&gt;</code> tag inherits all of the 
 *  tag attributes of its superclass and it adds no new tag attributes.</p>
 *
 *  <pre>
 *  &lt;mx:HorizontalList/&gt
 *  </pre>
 *
 *  @includeExample examples/HorizontalListExample.mxml
 */
public class HorizontalList extends TileBase
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
	public function HorizontalList()
	{
		super();

		_horizontalScrollPolicy = ScrollPolicy.AUTO;
		_verticalScrollPolicy = ScrollPolicy.OFF;

		direction = TileBaseDirection.VERTICAL;
		maxRows = 1;
		defaultRowCount = 1;
	}
}

}
