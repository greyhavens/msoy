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
 *  The ZipCodeValidator class validates that a String
 *  has the correct length and format for a five-digit ZIP code,
 *  a five-digit+four-digit United States ZIP code, or Canadian postal code.
 *  
 *  @mxml
 *
 *  <p>The <code>&lt;mx:ZipCodeValidator&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds the following tag attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:ZipCodeValidator
 *    allowedFormatChars=" -" 
 *    domain="US Only | US or Canada"
 *    invalidCharError="The ZIP code contains invalid characters." 
 *    invalidDomainError="The domain parameter is invalid. It must be either 'US Only' or 'US or Canada'." 
 *    wrongCAFormatError="The Canadian ZIP code must be formatted 'A1B 2C3'." 
 *    wrongLengthError="The ZIP code must be 5 digits or 5+4 digits." 
 *    wrongUSFormatError="The ZIP+4 extension must be formatted '12345-6789'." 
 *  /&gt;
 *  </pre>
 *  
 *  @see mx.validators.ZipCodeValidatorDomainType
 * 
 *  @includeExample examples/ZipCodeValidatorExample.mxml
 */
public class ZipCodeValidator extends Validator
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
	//  Class constants
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
	private static const DOMAIN_US:uint = 1;
    
    /**
	 *  @private
	 */
	private static const DOMAIN_US_OR_CANADA:uint = 2;

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
	private static var resourceDomain:String;
		
	/**
	 *  @private
     */
	private static var resourceInvalidDomainError:String;		
    
	/**
	 *  @private
     */
	private static var resourceInvalidCharError:String;

    /**
	 *  @private
     */
	private static var resourceWrongCAFormatError:String;

    /**
	 *  @private
     */
	private static var resourceWrongLengthError:String;

    /**
	 *  @private
     */
	private static var resourceWrongUSFormatError:String;
	
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
		resourceAllowedFormatChars =
			packageResources.getString("zipCodeValidatorAllowedFormatChars");

		resourceDomain =
			packageResources.getString("zipCodeValidatorDomain");
		
		resourceInvalidDomainError =
			packageResources.getString("invalidDomainErrorZCV");

		resourceInvalidCharError =
			packageResources.getString("invalidCharErrorZCV");	
			
		resourceWrongCAFormatError =
			packageResources.getString("wrongCAFormatError");

		resourceWrongLengthError =
			packageResources.getString("wrongLengthErrorZCV");

		resourceWrongUSFormatError =
			packageResources.getString("wrongUSFormatError");

		resourceInvalidFormatChars =
			packageResources.getString("invalidFormatCharsZCV");
	}
	
	/**
	 *  Convenience method for calling a validator.
	 *  Each of the standard Flex validators has a similar convenience method.
	 *
	 *  @param validator The ZipCodeValidator instance.
	 *
	 *  @param value A field to validate.
	 *
	 *  @param baseField Text representation of the subfield
	 *  specified in the <code>value</code> parameter.
	 *  For example, if the <code>value</code> parameter specifies value.zipCode,
	 *  the <code>baseField</code> value is <code>"zipCode"</code>.
     *
	 *  @return An Array of ValidationResult objects, with one ValidationResult 
	 *  object for each field examined by the validator. 
	 *
	 *  @see mx.validators.ValidationResult
	 *
	 */
    public static function validateZipCode(validator:ZipCodeValidator,
										   value:Object,
										   baseField:String):Array
    {
		var results:Array = [];
	
        var zip:String = String(value);
        var len:int = zip.length;
        
		var domainType:uint = DOMAIN_US;
        if (validator.domain == ZipCodeValidatorDomainType.US_OR_CANADA)
		{
            domainType = DOMAIN_US_OR_CANADA;
        }
        else if (validator.domain == ZipCodeValidatorDomainType.US_ONLY)
		{
            domainType = DOMAIN_US;
        }
        else
        {
			results.push(new ValidationResult(
				true, baseField, "invalidDomain",
				validator.invalidDomainError));
            return results;
        }

		var n:int;
		var i:int;
		
        // Make sure localAllowedFormatChars contains no numbers or letters.
        n = validator.allowedFormatChars.length;
		for (i = 0; i < n; i++)
        {
            if (DECIMAL_DIGITS.indexOf(validator.allowedFormatChars.charAt(i)) != -1 ||
                ROMAN_LETTERS.indexOf(validator.allowedFormatChars.charAt(i)) != -1)
            {
				throw new Error(resourceInvalidFormatChars);
            }
        }

        // Now start checking the ZIP code.
        // At present, only US and Canadian ZIP codes are supported.
		// As a result, the easiest thing to check first
		// to determine the domain is the length.
		// A length of 5 or 10 means a US ZIP code
		// and a length of 6 or 7 means a Canadian ZIP.
        // If more countries are supported in the future, it may make sense
		// to check other conditions first depending on the domain specified
		// and all the possible ZIP code formats for that domain.
		// For now, this approach makes the most sense.

        // Find out if the ZIP code contains any letters.
        var containsLetters:Boolean = false;
        for (i = 0; i < len; i++)
        {
            if (ROMAN_LETTERS.indexOf(zip.charAt(i)) != -1)
            {
                containsLetters = true;
                break;
            }
        }

        // Make sure there are no invalid characters in the ZIP.
        for (i = 0; i < len; i++)
        {
            var c:String = zip.charAt(i);
            
            if (ROMAN_LETTERS.indexOf(c) == -1 &&
                DECIMAL_DIGITS.indexOf(c)  == -1 &&
                validator.allowedFormatChars.indexOf(c) == -1)
            {
				results.push(new ValidationResult(
					true, baseField, "invalidChar",
					validator.invalidCharError));
                return results;
            }
        }

        if (len == 5 || len == 9 || len == 10) // US ZIP code
        {
            // Make sure the first 5 characters are all digits.
            for (i = 0; i < 5; i++)
            {
                if (DECIMAL_DIGITS.indexOf(zip.charAt(i)) == -1)
                {
					results.push(new ValidationResult(
						true, baseField, "wrongUSFormat",
						validator.wrongUSFormatError));
                    return results;
                }
            }
            
            if (len == 9 || len == 10)
            {
                if (len == 10)
                {
                    // Make sure the 6th character
					// is an allowed formatting character.
                    if (validator.allowedFormatChars.indexOf(zip.charAt(5)) == -1)
                    {
						results.push(new ValidationResult(
							true, baseField, "wrongUSFormat",
							validator.wrongUSFormatError));
                        return results;
                    }
                    i++;
                }
                
                // Make sure the remaining 4 characters are digits.
                for (; i < len; i++)
                {
                    if (DECIMAL_DIGITS.indexOf(zip.charAt(i)) == -1)
                    {
						results.push(new ValidationResult(
							true, baseField, "wrongUSFormat",
							validator.wrongUSFormatError));
                        return results;
                    }
                }
            }
        }

        else if (domainType == DOMAIN_US_OR_CANADA &&
				 containsLetters &&
				 (len == 6 || len == 7)) // Canadian zip code
        {
            i = 0;

            // Make sure the zip is in the form 'ldlfdld'
			// where l is a letter, d is a digit,
			// and f is an allowed formatting character.
            if (ROMAN_LETTERS.indexOf(zip.charAt(i++)) == -1 ||
                DECIMAL_DIGITS.indexOf(zip.charAt(i++)) == -1 ||
                ROMAN_LETTERS.indexOf(zip.charAt(i++)) == -1)
            {
				results.push(new ValidationResult(
					true, baseField, "wrongCAFormat",
					validator.wrongCAFormatError));
                return results;
            }
            
            if (len == 7 &&
				validator.allowedFormatChars.indexOf(zip.charAt(i++)) == -1)
            {
				results.push(new ValidationResult(
					true, baseField, "wrongCAFormat",
					validator.wrongCAFormatError));
                return results;
            }
            
            if (DECIMAL_DIGITS.indexOf(zip.charAt(i++)) == -1 ||
                ROMAN_LETTERS.indexOf(zip.charAt(i++)) == -1 ||
                DECIMAL_DIGITS.indexOf(zip.charAt(i++)) == -1)
            {
				results.push(new ValidationResult(
					true, baseField, "wrongCAFormat",
					validator.wrongCAFormatError));
                return results;
            }
        }
        
		else
        {
			results.push(new ValidationResult(
				true, baseField, "wrongLength",
				validator.wrongLengthError));
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
	public function ZipCodeValidator()
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

	[Inspectable(category="General", defaultValue=" -")]

	/** 
     *  The set of formatting characters allowed in the ZIP code.
	 *  This can not have digits or alphabets [a-z A-Z].
	 *
	 *  @default " -". 
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
			for (var i:int = 0; i < value.length; i++)
			{
				if (DECIMAL_DIGITS.indexOf(value.charAt(i)) != -1 ||
					ROMAN_LETTERS.indexOf(value.charAt(i)) != -1)
					throw new Error(resourceInvalidFormatChars);
			}
			_allowedFormatChars = value;
		}
	}

	//----------------------------------
	//  domain
	//----------------------------------

    [Inspectable(category="General", defaultValue="US Only")]

    /** 
     *  Type of ZIP code to check.
     *  In MXML, valid values are <code>"US or Canada"</code> 
     *  and <code>"US Only"</code>.
	 *
	 *  <p>In ActionScript, you can use the following constants to set this property: 
	 *  <code>ZipCodeValidatorDomainType.US_ONLY</code> and 
	 *  <code>ZipCodeValidatorDomainType.US_OR_CANADA</code>.</p>
	 *
	 *  @default ZipCodeValidatorDomainType.US_ONLY
     */
    public var domain:String;

	//--------------------------------------------------------------------------
	//
	//  Properties: Errors
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  invalidCharError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The ZIP code contains invalid characters.")]

    /** 
     *  Error message when the ZIP code contains invalid characters.
	 *
	 *  @default "The ZIP code contains invalid characters."
     */
    public var invalidCharError:String;

	//----------------------------------
	//  invalidDomainError
	//----------------------------------
    
    [Inspectable(category="Errors", defaultValue="The domain parameter is invalid. It must be either 'US Only' or 'US or Canada'.")]

    /** 
     *  Error message when the <code>domain</code> property contains an invalid value.
	 *
	 *  @default "The domain parameter is invalid. It must be either 'US Only' or 'US or Canada'."
     */
    public var invalidDomainError:String;
    
	//----------------------------------
	//  wrongCAFormatError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The Canadian postal code must be formatted 'A1B 2C3'.")]

    /** 
     *  Error message for an invalid Canadian postal code.
	 *
	 *  @default "The Canadian postal code must be formatted 'A1B 2C3'."
     */
    public var wrongCAFormatError:String;
	
	//----------------------------------
	//  wrongLengthError
	//----------------------------------
    
    [Inspectable(category="Errors", defaultValue="The ZIP code must be 5 digits or 5+4 digits.")]

    /** 
     *  Error message for an invalid US ZIP code.
	 *
	 *  @default "The ZIP code must be 5 digits or 5+4 digits."
     */
    public var wrongLengthError:String;
	
 	//----------------------------------
	//  wrongUSFormatError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The ZIP+4 code extension must be formatted '12345-6789'.")]

	/** 
     *  Error message for an incorrectly formatted ZIP code.
	 *
	 *  @default "The ZIP+4 code extension must be formatted '12345-6789'."
     */
    public var wrongUSFormatError:String;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------
   
    /**
     *  Override of the base class <code>doValidation()</code> method
     *  to validate a ZIP code.
     *
	 *  <p>You do not call this method directly;
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
		// or if the required property is set to false and length is 0.
		var val:String = value ? String(value) : "";
		if (results.length > 0 || ((val.length == 0) && !required))
			return results;
		else
		    return ZipCodeValidator.validateZipCode(this, value, null);
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
		domain = resourceDomain;

		invalidDomainError = resourceInvalidDomainError;
		invalidCharError = resourceInvalidCharError;
		wrongCAFormatError = resourceWrongCAFormatError;
		wrongLengthError = resourceWrongLengthError;
		wrongUSFormatError = resourceWrongUSFormatError;	
	}
}

}
