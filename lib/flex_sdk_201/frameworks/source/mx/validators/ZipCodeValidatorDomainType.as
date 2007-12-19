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
 *  The ZipCodeValidatorDomainType class defines the values 
 *  for the <code>domain</code> property of the ZipCodeValidator class,
 *  which you use to specify the type of ZIP code to validate.
 *
 *  @see mx.validators.ZipCodeValidator
 */
public final class ZipCodeValidatorDomainType
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------
	
	/**
	 *  Specifies to validate a United States or Canadian ZIP code.
	 */
	public static var US_OR_CANADA:String = "US or Canada";
	
	/**
	 *  Specifies to validate a United States ZIP code.
	 */
	public static var US_ONLY:String = "US Only";
}

}