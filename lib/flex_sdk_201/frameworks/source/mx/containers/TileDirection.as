////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.containers
{

/**
 *  The TileDirection class defines the constant values for the
 *  <code>direction</code> property of the Tile container.
 *
 *  @see mx.containers.Tile
 */
public final class TileDirection
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

    /**
	 *  Specifies that the children of the Tile container are laid out
	 *  horizontally; that is, starting with the first row.
     */
    public static const HORIZONTAL:String = "horizontal";
    
    /**
	 *  Specifies that the children of the Tile container are laid out
	 *  vertically; that is, starting with the first column.
     */
    public static const VERTICAL:String = "vertical";
}

}
