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

import mx.skins.Border;

/**
 *  The skin for the highlighted state of the track of a Slider.
 */
public class SliderHighlightSkin extends Border
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
	 *  @private
	 */
	override public function get measuredWidth():Number
	{
		return 1;
	}

	//----------------------------------
	//  measuredHeight
	//----------------------------------

	/**
	 *  @private
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

		var themeColor:int = getStyle("themeColor");
		
		graphics.clear();
				
		// Highlight
		drawRoundRect(
			0, 0, w, 1, 0,
			themeColor, 0.7);
		drawRoundRect(
			0, h - 1, w, 1, 0,
			themeColor, 1);
		drawRoundRect(
			0, h - 2, w, 1, 0,
			themeColor, 0.4);
	}
}

}
