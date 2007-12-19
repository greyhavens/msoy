////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.managers
{
	
/**
 *  The CursorManagerPriority class defines the constant values for the 
 *  <code>priority</code> argument to the 
 *  <code>CursorManager.setCursor()</code> method. 
 *
 *  @see mx.managers.CursorManager
 */
public final class CursorManagerPriority
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constant that specifies the highest cursor priority when passed
	 *  as the <code>priority</code> argument to <code>setCursor()</code>.
	 */
	public static const HIGH:int = 1;
	
	/**
	 *  Constant that specifies a medium cursor priority when passed 
	 *  as the <code>priority</code> argument to <code>setCursor()</code>.
	 */
	public static const MEDIUM:int = 2;
	
	/**
	 *  Constant that specifies the lowest cursor priority when passed
	 *  as the <code>priority</code> argument to <code>setCursor()</code>.
	 */
	public static const LOW:int = 3;
}

}
