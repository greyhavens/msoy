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

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.InteractiveObject;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.Event;
import mx.core.FlexShape;
import mx.styles.StyleManager;

/**
 *  Documentation is not currently available.
 *  @review
 */
public class BusyCursor extends Sprite
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
	public function BusyCursor()
	{
		super();
		
		cursorClass =
			StyleManager.getStyleDeclaration("CursorManager").getStyle("busyCursorBackground");
		
		cursorHolder = new cursorClass();
		addChild(cursorHolder);
		InteractiveObject(cursorHolder).mouseEnabled = false;
		
		var xOff:Number = -0.5;
		var yOff:Number = -0.5;

		var g:Graphics;

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
	private var cursorClass:Class;
	
	/**
	 *  @private
	 */
	private var cursorHolder:DisplayObject;
	
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
	//  Event handlers
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
