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
import mx.controls.listClasses.TileListItemRenderer;
import mx.core.ClassFactory;

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="maxHorizontalScrollPosition", kind="property")]
[Exclude(name="maxVerticalScrollPosition", kind="property")]

//--------------------------------------
//  Other metadata
//--------------------------------------

[DefaultBindingProperty(source="selectedItem", destination="dataProvider")]

[DefaultProperty("dataProvider")]

[DefaultTriggerEvent("change")]

[IconFile("TileList.png")]

/**
 *  The TileList control displays a number of items laid out in tiles.
 *  It displays a scroll bar on one of its axes to access all items
 *  in the list, depending on the <code>direction</code> property.
 *  You can set the size of the tiles by using the <code>rowHeight</code>
 *  or <code>columnWidth</code> properties.
 *  Alternatively, Flex measures the item renderer for the first item
 *  in the dataProvider and uses that size for all tiles.
 *  
 *  @mxml
 *  
 *  <p>The <code>&lt;mx:TileList&gt;</code> tag inherits
 *  all of the tag attributes of its superclass, but ignores the
 *  <code>variableRowHeight</code> and <code>wordWrap</code> tag attributes.  
 *  It adds no additional tag attributes.</p>
 *  
 *  <pre>
 *  &lt;mx:TileList/&gt;
 *  </pre>
 *
 *  @includeExample examples/TileListExample.mxml
 */
public class TileList extends TileBase
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
	public function TileList()
	{
		super();

		itemRenderer = new ClassFactory(TileListItemRenderer);
	}
}

}
