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
 *  The CreditCardValidatorCardType class defines value constants
 *  for specifying the type of credit card to validate.
 *  These values are used in the <code>CreditCardValidator.cardType</code>
 *  property.
 *
 *  @see mx.validators.CreditCardValidator
 */
public final class CreditCardValidatorCardType
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants 
	//
	//--------------------------------------------------------------------------
		
	/**
	 *  Specifies the card type as MasterCard.
	 */
	public static const MASTER_CARD:String = "MasterCard"
	
	/**
	 *  Specifies the card type as Visa.
	 */
	public static const VISA:String = "Visa";
	
	/**
	 *  Specifies the card type as American Express.
	 */
	public static const AMERICAN_EXPRESS:String = "American Express";
	
	/**
	 *  Specifies the card type as Discover.
	 */
	public static const DISCOVER:String = "Discover";
	
	/**
	 *  Specifies the card type as Diners Club.
	 */
	public static const DINERS_CLUB:String = "Diners Club";
}

}
