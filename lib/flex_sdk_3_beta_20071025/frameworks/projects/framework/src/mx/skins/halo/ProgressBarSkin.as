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
import mx.styles.StyleManager;
import mx.utils.ColorUtil;

/**
 *  The skin for a ProgressBar.
 */
public class ProgressBarSkin extends Border
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
	public function ProgressBarSkin()
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
		var barColorStyle:* = getStyle("barColor");
		var barColor:uint = StyleManager.isValidStyleValue(barColorStyle) ?
							barColorStyle :
							getStyle("themeColor");

		var barColor0:Number = ColorUtil.adjustBrightness2(barColor, 40);

		// default fill color for halo uses theme color
		var fillColors:Array = [ barColor0, barColor ]; 
				
		graphics.clear();
				
		// glow
		drawRoundRect(
			0, 0, w, h, 0,
			fillColors, 0.5,
			verticalGradientMatrix(0, 0, w - 2, h - 2));
		
		// fill
		drawRoundRect(
			1, 1, w - 2, h - 2, 0,
			fillColors, 1,
			verticalGradientMatrix(0, 0, w - 2, h - 2));
	}
}

}
