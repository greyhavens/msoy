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

import mx.core.EdgeMetrics;
import mx.skins.Border;

/**
 *  The skin for all the states of a LinkButton.
 */
public class LinkButtonSkin extends Border
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
	public function LinkButtonSkin()
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
	override public function get borderMetrics():EdgeMetrics
	{		
		return EdgeMetrics.EMPTY;
	}

	/**
	 *  @private
	 */
	override protected function updateDisplayList(w:Number, h:Number):void
	{
		super.updateDisplayList(w, h);

		var cornerRadius:Number = getStyle("cornerRadius");
		var rollOverColor:uint = getStyle("rollOverColor");
		var selectionColor:uint = getStyle("selectionColor");

		graphics.clear();
														
		switch (name)
		{			
			case "upSkin":
			{
				// Draw invisible shape so we have a hit area.
				drawRoundRect(
					0, 0, w, h, cornerRadius,
					0x000000, 0);
				break;
			}
			
			case "overSkin":
			{
				drawRoundRect(
					0, 0, w, h, cornerRadius,
					rollOverColor, 1);
				break;
			}
			
			case "downSkin":
			{
				drawRoundRect(0, 0, w, h, cornerRadius,
					selectionColor, 1);
				break;
			}

			case "disabledSkin":
			{
				// Draw invisible shape so we have a hit area.
				drawRoundRect(
					0, 0, w, h, cornerRadius,
					0x000000, 0);
				break;
			}
		}
	}
}

}
