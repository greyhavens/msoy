////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.containers.utilityClasses
{

import mx.core.Container;
import mx.resources.IResourceManager;
import mx.resources.ResourceManager;

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
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  Used for accessing localized error messages.
	 */
	protected var resourceManager:IResourceManager =
									ResourceManager.getInstance();

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
