////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.formatters
{
	
/**
 *  The NumberBaseRoundType class defines the constant values for formatter properties
 *  that specify a type of rounding. For example, you can set the 
 *  <code>NumberFormatter.rounding</code> property using these constants.
 *
 *  @see mx.formatters.NumberFormatter
 *  @see mx.formatters.NumberBase
 */
public final class NumberBaseRoundType
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

	/**
	 *  Rounds a number down to an integer that is both closest to, 
	 *  and less than or equal to, the input number.
	 */
	public static const DOWN:String = "down";

	/**
	 *  Rounds a number up or down to the nearest integer.
	 */
	public static const NEAREST:String = "nearest";

	/**
	 *  Perform no rounding.
	 */
	public static const NONE:String = "none";

	/**
	 *  Rounds a number up to an integer value that is both closest to, 
	 *  and greater than or equal to, the input number.
	 */
	public static const UP:String = "up";
}

}
