////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.dataGridClasses
{

import flash.display.DisplayObject;
import mx.controls.DataGrid;
import mx.controls.listClasses.IDropInListItemRenderer;
import mx.controls.listClasses.IListItemRenderer;
import mx.core.UIComponent;
import mx.core.mx_internal;

use namespace mx_internal;

/**
 *  The DataGridDragProxy class defines the default drag proxy 
 *  used when dragging data from a DataGrid control.
 */
public class DataGridDragProxy extends UIComponent
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
	public function DataGridDragProxy()
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
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override protected function createChildren():void
	{
        super.createChildren();
        
		var items:Array /* of unit */ = DataGrid(owner).selectedItems;

		var n:int = items.length;
		for (var i:int = 0; i < n; i++)
		{
			var src:IListItemRenderer = DataGrid(owner).itemToItemRenderer(items[i]);
			if (!src)
				continue;

			var o:UIComponent;
			
			var data:Object = items[i];
			
			o = new UIComponent();
			addChild(DisplayObject(o));
			
			var ww:Number = 0;
			
			var m:int = DataGrid(owner).visibleColumns.length;
			for (var j:int = 0; j < m; j++)
			{
				var col:DataGridColumn = DataGrid(owner).visibleColumns[j];
				
				var c:IListItemRenderer = DataGrid(owner).columnItemRenderer(col, false);
				
				var	rowData:DataGridListData = new DataGridListData(
					col.itemToLabel(data), col.dataField,
					col.colNum, "", DataGrid(owner));
				
				if (c is IDropInListItemRenderer)
				{
					IDropInListItemRenderer(c).listData =
						data ? rowData : null;
				}
				
				c.data = data;
				c.styleName = DataGrid(owner);
				
				o.addChild(DisplayObject(c));
				
				c.setActualSize(col.width, src.height);
				c.move(ww, 0);
				
				ww += col.width;
			}

			o.setActualSize(ww, src.height);
			o.y = src.y;

			measuredHeight = o.y + o.height;
			measuredWidth = ww;
		}

		invalidateDisplayList();
	}
}

}
