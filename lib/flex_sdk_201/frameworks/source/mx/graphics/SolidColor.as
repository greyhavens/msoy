////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.graphics
{

import flash.display.Graphics;
import flash.geom.Rectangle;

[DefaultProperty("color")]

/** 
 *  Defines a representation for a color,
 *  including a color and an alpha value. 
 *  
 *  @see mx.graphics.IFill
 */
public class SolidColor implements IFill
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

 	/**
	 *  Constructor.
	 *
	 *  @param color Specifies the color. The default value is 0x000000 (black).
	 *
	 *  @param alpha Specifies the level of transparency. Valid values range from 
	 *  0.0 (completely transparent) to 1.0 (completely opaque). The default value is 1.0.
 	 */
	public function SolidColor(color:uint = 0x000000, alpha:Number = 1.0)
 	{
		super();

		this.color = color;
		this.alpha = alpha;
	}
	
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  alpha
	//----------------------------------

    [Inspectable(category="General")]

	/**
	 *  The transparency of a color.
	 *  Possible values are 0.0 (invisible) through 1.0 (opaque). 
	 *  
	 *  @default 1.0
	 */
	public var alpha:Number = 1.0;
	
	//----------------------------------
	//  color
	//----------------------------------

    [Inspectable(category="General", format="Color")]

	/**
	 *  A color value. 
	 */
	public var color:uint = 0x000000;
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @inheritDoc
	 */
	public function begin(target:Graphics, rc:Rectangle):void
	{
		target.beginFill(color, alpha);
	}
	
	/**
	 *  @inheritDoc
	 */
	public function end(target:Graphics):void
	{
		target.endFill();
	}
}

}
