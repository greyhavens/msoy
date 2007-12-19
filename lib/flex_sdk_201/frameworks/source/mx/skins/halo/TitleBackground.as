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

import flash.display.GradientType;
import flash.display.Graphics;
import mx.skins.ProgrammaticSkin;
import mx.styles.StyleManager;
import mx.utils.ColorUtil;

/**
 *  The skin for a title bar area of a Panel.
 */
public class TitleBackground extends ProgrammaticSkin
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
	public function TitleBackground()
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

		var borderAlpha:Number = getStyle("borderAlpha");
		var cornerRadius:Number = getStyle("cornerRadius");
		var highlightAlphas:Array = getStyle("highlightAlphas");
		var headerColors:Array = getStyle("headerColors");
		var showChrome:Boolean = headerColors != null;
        StyleManager.getColorNames(headerColors);
		
		var colorDark:Number = ColorUtil.adjustBrightness2(
			headerColors ? headerColors[1] : 0xFFFFFF, -20);
				
		var g:Graphics = graphics;
		
		g.clear();
		
		if (h < 3)
			return;
		
		// Only draw the background if headerColors are defined.
		if (showChrome) 
		{
			g.lineStyle(0, colorDark, borderAlpha);
			g.moveTo(0, h);
			g.lineTo(w, h);
			g.lineStyle(0, 0, 0); 

			// surface
			drawRoundRect(
				0, 0, w, h,
				{ tl: cornerRadius, tr: cornerRadius, bl: 0, br: 0 },
				headerColors, borderAlpha,
				verticalGradientMatrix(0, 0, w, h));

			// highlight
			drawRoundRect(
				0, 0, w, h / 2,
				{ tl: cornerRadius, tr: cornerRadius, bl: 0, br: 0 },
				[ 0xFFFFFF, 0xFFFFFF ], highlightAlphas,
				verticalGradientMatrix(0, 0, w, h / 2));

		}
		
		// edge
		drawRoundRect(
			0, 0, w, h,
			{ tl: cornerRadius, tr: cornerRadius, bl: 0, br: 0 },
			0xFFFFFF, 0.3, null,
			GradientType.LINEAR, null, 
			{ x: 0, y: 1, w: w, h: h - 1,
			  r: { tl: cornerRadius, tr: cornerRadius, bl: 0, br: 0 } });
	}	
}

}
