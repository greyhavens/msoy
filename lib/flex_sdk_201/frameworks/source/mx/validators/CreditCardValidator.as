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

import mx.managers.ISystemManager;
import mx.managers.SystemManager;
import mx.resources.ResourceBundle;
import mx.utils.StringUtil;
import mx.validators.IValidatorListener;

/**
 *  The CreditCardValidator class validates that a credit card number
 *  is the correct length, has the correct prefix, and passes
 *  the Luhn mod10 algorithm for the specified card type. 
 *  This validator does not check whether the credit card
 *  is an actual active credit card account.
 *
 *  <p>You can specify the input to the CreditCardValidator in two ways:</p>
 *  <ul>
 *    <li>Use the <code>cardNumberSource</code> and
 *    <code>cardNumberProperty</code> properties to specify
 *    the location of the credit card number, and the 
 *    <code>cardTypeSource</code> and <code>cardTypeProperty</code> properties
 *    to specify the location of the credit card type to validate.</li>
 *    <li>Use the <code>source</code> and 
 *    <code>property</code> properties to specify a single Object.
 *    The Object should contain the following fields:
 *    <ul>
 *        <li><code>cardType</code> - Specifies the type
 *        of credit card being validated. 
 *         <p>In MXML, use the values: <code>"American Express"</code>, 
 *            <code>"Diners Club"</code>, <code>"Discover"</code>, 
 *            <code>"MasterCard"</code>, or <code>"Visa"</code>.</p>
 *         <p>In ActionScript, use the static constants
 *            <code>CreditCardValidatorCardType.MASTER_CARD</code>, 
 *            <code>CreditCardValidatorCardType.VISA</code>, or
 *            <code>CreditCardValidatorCardType.AMERICAN_EXPRESS</code> 
 *            <code>CreditCardValidatorCardType.DISCOVER</code>, or
 *            <code>CreditCardValidatorCardType.DINERS_CLUB</code>.</p>
 *          </li>
 *       <li><code>cardNumber</code> - Specifies the number of the card
 *       being validated.</li>
 *     </ul>
 *    </li>
 *  </ul>
 *  
 *  <p>To perform the validation, it uses the following guidelines:</p>
 *  <p>Length:</p>
 *  <ol>
 *    <li>Visa: 13 or 16 digits</li> 
 *    <li>MasterCard: 16 digits</li> 
 *    <li>Discover: 16 digits</li> 
 *    <li>American Express: 15 digits</li> 
 *    <li>Diners Club: 14 digits or 16 digits if it also functions as MasterCard</li>
 *  </ol>
 *  Prefix:
 *  <ol>
 *    <li>Visa: 4</li> 
 *    <li>MasterCard: 51 to 55</li>
 *    <li>Discover: 6011</li>
 *    <li>American Express: 34 or 37</li>
 *    <li>Diners Club: 300 to 305, 36 or 38, 51 to 55</li>
 *  </ol>
 *  
 *  @mxml
 *
 *  <p>The <code>&lt;mx:CreditCardValidator&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds the following tag attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:CreditCardValidator
 *    allowedFormatChars=" -" 
 *    cardNumberListener="<i>Object specified by cardNumberSource</i>"
 *    cardNumberProperty="<i>No default</i>"
 *    cardNumberSource="<i>No default</i>"
 *    cardTypeListener="<i>Object specified by cardTypeSource</i>"
 *    cardTypeProperty="<i>No default</i>"
 *    cardTypeSource="<i>No default</i>"
 *    invalidCharError= "Invalid characters in your credit card number. (Only enter numbers.)" 
 *    invalidNumberError="The credit card number is invalid." 
 *    noNumError="No credit card number specified." 
 *    noTypeError="No credit card type specified or the type is not valid." 
 *    wrongLengthError="Your credit card number contains the wrong number of digits." 
 *    wrongTypeError="Incorrect card type specified." 
 *  /&gt;
 *  </pre>
 *
 *  @see mx.validators.CreditCardValidatorCardType
 *  
 *  @includeExample examples/CreditCardValidatorExample.mxml
 */
public class CreditCardValidator extends Validator
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class initialization
	//
	//--------------------------------------------------------------------------

	loadResources();
	
	//--------------------------------------------------------------------------
	//
	//  Class resources
	//
	//--------------------------------------------------------------------------

	[ResourceBundle("validators")]

    /**
	 *  @private    
     */
	private static var packageResources:ResourceBundle;

    /**
	 *  @private    
     */
	private static var resourceInvalidCharError:String;

    /**
	 *  @private    
     */
	private static var resourceInvalidNumberError:String;

    /**
	 *  @private    
     */
	private static var resourceNoNumError:String;

    /**
	 *  @private    
     */
	private static var resourceNoTypeError:String;

    /**
	 *  @private    
     */
	private static var resourceWrongLengthError:String;

    /**
	 *  @private    
     */
	private static var resourceWrongTypeError:String;

	/**
	 *  @private    
     */	
	private static var resourceAllowedFormatChars:String;
	
	/**
	 *  @private    
     */	
	private static var resourceMissingCardType:String;	

	/**
	 *  @private    
     */	
	private static var resourceMissingCardNumber:String;	

	/**
	 *  @private    
     */	
	private static var resourceInvalidFormatChars:String;	

	/**
	 *  @private    
     */	
	private static var resourceCNSAttribute:String;

	/**
	 *  @private    
     */	
	private static var resourceCTSAttribute:String;

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private    
     *  Loads resources for this class.
     */
	private static function loadResources():void
	{
		resourceAllowedFormatChars =
			packageResources.getString("creditCardValidatorAllowedFormatChars");
				
		resourceInvalidFormatChars =
			packageResources.getString("invalidFormatChars");
			
		resourceInvalidCharError =
			packageResources.getString("invalidCharErrorCCV");

		resourceInvalidNumberError =
			packageResources.getString("invalidNumberError");

		resourceNoNumError =
			packageResources.getString("noNumError");

		resourceNoTypeError =
			packageResources.getString("noTypeError");

		resourceWrongLengthError =
			packageResources.getString("wrongLengthErrorCCV");

		resourceWrongTypeError =
			packageResources.getString("wrongTypeError");

		resourceMissingCardType =
			packageResources.getString("missingCardType");

		resourceMissingCardNumber =
			packageResources.getString("missingCardNumber");

		resourceCNSAttribute =
			packageResources.getString("CNSAttribute");

		resourceCTSAttribute =
			packageResources.getString("CTSAttribute");		
	}
	
	/**
	 *  Convenience method for calling a validator.
	 *  Each of the standard Flex validators has a similar convenience method.
	 *
	 *  @param validator The CreditCardValidator instance.
	 *
	 *  @param value A field to validate, which must contain
	 *  the following fields:
	 *  <ul>
	 *    <li><code>cardType</code> - Specifies the type of credit card being validated. 
	 *    Use the static constants
	 *    <code>CreditCardValidatorCardType.MASTER_CARD</code>, 
	 *    <code>CreditCardValidatorCardType.VISA</code>,
	 *    <code>CreditCardValidatorCardType.AMERICAN_EXPRESS</code>,
	 *    <code>CreditCardValidatorCardType.DISCOVER</code>, or
	 *    <code>CreditCardValidatorCardType.DINERS_CLUB</code>.</li>
	 *    <li><code>cardNumber</code> - Specifies the number of the card
	 *    being validated.</li></ul>
	 *
	 *  @param baseField Text representation of the subfield
	 *  specified in the value parameter. 
	 *  For example, if the <code>value</code> parameter
	 *  specifies value.date, the <code>baseField</code> value is "date".
	 *
	 *  @return An Array of ValidationResult objects, with one ValidationResult 
	 *  object for each field examined by the validator. 
	 *
	 *  @see mx.validators.ValidationResult
	 */
	public static function validateCreditCard(validator:CreditCardValidator,
											  value:Object,
											  baseField:String):Array
	{
		var results:Array = [];
	    var baseFieldDot:String = baseField ? baseField + "." : "";
		
		var valid:String = DECIMAL_DIGITS + validator.allowedFormatChars;
		var cardType:String = null;
		var cardNum:String = null;
		var digitsOnlyCardNum:String = "";

		var n:int;
		var i:int;
		
		try 
		{
			cardType = String(value.cardType);
		}
		catch(e:Error)
		{
			// Use the default value and move on
			throw new Error(resourceMissingCardType);
		}
		
		try 
		{
			cardNum = value.cardNumber;
		}
		catch(f:Error)
		{
			// Use the default value and move on
			throw new Error(resourceMissingCardNumber);
		}
		
        if (validator.required)
        {
            if (cardType.length == 0)
            {
				results.push(new ValidationResult(
					true, baseFieldDot + "cardType",
					"requiredField", validator.requiredFieldError));
            }

            if (!cardNum)
            {
                results.push(new ValidationResult(
					true, baseFieldDot + "cardNumber",
					"requiredField", validator.requiredFieldError));
            }
        }
		
		n = validator.allowedFormatChars.length;
		for (i = 0; i < n; i++)
		{
			if (DECIMAL_DIGITS.indexOf(validator.allowedFormatChars.charAt(i)) != -1)
				throw new Error(resourceInvalidFormatChars);
		}
		
		if (!cardType)
		{
			results.push(new ValidationResult(
				true, baseFieldDot + "cardType",
				"noType", validator.noTypeError));
		}
		else if (cardType != CreditCardValidatorCardType.MASTER_CARD &&
				 cardType != CreditCardValidatorCardType.VISA &&
				 cardType != CreditCardValidatorCardType.AMERICAN_EXPRESS &&
				 cardType != CreditCardValidatorCardType.DISCOVER &&
				 cardType != CreditCardValidatorCardType.DINERS_CLUB)
		{
			results.push(new ValidationResult(
				true, baseFieldDot + "cardType",
				"wrongType", validator.wrongTypeError));
		}

		if (!cardNum)
		{
			results.push(new ValidationResult(
				true, baseFieldDot + "cardNumber",
				"noNum", validator.noNumError));
		}

		if (cardNum)
		{
			n = cardNum.length;
			for (i = 0; i < n; i++)
			{
				var temp:String = "" + cardNum.substring(i, i + 1);
				if (valid.indexOf(temp) == -1)
				{
					results.push(new ValidationResult(
						true, baseFieldDot + "cardNumber",
						"invalidChar", validator.invalidCharError));
				}
				if (DECIMAL_DIGITS.indexOf(temp) != -1)
					digitsOnlyCardNum += temp;
			}
		}
		
		if (results.length > 0)
			return results;

		var cardNumLen:int = digitsOnlyCardNum.toString().length;
		var correctLen:Number = -1;
		var correctLen2:Number = -1;
		var correctPrefixArray:Array = [];
		
		// diner club cards with a beginning digit of 5 need to be treated as
		// master cards. Go to the following link for more info.
		// http://www.globalpaymentsinc.com/myglobal/industry_initiatives/mc-dc-canada.html
		if (cardType == CreditCardValidatorCardType.DINERS_CLUB &&
			digitsOnlyCardNum.charAt(0) == "5")
		{
			cardType = CreditCardValidatorCardType.MASTER_CARD;
		}
		
		switch (cardType)
		{
			case CreditCardValidatorCardType.MASTER_CARD:
			{
				correctLen = 16;
				correctPrefixArray.push("51");
				correctPrefixArray.push("52");
				correctPrefixArray.push("53");
				correctPrefixArray.push("54");
				correctPrefixArray.push("55");
				break;
			}

			case CreditCardValidatorCardType.VISA:
			{
				correctLen = 13;
				correctLen2 = 16;
				correctPrefixArray.push("4");
				break;
			}

			case CreditCardValidatorCardType.AMERICAN_EXPRESS:
			{
				correctLen = 15;
				correctPrefixArray.push("34");
				correctPrefixArray.push("37");
				break;
			}

			case CreditCardValidatorCardType.DISCOVER:
			{
				correctLen = 16;
				correctPrefixArray.push("6011");
				break;
			}

			case CreditCardValidatorCardType.DINERS_CLUB:
			{
				correctLen = 14;
				correctPrefixArray.push("300");
				correctPrefixArray.push("301");
				correctPrefixArray.push("302");
				correctPrefixArray.push("303");
				correctPrefixArray.push("304");
				correctPrefixArray.push("305");
				correctPrefixArray.push("36");
				correctPrefixArray.push("38");
				break;
			}

			default:
			{
				results.push(new ValidationResult(
					true, baseFieldDot + "cardType",
					"wrongType", validator.wrongTypeError));
				return results;
			}
		}

		if ((cardNumLen != correctLen) && (cardNumLen != correctLen2)) 
		{ 
			results.push(new ValidationResult(
				true, baseFieldDot + "cardNumber",
				"wrongLength", validator.wrongLengthError));
			return results;
		}

		// Validate the prefix
		var foundPrefix:Boolean = false;
		for (i = correctPrefixArray.length - 1; i >= 0; i--)
		{
			if (digitsOnlyCardNum.indexOf(correctPrefixArray[i]) == 0)
			{
				foundPrefix = true;
				break;
			}
		}

		if (!foundPrefix)
		{
			results.push(new ValidationResult(
				true, baseFieldDot + "cardNumber",
				"invalidNumber", validator.invalidNumberError));
			return results;
		}

		// Implement Luhn formula testing of this.cardNumber
		var doubledigit:Boolean = false;
		var checkdigit:int = 0;
		var tempdigit:int;
		for (i = cardNumLen - 1; i >= 0; i--)
		{
			tempdigit = Number(digitsOnlyCardNum.charAt(i));
			if (doubledigit)
			{
				tempdigit *= 2;
				checkdigit += (tempdigit % 10);
				if ((tempdigit / 10) >= 1.0)
					checkdigit++;
				doubledigit = false;
			}
			else
			{
				checkdigit = checkdigit + tempdigit;
				doubledigit = true;
			}
		}

		if ((checkdigit % 10) != 0)
		{
			results.push(new ValidationResult(
				true, baseFieldDot + "cardNumber",
				"invalidNumber", validator.invalidNumberError));
			return results;
		}

		return results;
	}

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function CreditCardValidator()
	{
		super();

		bundleChanged();

		subFields = [ "cardNumber", "cardType" ];
	}

	//--------------------------------------------------------------------------
	//
	//  Overridden properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  actualListeners
	//----------------------------------

	/** 
	 *  @private
	 *  Returns either the listener or the source
	 *  for the cardType and cardNumber subfields.
	 */
	override protected function get actualListeners():Array
	{
		var results:Array = [];
		
		var typeResult:Object;
		if (_cardTypeListener)
			typeResult = _cardTypeListener;
		else if (_cardTypeSource)
			typeResult = _cardTypeSource;
			
		results.push(typeResult);
		if (typeResult is IValidatorListener)
			IValidatorListener(typeResult).validationSubField = "cardType";
			
		var numResult:Object;
		if (_cardNumberListener)
			numResult = _cardNumberListener;
		else if (_cardNumberSource)
			numResult = _cardNumberSource;
			
		results.push(numResult);
		if (numResult is IValidatorListener)
			IValidatorListener(numResult).validationSubField = "cardNumber";
				
		if (results.length > 0 && listener)
			results.push(listener);


		return results;
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  allowedFormatChars
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the allowedFormatChars property.
	 */
	private var _allowedFormatChars:String;

	[Inspectable(category="General", defaultValue=" -")]

	/** 
	 *  The set of formatting characters allowed in the
	 *  <code>cardNumber</code> field.
	 *
	 *  @default " -" (space and dash)
	 */
	public function get allowedFormatChars():String
	{
		return _allowedFormatChars;	
	}

    /**
	 *  @private
	 */
	public function set allowedFormatChars(value:String):void
	{
		if (value != _allowedFormatChars)
		{
			var n:int = value.length;
			for (var i:int = 0; i < n; i++)
			{
				if (DECIMAL_DIGITS.indexOf(value.charAt(i)) != -1)
					throw new Error(resourceInvalidFormatChars);
			}
			_allowedFormatChars = value;
		}
	}
	
	//----------------------------------
	//  cardNumberListener
	//----------------------------------
	
	/**
	 *  @private
	 *  Storage for the cardNumberListener property.
	 */
	private var _cardNumberListener:IValidatorListener;
	
    [Inspectable(category="General")]

	/** 
	 *  The component that listens for the validation result
	 *  for the card number subfield. 
	 *  If none is specified, use the value specified
	 *  to the <code>cardNumberSource</code> property.
	 */
	public function get cardNumberListener():IValidatorListener
	{
		return _cardNumberListener;
	}
	
	/**
	 *  @private
	 */
	public function set cardNumberListener(value:IValidatorListener):void
	{
		if (_cardNumberListener == value)
			return;
			
		removeListenerHandler();
		
		_cardNumberListener = value;
		
		addListenerHandler();
	}
		
	//----------------------------------
	//  cardNumberProperty
	//----------------------------------

    [Inspectable(category="General")]

	/**
	 *  Name of the card number property to validate. 
	 *  This attribute is optional, but if you specify
	 *  the <code>cardNumberSource</code> property, 
	 *  you should also set this property.
	 */
	public var cardNumberProperty:String;
	
	//----------------------------------
	//  cardNumberSource
	//----------------------------------	

	/**
	 *  @private
	 *  Storage for the cardNumberSource property.
	 */
	private var _cardNumberSource:Object;
	
    [Inspectable(category="General")]

	/** 
	 *  Object that contains the value of the card number field.
	 *  If you specify a value for this property, you must also specify
	 *  a value for the <code>cardNumberProperty</code> property. 
	 *  Do not use this property if you set the <code>source</code> 
	 *  and <code>property</code> properties. 
	 */
	public function get cardNumberSource():Object
	{
		return _cardNumberSource;
	}
	
	/**
	 *  @private
	 */
	public function set cardNumberSource(value:Object):void
	{
		if (_cardNumberSource == value)
			return;
		
		if (value is String)
			throw new Error(StringUtil.substitute(resourceCNSAttribute, value));
			
		removeListenerHandler();	
		
		_cardNumberSource = value;
		
		addListenerHandler();
	}
	
	//----------------------------------
	//  cardTypeListener
	//----------------------------------
	
	/**
	 *  @private
	 *  Storage for the cardTypeListener property.
	 */
	private var _cardTypeListener:IValidatorListener;
	
    [Inspectable(category="General")]

	/** 
	 *  The component that listens for the validation result
	 *  for the card type subfield. 
	 *  If none is specified, then use the value
	 *  specified to the <code>cardTypeSource</code> property.
	 */
	public function get cardTypeListener():IValidatorListener
	{
		return _cardTypeListener;
	}
	
	/**
	 *  @private
	 */
	public function set cardTypeListener(value:IValidatorListener):void
	{
		if (_cardTypeListener == value)
			return;
			
		removeListenerHandler();
		
		_cardTypeListener = value;
		
		addListenerHandler();
	}
		
	//----------------------------------
	//  cardTypeProperty
	//----------------------------------

    [Inspectable(category="General")]

	/**
	 *  Name of the card type property to validate. 
	 *  This attribute is optional, but if you specify the
	 *  <code>cardTypeSource</code> property,
	 *  you should also set this property.
	 *
     *  <p>In MXML, valid values are:</p>
     *  <ul>
     *    <li><code>"American Express"</code></li>
     *    <li><code>"Diners Club"</code></li>
     *    <li><code>"Discover"</code></li>
     *    <li><code>"MasterCard"</code></li>
     *    <li><code>"Visa"</code></li>
     *  </ul>
	 *
	 *  <p>In ActionScript, you can use the following constants to set this property:</p>
	 *  <p><code>CreditCardValidatorCardType.AMERICAN_EXPRESS</code>, 
	 *  <code>CreditCardValidatorCardType.DINERS_CLUB</code>,
	 *  <code>CreditCardValidatorCardType.DISCOVER</code>, 
	 *  <code>CreditCardValidatorCardType.MASTER_CARD</code>, and 
	 *  <code>CreditCardValidatorCardType.VISA</code>.</p>
	 *
	 *  @see mx.validators.CreditCardValidatorCardType
	 */
	public var cardTypeProperty:String;
	
	//----------------------------------
	//  cardTypeSource
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the cardTypeSource property.
	 */
	private var _cardTypeSource:Object;
	
    [Inspectable(category="General")]

	/** 
	 *  Object that contains the value of the card type field.
	 *  If you specify a value for this property, you must also specify
	 *  a value for the <code>cardTypeProperty</code> property. 
	 *  Do not use this property if you set the <code>source</code> 
	 *  and <code>property</code> properties. 
	 */
	public function get cardTypeSource():Object
	{
		return _cardTypeSource;
	}
	
	/**
	 *  @private
	 */
	public function set cardTypeSource(value:Object):void
	{
		if (_cardTypeSource == value)
			return;
			
		if (value is String)
			throw new Error(StringUtil.substitute(resourceCTSAttribute, value));
			
		removeListenerHandler();	
		
		_cardTypeSource = value;
		
		addListenerHandler();
	}
	
	//--------------------------------------------------------------------------
	//
	//  Properties: Errors
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  invalidCharError
	//----------------------------------
	
	[Inspectable(category="Errors", defaultValue="Invalid characters in your credit card number. (Only enter numbers.)")]

	/** 
	 *  Error message when the <code>cardNumber</code> field contains invalid characters.
	 *
	 *  @default "Invalid characters in your credit card number. (Enter numbers only.)"
	 */
	public var invalidCharError:String;

	//----------------------------------
	//  invalidNumberError
	//----------------------------------
	
	[Inspectable(category="Errors", defaultValue="The credit card number is invalid.")]
	
	/** 
	 *  Error message when the credit card number is invalid.
	 *
	 *  @default "The credit card number is invalid."
	 */
	public var invalidNumberError:String;

	//----------------------------------
	//  noNumError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="No credit card number specified.")]
	
	/** 
	 *  Error message when the <code>cardNumber</code> field is empty.
	 *
	 *  @default "No credit card number is specified."
	 */
	public var noNumError:String;
	
	//----------------------------------
	//  noTypeError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="No credit card type specified or the type is not valid.")]

	/** 
	 *  Error message when the <code>cardType</code> field is blank.
	 *
	 *  @default "No credit card type is specified or the type is not valid."
	 */
	public var noTypeError:String;

	//----------------------------------
	//  wrongLengthError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Your credit card number contains the wrong number of digits.")]

	/**
	 *  Error message when the <code>cardNumber</code> field contains the wrong
	 *  number of digits for the specified credit card type.
	 *
	 *  @default "Your credit card number contains the wrong number of digits." 
	 */
	public var wrongLengthError:String;
	
	//----------------------------------
	//  wrongTypeError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Incorrect card type specified.")]

	/** 
	 *  Error message the <code>cardType</code> field contains an invalid credit card type. 
	 *  You should use the predefined constants for the <code>cardType</code> field:
	 *  <code>CreditCardValidatorCardType.MASTER_CARD</code>,
	 *  <code>CreditCardValidatorCardType.VISA</code>, 
	 *  <code>CreditCardValidatorCardType.AMERICAN_EXPRESS</code>,
	 *  <code>CreditCardValidatorCardType.DISCOVER</code>, or 
	 *  <code>CreditCardValidatorCardType.DINERS_CLUB</code>.
	 *
	 *  @default "Incorrect card type is specified."
	 */
	public var wrongTypeError:String;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
     *  Override of the base class <code>doValidation()</code> method
	 *  to validate a credit card number.
	 *
	 *  <p>You do not call this method directly;
	 *  Flex calls it as part of performing a validation.
	 *  If you create a custom Validator class, you must implement this method. </p>
	 *
	 *  @param value an Object to validate.
     *
	 *  @return An Array of ValidationResult objects, with one ValidationResult 
	 *  object for each field examined by the validator. 
	 */
	override protected function doValidation(value:Object):Array
    {
		var results:Array = super.doValidation(value);
		
		// Return if there are errors
		// or if the required property is set to false and length is 0.
		var val:String = value ? String(value) : "";
		if (results.length > 0 || ((val.length == 0) && !required))
			return results;
		else
		    return CreditCardValidator.validateCreditCard(this, value, null);
    }
	
	/**
	 *  @private
	 *  Grabs the data for the validator from two different sources
	 */
	override protected function getValueFromSource():Object
	{
		var useValue:Boolean = false;
	
		var value:Object = {};
		
		if (cardTypeSource && cardTypeProperty)
		{
			value.cardType = cardTypeSource[cardTypeProperty];
			useValue = true;
		}
		
		if (cardNumberSource && cardNumberProperty)
		{
			value.cardNumber = cardNumberSource[cardNumberProperty];
			useValue = true;
		}
	
		if (useValue)
			return value;
		else
			return super.getValueFromSource();
	}
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private    
     *  Populates localizable properties from the loaded bundle for this class.
     */
	private function bundleChanged():void
	{
		allowedFormatChars = resourceAllowedFormatChars;
		
		invalidCharError = resourceInvalidCharError;
		invalidNumberError = resourceInvalidNumberError;
		noNumError = resourceNoNumError;
		noTypeError = resourceNoTypeError;
		wrongLengthError = resourceWrongLengthError;
		wrongTypeError = resourceWrongTypeError;
	}
}

}

