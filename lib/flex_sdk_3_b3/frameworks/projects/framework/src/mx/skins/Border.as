////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.skins
{

import mx.core.EdgeMetrics;
import mx.core.IBorder;

/**
 *  The Border class is an abstract base class for various classes that
 *  draw borders, either rectangular or non-rectangular, around UIComponents.
 *  This class does not do any actual drawing itself.
 *
 *  <p>If you create a new non-rectangular border class, you should extend
 *  this class.
 *  If you create a new rectangular border class, you should extend the
 *  abstract subclass RectangularBorder.</p>
 *
 *  @tiptext
 *  @helpid 3321
 */
public class Border extends ProgrammaticSkin implements IBorder
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
	public function Border()
	{
		super();
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  borderMetrics
	//----------------------------------

	/**
	 *  The thickness of the border edges.
	 *
	 *  @return EdgeMetrics with left, top, right, bottom thickness in pixels
	 */
	public function get borderMetrics():EdgeMetrics
	{
		return EdgeMetrics.EMPTY;
	}
}

}
