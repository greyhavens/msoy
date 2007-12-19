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

import mx.core.Container;

[ExcludeClass]

/**
 *  @private
 */
public class Layout
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
	public function Layout()
	{
		super();
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  target
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the target property.
	 */
	private var _target:Container;

	/**
	 *  The container associated with this layout.
	 */
	public function get target():Container
	{
		return _target;
	}
	
	/**
	 *  @private
	 */
	public function set target(value:Container):void
	{
		_target = value;
	}

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	public function measure():void
	{
	}

	/**
	 *  @private
	 */
	public function updateDisplayList(unscaledWidth:Number,
									  unscaledHeight:Number):void
	{
	}
}

}
