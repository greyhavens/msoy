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
import mx.containers.Box;
import mx.containers.BoxDirection;
import mx.skins.ProgrammaticSkin;

/**
 *  The skin for the separator between the Links in a LinkBar.
 */
public class LinkSeparator extends ProgrammaticSkin
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
	public function LinkSeparator()
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

		var separatorColor:uint = getStyle("separatorColor");
		var separatorWidth:Number = getStyle("separatorWidth");
		
		var isVertical:Boolean = false;
		
		var g:Graphics = graphics;
				
		g.clear();
		
		if (separatorWidth > 0)
		{
			if (parent is Box)
				isVertical = Box(parent).direction == BoxDirection.VERTICAL;
			
			g.lineStyle(separatorWidth, separatorColor);
			if (isVertical)
			{
				g.moveTo(4, h / 2);
				g.lineTo(w - 4, h / 2);
			}
			else
			{
				g.moveTo(w / 2, 6);
				g.lineTo(w / 2, h - 5);
			}
		}
	}
}

}
