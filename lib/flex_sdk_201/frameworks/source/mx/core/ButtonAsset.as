////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

import mx.core.FlexSimpleButton;

/**
 *  ButtonAsset is a subclass of the Flash Player's SimpleButton class
 *  which represents button symbols that you embed in a Flex
 *  application from a SWF file produced by Flash.
 *  It implements the IFlexDisplayObject interface, which makes it
 *  possible for a SimpleButtonAsset to be displayed in an Image control,
 *  or to be used as a container background or a component skin.
 *
 *  <p>This class is included in Flex for completeness, so that any kind
 *  of symbol in a SWF file produced by Flash can be embedded
 *  in a Flex application.
 *  However, Flex applications do not typically use embedded SimpleButtons.
 *  Refer to more commonly-used asset classes such as BitmapAsset
 *  for more information about how embedded assets work in Flex.</p>
 */
public class ButtonAsset extends FlexSimpleButton
						 implements IFlexAsset, IFlexDisplayObject
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
	public function ButtonAsset()
	{
		super();

		// Remember initial size as our measured size.
		_measuredWidth = width;
		_measuredHeight = height;
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  measuredHeight
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the measuredWidth property.
	 */
	private var _measuredHeight:Number;

	/**
	 *  @inheritDoc
	 */
	public function get measuredHeight():Number
	{
		return _measuredHeight;
	}

	//----------------------------------
	//  measuredWidth
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the measuredWidth property.
	 */
	private var _measuredWidth:Number;

	/**
	 *  @inheritDoc
	 */
	public function get measuredWidth():Number
	{
		return _measuredWidth;
	}

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @inheritDoc
	 */
	public function move(x:Number, y:Number):void
	{
		this.x = x;
		this.y = y;
	}

	/**
	 *  @inheritDoc
	 */
	public function setActualSize(newWidth:Number, newHeight:Number):void
	{
		width = newWidth;
		height = newHeight;
	}
}

}
