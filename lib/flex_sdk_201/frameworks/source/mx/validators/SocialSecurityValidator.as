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
 *  The SocialSecurityValidator class validates that a String
 *  is a valid United States Social Security number.
 *  It does not check whether it is an existing Social Security number.
 *
 *  @mxml
 *
 *  <p>The <code>&lt;mx:SocialSecurityValidator&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds the following tag attributes:</p>
 *
 *  <pre>
 *  &lt;mx:SocialSecurityValidator
 *    allowedFormatChars=" -"
 *    invalidCharError="You entered invalid characters in your Social Security number."
 *    wrongFormatError="The Social Security number must be 9 digits or in the form NNN-NN-NNNN."
 *    zeroStartError="Invalid Social Security number: the number cannot start with 000."
 *  /&gt;
 *  </pre>
 *
 *  @includeExample examples/SocialSecurityValidatorExample.mxml
 */
public class SocialSecurityValidator extends Validator
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
	private static var resourceWrongFormatError:String;

    /**
	 *  @private
     */
	private static var resourceZeroStartError:String;

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
			packageResources.getString("socialSecurityValidatorAllowedFormatChars");

		resourceInvalidFormatChars =
			packageResources.getString("invalidFormatChars");

		resourceInvalidCharError =
			packageResources.getString("invalidCharErrorSSV");

		resourceWrongFormatError =
			packageResources.getString("wrongFormatError");

		resourceZeroStartError =
			packageResources.getString("zeroStartError");
	}

	/**
	 *  Convenience method for calling a validator.
	 *  Each of the standard Flex validators has a similar convenience method.
	 *
	 *  @param validator The SocialSecurityValidator instance.
	 *
	 *  @param value A field to validate.
	 *
	 *  @param baseField Text representation of the subfield
	 *  specified in the <code>value</code> parameter.
	 *  For example, if the <code>value</code> parameter specifies
	 *  value.social, the <code>baseField</code> value is <code>social</code>.
	 *
	 *  @return An Array of ValidationResult objects, with one ValidationResult
	 *  object for each field examined by the validator.
	 *
	 *  @see mx.validators.ValidationResult
	 */
	public static function validateSocialSecurity(
								validator:SocialSecurityValidator,
								value:Object,
								baseField:String):Array
	{
		var results:Array = [];

		var hyphencount:int = 0;
		var len:int = value.toString().length;
		var checkForFormatChars:Boolean = false;

		var n:int;
		var i:int;

		if ((len != 9) && (len != 11))
		{
			results.push(new ValidationResult(
				true, baseField, "wrongFormat",
				validator.wrongFormatError));
			return results;
		}

		n = validator.allowedFormatChars.length;
		for (i = 0; i < n; i++)
		{
			if (DECIMAL_DIGITS.indexOf(validator.allowedFormatChars.charAt(i)) != -1)
				throw new Error(resourceInvalidFormatChars);
		}

		if (len == 11)
			checkForFormatChars = true;

		for (i = 0; i < len; i++)
		{
			var allowedChars:String;
			if (checkForFormatChars && (i == 3 || i == 6))
				allowedChars = validator.allowedFormatChars;
			else
				allowedChars = DECIMAL_DIGITS;

			if (allowedChars.indexOf(value.charAt(i)) == -1)
			{
				results.push(new ValidationResult(
					true, baseField, "invalidChar",
					validator.invalidCharError));
				return results;
			}
		}

		if (value.substring(0, 3) == "000")
		{
			results.push(new ValidationResult(
				true, baseField, "zeroStart",
				validator.zeroStartError));
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
	public function SocialSecurityValidator()
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
	 *  Specifies the set of formatting characters allowed in the input.
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

	[Inspectable(category="Errors", defaultValue="You entered invalid characters in your Social Security number.")]

	/**
	 *  Error message when the value contains characters
	 *  other than digits and formatting characters
	 *  defined by the <code>allowedFormatChars</code> property.
	 *
	 *  @default "You entered invalid characters in your Social Security number."
	 */
	public var invalidCharError:String;

	//----------------------------------
	//  wrongFormatError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="The Social Security number must be 9 digits or in the form NNN-NN-NNNN.")]

	/**
	 *  Error message when the value is incorrectly formatted.
	 *
	 *  @default "The Social Security number must be 9 digits or in the form NNN-NN-NNNN."
	 */
	public var wrongFormatError:String;

	//----------------------------------
	//  zeroStartError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Invalid Social Security number: the number cannot start with 000.")]

	/**
	 *  Error message when the value contains an invalid Social Security number.
	 *
	 *  @default "Invalid Social Security number: the number cannot start with 000."
	 */
	public var zeroStartError:String;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
     *  Override of the base class <code>doValidation()</code> method
     *  to validate a Social Security number.
     *
	 *  <p>You do not call this method directly;
	 *  Flex calls it as part of performing a validation.
	 *  If you create a custom Validator class, you must implement this method.</p>
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
		    return SocialSecurityValidator.validateSocialSecurity(this, value, null);
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
		wrongFormatError = resourceWrongFormatError;
		zeroStartError = resourceZeroStartError;
	}
}

}
