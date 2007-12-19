////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.listClasses
{

import flash.display.DisplayObject;
import mx.core.mx_internal;
import mx.core.UIComponent;

use namespace mx_internal;

/**
 *  The default drag proxy used when dragging from a list-based control
 *  (except for the DataGrid class).
 *  A drag proxy is a component that parents the objects
 *  or copies of the objects being dragged
 *
 *  @see mx.controls.dataGridClasses.DataGridDragProxy
 */
public class ListItemDragProxy extends UIComponent
{
	include "../../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function ListItemDragProxy()
	{
		super();
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	//--------------------------------------------------------------------------
	//
	//  Overridden methods: UIComponent
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override protected function createChildren():void
	{
        super.createChildren();
        
		var items:Array /* of unit */ = ListBase(owner).selectedItems;

		var n:int = items.length;
		for (var i:int = 0; i < n; i++)
		{
			var src:IListItemRenderer = ListBase(owner).itemToItemRenderer(items[i]);
			if (!src)
				continue;

			var o:IListItemRenderer = ListBase(owner).itemRenderer.newInstance();
			
			o.styleName = ListBase(owner);
			
			if (o is IDropInListItemRenderer)
			{
				var listData:BaseListData =
					IDropInListItemRenderer(src).listData;
				
				IDropInListItemRenderer(o).listData = items[i] ?
													  listData :
													  null;
			}

			o.data = items[i];
			
			addChild(DisplayObject(o));
			
			o.setActualSize(src.width, src.height);
			o.x = src.x;
			o.y = src.y;

			measuredHeight = Math.max(measuredHeight, o.y + o.height);
			measuredWidth = Math.max(measuredWidth, o.x + o.width);
		}

		invalidateDisplayList();
	}
}

}
