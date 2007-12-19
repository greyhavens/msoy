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

import mx.containers.utilityClasses.FlexChildInfo;
import mx.core.UIComponent;

[ExcludeClass]

/**
 *  @private
 *  Internal helper class used to exchange information between
 *  Grid and GridRow.
 */
public class GridColumnInfo extends FlexChildInfo
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
	public function GridColumnInfo()
	{
		super();

		min = 0;
		preferred = 0;
		max = UIComponent.DEFAULT_MAX_WIDTH;
		flex = 0;
		percent = 0;
	}
	
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  x
	//----------------------------------

	/**
	 *  Output: the actual position of each column,
	 *  as determined by updateDisplayList().
	 */
	public var x:Number;
}

}
