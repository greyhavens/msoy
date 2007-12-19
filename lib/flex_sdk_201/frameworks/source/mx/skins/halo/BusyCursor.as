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

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.InteractiveObject;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.Event;
import mx.core.FlexShape;
import mx.core.FlexSprite;
import mx.styles.CSSStyleDeclaration;
import mx.styles.StyleManager;

/**
 *  Defines the appearance of the cursor that appears while an operation is taking place. For example, 
 *  while the SWFLoader class loads an asset.
 */
public class BusyCursor extends FlexSprite
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
	public function BusyCursor()
	{
		super();
		
		var cursorManagerStyleDeclaration:CSSStyleDeclaration =
			StyleManager.getStyleDeclaration("CursorManager");
		
		var cursorClass:Class =
			cursorManagerStyleDeclaration.getStyle("busyCursorBackground");
		
		var cursorHolder:DisplayObject = new cursorClass();
		if (cursorHolder is InteractiveObject)
			InteractiveObject(cursorHolder).mouseEnabled = false;
		addChild(cursorHolder);
		
		var xOff:Number = -0.5;
		var yOff:Number = -0.5;

		var g:Graphics;
		
		// Create the minute hand.
		minuteHand = new FlexShape();
		minuteHand.name = "minuteHand";
		g = minuteHand.graphics;
		g.beginFill(0x000000);
		g.moveTo(xOff, yOff);
		g.lineTo(1 + xOff, 0 + yOff);
		g.lineTo(1 + xOff, 5 + yOff);
		g.lineTo(0 + xOff, 5 + yOff);
		g.lineTo(0 + xOff, 0 + yOff);
		g.endFill();
		addChild(minuteHand);
		
		// Create the hour hand.
		hourHand = new FlexShape();
		hourHand.name = "hourHand";
		g = hourHand.graphics;
		g.beginFill(0x000000);
		g.moveTo(xOff, yOff);
		g.lineTo(4 + xOff, 0 + yOff);
		g.lineTo(4 + xOff, 1 + yOff);
		g.lineTo(0 + xOff, 1 + yOff);
		g.lineTo(0 + xOff, 0 + yOff);
		g.endFill();
		addChild(hourHand);
		
		addEventListener(Event.ENTER_FRAME, enterFrameHandler);
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private var minuteHand:Shape;

	/**
	 *  @private
	 */
	private var hourHand:Shape;
	
	//--------------------------------------------------------------------------
	//
	//  Event Handlers
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private function enterFrameHandler(event:Event):void
	{
		minuteHand.rotation += 12;
		hourHand.rotation += 1;
	}
}

}
