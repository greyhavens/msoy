////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.containers.gridClasses
{

import mx.core.UIComponent;

[ExcludeClass]

/**
 *  @private
 *  Internal helper class used to exchange information between
 *  Grid and GridRow.
 */
public class GridRowInfo
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
	public function GridRowInfo()
	{
		super();

		min = 0;
		preferred = 0;
		max = UIComponent.DEFAULT_MAX_HEIGHT;
		flex = 0;
	}
	
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  flex
	//----------------------------------

	/**
	 *  Input: Measurement for the GridRow.
	 */
	public var flex:Number;
	
	//----------------------------------
	//  height
	//----------------------------------

	/**
	 *  Output: The actual height of each row,
	 *  as determined by updateDisplayList().
	 */
	public var height:Number;

	//----------------------------------
	//  max
	//----------------------------------

	/**
	 *  Input: Measurement for the GridRow.
	 */
	public var max:Number;
	
	//----------------------------------
	//  min
	//----------------------------------

	/**
	 *  Input: Measurement for the GridRow.
	 */
	public var min:Number;
	
	//----------------------------------
	//  preferred
	//----------------------------------

	/**
	 *  Input: Measurement for the GridRow.
	 */
	public var preferred:Number;
	
	//----------------------------------
	//  y
	//----------------------------------

	/**
	 *  Output: The actual position of each row,
	 *  as determined by updateDisplayList().
	 */
	public var y:Number;
}

}
