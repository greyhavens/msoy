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

import flash.geom.Rectangle;

/**
 *  RoundedRectangle represents a Rectangle with curved corners
 */
public class RoundedRectangle extends Rectangle
{	
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function RoundedRectangle(x:Number = 0, y:Number = 0,
									 width:Number = 0, height:Number = 0,
									 cornerRadius:Number = 0)
	{
		super(x, y, width, height);

		this.cornerRadius = cornerRadius;
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  cornerRadius
	//----------------------------------

	[Inspectable]

	/**
	 *  The radius of each corner (in pixels).
	 *  Default value is 0.
	 */
	public var cornerRadius:Number = 0;
}

}
