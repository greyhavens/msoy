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
public class DateChooserFwdArrowSkin extends Border
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
	public function DateChooserFwdArrowSkin()
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
		return 11;
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
			case "nextMonthUpSkin":
			case "nextMonthDownSkin":
			case "nextMonthOverSkin":
			{
				// Border 
				g.beginFill(0x000000);
				g.moveTo(0, 0);
				g.lineTo(w, h / 2);
				g.lineTo(0, h);
				g.lineTo(0, 0);
				g.endFill();
				break;
			}

			case "nextMonthDisabledSkin":
			{
				g.beginFill(0x999999);
				g.moveTo(0, 0);
				g.lineTo(w, h / 2);
				g.lineTo(0, h);
				g.lineTo(0, 0);
				g.endFill();
				break;
			}
		}
	}
}

}
