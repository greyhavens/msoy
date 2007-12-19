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

/**
 *  The PhoneNumberValidator class validates that a string
 *  is a valid phone number.
 *  A valid phone number contains at least 10 digits,
 *  plus additional formatting characters.
 *  The validator does not check if the phone number
 *  is an actual active phone number.
 *  
 *  @mxml
 *
 *  <p>The <code>&lt;mx:PhoneNumberValidator&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds the following tag attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:PhoneNumberValidator 
 *    allowedFormatChars="()- .+" 
 *    invalidCharError="Invalid characters in your phone number." 
 *    wrongLengthError="Your telephone number must be at least 10 digits in length." 
 *  /&gt;
 *  </pre>
 *  
 *  @includeExample examples/PhoneNumberValidatorExample.mxml
 */
public class PhoneNumberValidator extends Validator
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
	private static var resourceAllowedFormatChars:String;	
	
    /**
	 *  @private    
     */
	private static var resourceInvalidCharError:String;

    /**
	 *  @private    
     */
	private static var resourceWrongLengthError:String;
	
	/**
	 *  @private    
     */	
	private static var resourceInvalidFormatChars:String;	

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
		resourceInvalidFormatChars =
			packageResources.getString("invalidFormatChars");			
		
		resourceAllowedFormatChars =
			packageResources.getString("phoneNumberValidatorAllowedFormatChars");
		
		resourceInvalidCharError = packageResources.getString("invalidCharErrorPNV");

		resourceWrongLengthError = packageResources.getString("wrongLengthErrorPNV");
	}
	
	/**
	 *  Convenience method for calling a validator
	 *  from within a custom validation function.
	 *  Each of the standard Flex validators has a similar convenience method.
	 *
	 *  @param validator The PhoneNumberValidator instance.
	 *
	 *  @param value A field to validate.
	 *
	 *  @param baseField Text representation of the subfield
	 *  specified in the <code>value</code> parameter.
	 *  For example, if the <code>value</code> parameter specifies value.phone,
	 *  the <code>baseField</code> value is "phone".
	 *
	 *  @return An Array of ValidationResult objects, with one ValidationResult 
	 *  object for each field examined by the validator. 
	 *
	 *  @see mx.validators.ValidationResult
	 */
	public static function validatePhoneNumber(validator:PhoneNumberValidator,
											   value:Object,
											   baseField:String):Array
	{
		var results:Array = [];
		
		var valid:String =  DECIMAL_DIGITS + validator.allowedFormatChars;
		var len:int = value.toString().length;
		var digitLen:int = 0;
		var n:int;
		var i:int;
		
		n = validator.allowedFormatChars.length;
		for (i = 0; i < n; i++)
		{
			if (DECIMAL_DIGITS.indexOf(validator.allowedFormatChars.charAt(i)) != -1)
				throw new Error(resourceInvalidFormatChars);
		}

		for (i = 0; i < len; i++)
		{
			var temp:String = "" + value.toString().substring(i, i + 1);
			if (valid.indexOf(temp) == -1)
			{
				results.push(new ValidationResult(true, baseField, "invalidChar", validator.invalidCharError));
				return results;
			}
			if (valid.indexOf(temp) <= 9)
				digitLen++;
		}

		if (digitLen < 10)
		{
			results.push(new ValidationResult(true, baseField, "wrongLength", validator.wrongLengthError));
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
	public function PhoneNumberValidator()
	{
		super();

		bundleChanged();
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
	 */
	private var _allowedFormatChars:String;

	[Inspectable(category="General", defaultValue="-()+. ")]

	/** 
	 *  The set of allowable formatting characters.
	 *
	 *  @default "()- .+"
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

	//--------------------------------------------------------------------------
	//
	//  Properties: Errors
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  invalidCharError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Invalid characters in your phone number.")]

	/** 
	 *  Error message when the value contains invalid characters.
	 *
	 *  @default "Invalid characters in your phone number."
	 */
	public var invalidCharError:String;

	//----------------------------------
	//  wrongLengthError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Your telephone number must be at least 10 digits in length.")]

	/** 
	 *  Error message when the value has fewer than 10 digits.
	 *
	 *  @default "Your telephone number must be at least 10 digits in length."
	 */
	public var wrongLengthError:String;
	
	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
     *  Override of the base class <code>doValidation()</code> method
     *  to validate a phone number.
     *
	 *  <p>You do not typically call this method directly;
	 *  Flex calls it as part of performing a validation.
	 *  If you create a custom Validator class, you must implement this method. </p>
	 *
     *  @param value Object to validate.
     *
	 *  @return An Array of ValidationResult objects, with one ValidationResult 
	 *  object for each field examined by the validator. 
	 */
	override protected function doValidation(value:Object):Array
    {
		var results:Array = super.doValidation(value);
		
		// Return if there are errors
		// or if the required property is set to <code>false</code> and length is 0.
		var val:String = value ? String(value) : "";
		if (results.length > 0 || ((val.length == 0) && !required))
			return results;
		else
		    return PhoneNumberValidator.validatePhoneNumber(this, value, null);
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
		wrongLengthError = resourceWrongLengthError;
	}
}

}
