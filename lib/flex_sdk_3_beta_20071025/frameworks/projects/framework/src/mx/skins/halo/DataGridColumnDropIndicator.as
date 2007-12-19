////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.skins.halo
{

import flash.display.Graphics;
import mx.skins.ProgrammaticSkin;
import mx.utils.ColorUtil;

/**
 *  The skin for the column drop indicator in a DataGrid.
 */
public class DataGridColumnDropIndicator extends ProgrammaticSkin
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
	public function DataGridColumnDropIndicator()
	{
		super();
	}
	
	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override protected function updateDisplayList(w:Number, h:Number):void
	{	
		super.updateDisplayList(w, h);

		var g:Graphics = graphics;
		
		g.clear();

		g.lineStyle(1, getStyle("rollOverColor"));
		g.moveTo(0, 0);
		g.lineTo(0, h);

		g.lineStyle(1, ColorUtil.adjustBrightness(getStyle("themeColor"), -75));
		g.moveTo(1, 0);
		g.lineTo(1, h);

		g.lineStyle(1, getStyle("rollOverColor"));
		g.moveTo(2, 0);
		g.lineTo(2, h);
	}
}

}
