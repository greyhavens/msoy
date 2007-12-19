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
import mx.skins.Border;

/**
 *  Documentation is not currently available.
 *  @review
 */
public class DateChooserDownArrowSkin extends Border
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
	public function DateChooserDownArrowSkin()
	{
		super();
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private var hitLength:Number = 2;
	
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
		return 6 + 2 * hitLength;
	}
	
	//----------------------------------
	//  measuredHeight
	//----------------------------------

	/**
	 *  @private
	 */
	override public function get measuredHeight():Number
	{
		return 4 + 2 * hitLength;
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
	
		switch (name)
		{
			case "prevYearUpSkin":
			case "prevYearDownSkin":
			case "prevYearOverSkin":
			{
				// Invisible hit area
				g.beginFill(0x000000, 0.0); 
				g.moveTo(0, 0);
				g.lineTo(0, h);
				g.lineTo(w, h);
				g.lineTo(w, 0);
				g.lineTo(0, 0);				
				g.endFill();
				
				// Visible button area				
				g.beginFill(0x000000);
				g.moveTo(w / 2, h / 2 + 2);
				g.lineTo(w / 2 - 3, h / 2 - 2);
				g.lineTo(w / 2 + 3, h / 2 - 2);
				g.lineTo(w / 2, h / 2 + 2);
				g.endFill();
				break;
			}

			case "prevYearDisabledSkin":
			{
				g.beginFill(0x999999);
				g.moveTo(w / 2, h / 2 + 2);
				g.lineTo(w / 2 - 3, h / 2 - 2);
				g.lineTo(w / 2 + 3, h / 2 - 2);
				g.lineTo(w / 2, h / 2 + 2);
				g.endFill();
				break;
			}
		}
	}
}

}
