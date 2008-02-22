////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

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

import mx.skins.ProgrammaticSkin;

/**
 *  The skin for the StatusBar of a WindowedApplication or Window.
 */
public class StatusBarBackgroundSkin extends ProgrammaticSkin
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
	public function StatusBarBackgroundSkin()
	{
		super();
	}
	
	//--------------------------------------------------------------------------
	//
	//  Overridden methods: Programmatic Skin
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private.
	 */
	override protected function updateDisplayList(unscaledWidth:Number,
									  			  unscaledHeight:Number):void
	{
		super.updateDisplayList(unscaledWidth, unscaledHeight);
		
		graphics.clear();
		drawRoundRect(
			0, 0, unscaledWidth, unscaledHeight, null,
			getStyle("statusBarBackgroundColor"), 1.0);
		graphics.moveTo(0, 0);
		graphics.lineStyle(1, 0x000000, 0.35);
		graphics.lineTo(unscaledWidth, 0);
	}
}

}
