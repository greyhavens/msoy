////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2005 Macromedia, Inc. All Rights Reserved.
//  The following is Sample Code and is subject to all restrictions
//  on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package haloclassic
{

import flash.display.Graphics;
import mx.core.SpriteAsset;

/**
 *  The skin for the separator between column headers in a DataGrid.
 */
public class DataGridHeaderSeparator extends SpriteAsset
{
	include "../mx/core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function DataGridHeaderSeparator()
	{
		super();
	}
	
	//--------------------------------------------------------------------------
	//
	//  Overridden properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  measuredWidth
	//----------------------------------
	
	/**
	 *  @private
	 */
	override public function get measuredWidth():Number
	{
		return 2;
	}
	
	//----------------------------------
	//  measuredHeight
	//----------------------------------

	/**
	 *  @private
	 */
	override public function get measuredHeight():Number
	{
		return 10;
	}
	
	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override public function setActualSize(newWidth:Number,
										   newHeight:Number):void
	{
		var g:Graphics = graphics;
		
		g.clear();
		
		// Clear rect for hit area
		g.beginFill(0x000000, 0);
		g.drawRect(-2, 0, newWidth + 2, newHeight);
		g.endFill();
		
		// Highlight
		g.lineStyle(1, 0xFFFFFF);
		g.moveTo(1, 0);
		g.lineTo(1, newHeight);
		g.lineStyle(1, 0x919999);
		g.moveTo(2, 0);
		g.lineTo(2, newHeight);
	}
}

}
