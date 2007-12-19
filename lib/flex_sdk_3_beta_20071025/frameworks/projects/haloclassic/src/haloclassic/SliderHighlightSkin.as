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

import mx.skins.Border;

/**
 *  The skin for the highlighted state of the track of a Slider.
 */
public class SliderHighlightSkin extends Border
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
	public function SliderHighlightSkin()
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
	 *  The preferred width of this object.
	 */
	override public function get measuredWidth():Number
	{
		return 1;
	}

	//----------------------------------
	//  measuredHeight
	//----------------------------------

	/**
	 *  The preferred height of this object.
	 */
	override public function get measuredHeight():Number
	{
		return 2;
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

		//var themeColor = getStyle("themeColor");
		var themeColor:int = 0x80FF4D;
		
		graphics.clear();
				
		// highlight
		drawRoundRect(
			0, 0, w, h, 0,
			themeColor, 1);
	}
}

}
