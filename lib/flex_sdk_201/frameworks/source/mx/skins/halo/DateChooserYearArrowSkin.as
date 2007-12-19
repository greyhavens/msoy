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
import mx.skins.Border;
import mx.utils.ColorUtil;

/**
 *  The skin for all the states of the next-year and previous-year
 *  buttons in a DateChooser.
 */
public class DateChooserYearArrowSkin extends Border
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
	public function DateChooserYearArrowSkin()
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
		return 6;
	}
	
	//----------------------------------
	//  measuredHeight
	//----------------------------------

	/**
	 *  @private
	 */
	override public function get measuredHeight():Number
	{
		return 4;
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

		var themeColor:uint = getStyle("themeColor");
		
		var themeColorDrk1:Number =
			ColorUtil.adjustBrightness2(themeColor, -25);

		var arrowColor:uint;

		var g:Graphics = graphics;
	
		g.clear();
	
		switch (name)
		{
			case "prevYearUpSkin":
			case "nextYearUpSkin":
			{
				arrowColor = 0x111111;
				break;
			}

			case "prevYearOverSkin":
			case "nextYearOverSkin":
			{
				arrowColor = themeColor;
				break;
			}

			case "prevYearDownSkin":
			case "nextYearDownSkin":		
			{
				arrowColor = themeColorDrk1;
				break;
			}

			case "prevYearDisabledSkin":
			case "nextYearDisabledSkin":
			{
				arrowColor = 0x999999;
				break;
			}
		}
		
		// Viewable Button area				
		g.beginFill(arrowColor);
		if (name.charAt(0) == "p")
		{
			g.moveTo(w / 2, h / 2 + 2);
			g.lineTo(w / 2 - 3, h / 2 - 2);
			g.lineTo(w / 2 + 3, h / 2 - 2);
			g.lineTo(w / 2, h / 2 + 2);
		}
		else
		{								
			g.moveTo(w / 2, h / 2 - 2);
			g.lineTo(w / 2 - 3, h / 2 + 2);
			g.lineTo(w / 2 + 3, h / 2 + 2);
			g.lineTo(w / 2, h / 2 - 2);
		}
		g.endFill();				
	}
}

}
