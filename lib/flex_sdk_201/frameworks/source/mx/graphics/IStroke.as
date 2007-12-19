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

/**
 *  Defines the interface that classes that define a line must implement.
 */
public interface IStroke
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  weight
	//----------------------------------

	/**
	 *  The line weight, in pixels.
	 *  For many chart lines, the default value is 1 pixel.
	 */
	function get weight():Number;
	
	/**
	 *  @private
	 */
	function set weight(value:Number):void;
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  Applies the properties to the specified Graphics object.
	 *   
	 *  @param g The Graphics object to apply the properties to.
	 *  
	 */
	function apply(g:Graphics):void;
}

}
