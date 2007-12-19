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
import mx.styles.StyleManager;
import mx.utils.ColorUtil;

/**
 *  The skin for the track in a ProgressBar.
 */
public class ProgressTrackSkin extends Border
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
	public function ProgressTrackSkin()
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
		return 200;
	}
	
	//----------------------------------
	//  measuredHeight
	//----------------------------------

	/**
	 *  @private
	 */
	override public function get measuredHeight():Number
	{
		return 6;
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

		// User-defined styles
		var bevel:Boolean = getStyle("bevel");
		var borderColor:uint = getStyle("borderColor");
		var fillColors:Array = getStyle("trackColors");
		StyleManager.getColorNames(fillColors);
		
		var borderColorDrk1:Number =
			ColorUtil.adjustBrightness2(borderColor, -60);
		
		var g:Graphics = graphics;
		
		g.clear();
		
		if (bevel)
		{
			drawRoundRect(
				0, 0, w, h, 0,
				borderColorDrk1, 1);

			drawRoundRect(
				1, 1, w - 1, h - 1, 0,
				borderColor, 1);
		}
		else
		{
			drawRoundRect(
				0, 0, w, h, 0,
				borderColor, 1);
		}

		drawRoundRect(
			1, 1, w - 2, h - 2, 0,
			fillColors, 1,
			verticalGradientMatrix(0, 0, w - 2, h - 2));
	}
}

}
