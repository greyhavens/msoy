////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.validators
{

/**
 *  The CurrencyValidatorAlignSymbol class defines value constants
 *  for specifying currency symbol alignment.
 *  These values are used in the <code>CurrencyValidator.alignSymbol</code>
 *  property.
 *
 *  @see mx.validators.CurrencyValidator
 */
public final class CurrencyValidatorAlignSymbol
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

	/**
	 *  Specifies <code>"any"</code> as the alignment of the currency symbol
	 *  for the CurrencyValidator class.
	 */
	public static const ANY:String = "any";

	/**
	 *  Specifies <code>"left"</code> as the alignment of the currency symbol
	 *  for the CurrencyValidator class.
	 */
	public static const LEFT:String = "left";

	/**
	 *  Specifies <code>"right"</code> as the alignment of the currency symbol
	 *  for the CurrencyValidator class.
	 */
	public static const RIGHT:String = "right";
}

}
