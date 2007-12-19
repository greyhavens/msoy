////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.containers.utilityClasses
{

import mx.core.IUIComponent;

[ExcludeClass]

/**
 *  @private
 *  Helper class for the Flex.flexChildrenProportionally() method.
 */
public class FlexChildInfo
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
	public function FlexChildInfo()
	{
		super();
	}
	
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  child
	//----------------------------------

	/**
	 *  @private
	 */
	public var child:IUIComponent;

	//----------------------------------
	//  size
	//----------------------------------

	/**
	 *  @private
	 */
	public var size:Number = 0;

	//----------------------------------
	//  preferred
	//----------------------------------

	/**
	 *  @private
	 */
	public var preferred:Number = 0;

	//----------------------------------
	//  flex
	//----------------------------------

	/**
	 *  @private
	 */
	public var flex:Number = 0;
	
	//----------------------------------
	//  percent
	//----------------------------------

	/**
	 *  @private
	 */
	public var percent:Number;

	//----------------------------------
	//  min
	//----------------------------------

	/**
	 *  @private
	 */
	public var min:Number;

	//----------------------------------
	//  max
	//----------------------------------

	/**
	 *  @private
	 */
	public var max:Number;

	//----------------------------------
	//  width
	//----------------------------------

	/**
	 *  @private
	 */
	public var width:Number;

	//----------------------------------
	//  height
	//----------------------------------

	/**
	 *  @private
	 */
	public var height:Number;
}

}
