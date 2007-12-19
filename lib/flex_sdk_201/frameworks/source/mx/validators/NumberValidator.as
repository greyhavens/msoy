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
 *  The NumberValidator class ensures that a String represents a valid number.
 *  It can ensure that the input falls within a given range
 *  (specified by <code>minValue</code> and <code>maxValue</code>),
 *  is an integer (specified by <code>domain</code>),
 *  is non-negative (specified by <code>allowNegative</code>),
 *  and does not exceed the specified <code>precision</code>.
 *  The validator correctly validates formatted numbers (e.g., "12,345.67")
 *  and you can customize the <code>thousandsSeparator</code> and
 *  <code>decimalSeparator</code> properties for internationalization.
 *  
 *  @mxml
 *
 *  <p>The <code>&lt;mx:NumberValidator&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds the following tag attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:NumberValidator 
 *    allowNegative="true|false" 
 *    decimalPointCountError="The decimal separator can only occur once." 
 *    decimalSeparator="." 
 *    domain="real|int" 
 *    exceedsMaxError="The number entered is too large." 
 *    integerError="The number must be an integer." 
 *    invalidCharError="The input contains invalid characters." 
 *    invalidFormatCharsError="One of the formatting parameters is invalid." 
 *    lowerThanMinError="The number entered is too small." 
 *    maxValue="NaN" 
 *    minValue="NaN" 
 *    negativeError="The number may not be negative." 
 *    precision="-1" 
 *    precisionError="The number entered has too many digits beyond the decimal point." 
 *    separationError="The thousands separator must be followed by three digits." 
 *    thousandsSeparator="," 
 *  /&gt;
 *  </pre>
 *  
 *  @includeExample examples/NumberValidatorExample.mxml
 */
public class NumberValidator extends Validator
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
	private static var resourceAllowNegative:Boolean;

    /**
	 *  @private
     */	
	private static var resourceDecimalSeparator:String;

    /**
	 *  @private
     */	
	private static var resourceDomain:String;

    /**
	 *  @private
     */	
	private static var resourceMaxValue:Number;

    /**
	 *  @private
     */	
	private static var resourceMinValue:Number;

    /**
	 *  @private
     */	
	private static var resourcePrecision:Number;

    /**
	 *  @private
     */	
	private static var resourceThousandsSeparator:String;

    /**
	 *  @private    
     */
	private static var resourceDecimalPointCountError:String;

    /**
	 *  @private    
     */
	private static var resourceExceedsMaxError:String;

    /**
	 *  @private    
     */
	private static var resourceIntegerError:String;

    /**
	 *  @private    
     */
	private static var resourceInvalidCharError:String;

    /**
	 *  @private    
     */
	private static var resourceInvalidFormatCharsError:String;

    /**
	 *  @private    
     */
	private static var resourceLowerThanMinError:String;

    /**
	 *  @private    
     */
	private static var resourceNegativeError:String;

    /**
	 *  @private    
     */
	private static var resourcePrecisionError:String;

    /**
	 *  @private    
     */
	private static var resourceSeparationError:String;

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
		resourceAllowNegative = packageResources.getBoolean("allowNegative"); 

		resourceDecimalSeparator =
			packageResources.getString("decimalSeparator");

		resourceDomain = packageResources.getString("numberValidatorDomain");

		resourceMaxValue = packageResources.getNumber("maxValue");

		resourceMinValue = packageResources.getNumber("minValue");

		resourcePrecision =
			packageResources.getNumber("numberValidatorPrecision");

		resourceThousandsSeparator =
			packageResources.getString("thousandsSeparator");

		resourceDecimalPointCountError =
			packageResources.getString("decimalPointCountError");

		resourceExceedsMaxError = packageResources.getString("exceedsMaxErrorNV");

		resourceIntegerError = packageResources.getString("integerError");

		resourceInvalidCharError =
			packageResources.getString("invalidCharError");

		resourceInvalidFormatCharsError =
			packageResources.getString("invalidFormatCharsError");

		resourceLowerThanMinError =
			packageResources.getString("lowerThanMinError");

		resourceNegativeError = packageResources.getString("negativeError");

		resourcePrecisionError = packageResources.getString("precisionError");

		resourceSeparationError = packageResources.getString("separationError");
	}
	
    /**
     *  Convenience method for calling a validator
	 *  from within a custom validation function.
	 *  Each of the standard Flex validators has a similar convenience method.
	 *
	 *  @param validator The NumberValidator instance.
	 *
	 *  @param value A field to validate.
	 *
     *  @param baseField Text representation of the subfield
	 *  specified in the <code>value</code> parameter.
	 *  For example, if the <code>value</code> parameter specifies value.number,
	 *  the <code>baseField</code> value is "number".
	 *
	 *  @return An Array of ValidationResult objects, with one ValidationResult 
	 *  object for each field examined by the validator. 
	 *
	 *  @see mx.validators.ValidationResult
     */
    public static function validateNumber(validator:NumberValidator,
										  value:Object,
										  baseField:String):Array
    {
		var results:Array = [];
	
        var input:String = String(value);
        var len:int = input.length;

        var isNegative:Boolean = false;
		
		var i:int;
		var c:String;

        // Make sure the formatting character parameters are unique,
		// are not digits or the negative sign,
		// and that the separators are one character.
        var invalidFormChars:String = DECIMAL_DIGITS + "-";

        if (validator.decimalSeparator == validator.thousandsSeparator ||
            invalidFormChars.indexOf(validator.decimalSeparator) != -1 ||
            invalidFormChars.indexOf(validator.thousandsSeparator) != -1 ||
            validator.decimalSeparator.length != 1 ||
            validator.thousandsSeparator.length != 1)
        {
            results.push(new ValidationResult(
				true, baseField, "invalidFormatChar",
				validator.invalidFormatCharsError));
			return results;
        }

        // Check for invalid characters in input.
        var validChars:String = DECIMAL_DIGITS + "-" + validator.decimalSeparator +
								validator.thousandsSeparator;
        for (i = 0; i < len; i++)
        {
            c = input.charAt(i);
            if (validChars.indexOf(c) == -1)
            {
                results.push(new ValidationResult(
					true, baseField, "invalidChar",
					validator.invalidCharError));
				return results;
            }
        }

        // Check if the input is negative.
        if (input.charAt(0) == "-")
        {
			if (len == 1) // we have only '-' char
			{
                results.push(new ValidationResult(
					true, baseField, "invalidChar",
					validator.invalidCharError));
				return results;
			}
			else if (len == 2 && input.charAt(1) == '.') // handle "-."
			{
                results.push(new ValidationResult(
					true, baseField, "invalidChar",
					validator.invalidCharError));
				return results;
			}

            // Check if negative input is allowed.
            if (!validator.allowNegative)
            {
                results.push(new ValidationResult(
					true, baseField, "negative",
					validator.negativeError));
				return results;
            }

            // Strip off the minus sign, update some variables.
            input = input.substring(1);
            len--;
            isNegative = true;
        }

        // Make sure there's only one decimal point.
        if (input.indexOf(validator.decimalSeparator) !=
			input.lastIndexOf(validator.decimalSeparator))
        {
            results.push(new ValidationResult(
				true, baseField, "decimalPointCount",
				validator.decimalPointCountError));
			return results;
        }

        // Make sure every character after the decimal is a digit,
		// and that there aren't too many digits after the decimal point:
        // if domain is int there should be none,
		// otherwise there should be no more than specified by precision.
        var decimalSeparatorIndex:Number =
			input.indexOf(validator.decimalSeparator);
        if (decimalSeparatorIndex != -1)
        {
            var numDigitsAfterDecimal:Number = 0;

			if (i == 1 && i == len) // we only have a '.'
			{
            	results.push(new ValidationResult(
					true, baseField, "invalidChar",
					validator.invalidCharError));
				return results;
			}
			
            for (i = decimalSeparatorIndex + 1; i < len; i++)
            {
                // This character must be a digit.
                if (DECIMAL_DIGITS.indexOf(input.charAt(i)) == -1)
                {
                    results.push(new ValidationResult(
						true, baseField, "invalidChar",
						validator.invalidCharError));
					return results;
                }

                ++numDigitsAfterDecimal;

                // There may not be any non-zero digits after the decimal
				// if domain is int.
                if (validator.domain == "int" && input.charAt(i) != "0")
                {
                    results.push(new ValidationResult(
						true, baseField,"integer",
						validator.integerError));
					return results;
                }

                // Make sure precision is not exceeded.
                if (validator.precision != -1 &&
					numDigitsAfterDecimal > validator.precision)
                {
                    results.push(new ValidationResult(
						true, baseField, "precision",
						validator.precisionError));
					return results;
                }
            }
        }

        // Make sure the input begins with a digit or a decimal point.
        if (DECIMAL_DIGITS.indexOf(input.charAt(0)) == -1 &&
			input.charAt(0) != validator.decimalSeparator)
        {
            results.push(new ValidationResult(
				true, baseField, "invalidChar",
				validator.invalidCharError));
			return results;
        }

        // Make sure that every character before the decimal point
		// is a digit or is a thousands separator.
        // If it's a thousands separator,
		// make sure it's followed by three consecutive digits.
        var end:int = decimalSeparatorIndex == -1 ?
					  len :
					  decimalSeparatorIndex;
        for (i = 1; i < end; i++)
        {
            c = input.charAt(i);
            if (c == validator.thousandsSeparator)
            {
                if (c == validator.thousandsSeparator)
                {
                    if ((end - i != 4 &&
						 input.charAt(i + 4) != validator.thousandsSeparator) ||
                        DECIMAL_DIGITS.indexOf(input.charAt(i + 1)) == -1 ||
                        DECIMAL_DIGITS.indexOf(input.charAt(i + 2)) == -1 ||
                        DECIMAL_DIGITS.indexOf(input.charAt(i + 3)) == -1)
                    {
                        results.push(new ValidationResult(
							true, baseField, "separation",
							validator.separationError));
						return results;
                    }
                }
            }
            else if (DECIMAL_DIGITS.indexOf(c) == -1)
            {
                results.push(new ValidationResult(
					true, baseField,"invalidChar",
					validator.invalidCharError));
				return results;
            }
        }

        // Make sure the input is within the specified range.
        if (!isNaN(validator.minValue) || !isNaN(validator.maxValue))
        {
            // First strip off the thousands separators.
            for (i = 0; i < end; i++)
            {
                if (input.charAt(i) == validator.thousandsSeparator)
                {
                    var left:String = input.substring(0, i);
                    var right:String = input.substring(i + 1);
                    input = left + right;
                }
            }

            // Check bounds

            var x:Number = Number(input);

            if (isNegative)
                x = -x;

            if (!isNaN(validator.minValue) && (x < validator.minValue))
            {
                results.push(new ValidationResult(
					true, baseField, "lowerThanMin",
					validator.lowerThanMinError));
				return results;
            }
            
			if (!isNaN(validator.maxValue) && (x > validator.maxValue))
            {
                results.push(new ValidationResult(
					true, baseField, "exceedsMax",
					validator.exceedsMaxError));
				return results;
            }
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
	public function NumberValidator()
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
	//  allowNegative
	//----------------------------------

    [Inspectable(category="General", defaultValue="true")]

    /**
     *  Specifies whether negative numbers are permitted.
	 *  Valid values are <code>true</code> or <code>false</code>.
	 *
	 *  @default true
     */
	public var allowNegative:Boolean;

	//----------------------------------
	//  decimalSeparator
	//----------------------------------

    [Inspectable(category="General", defaultValue=".")]

    /**
     *  The character used to separate the whole
	 *  from the fractional part of the number.
	 *  Cannot be a digit and must be distinct from the
     *  <code>thousandsSeparator</code>.
	 *
	 *  @default "."
     */	
	public var decimalSeparator:String;

	//----------------------------------
	//  domain
	//----------------------------------

    [Inspectable(category="General", enumeration="int,real", defaultValue="real")]

    /**
     *  Type of number to be validated.
	 *  Permitted values are <code>"real"</code> and <code>"int"</code>.
	 *
	 *  @default "real"
     */
    public var domain:String;
	
	//----------------------------------
	//  maxValue
	//----------------------------------
	
    /**
	 *  @private
	 */
	private var maxValueSet:Boolean = false;

    [Inspectable(category="General", defaultValue="NaN")]

    /**
     *  Maximum value for a valid number. A value of NaN means there is no maximum.
	 *
	 *  @default NaN
     */
	public var maxValue:Number;

	//----------------------------------
	//  minValue
	//----------------------------------

    [Inspectable(category="General", defaultValue="NaN")]

    /**
     *  Minimum value for a valid number. A value of NaN means there is no minimum.
	 *
	 *  @default NaN
     */
	public var minValue:Number;
	
	//----------------------------------
	//  precision
	//----------------------------------

    [Inspectable(category="General", defaultValue="-1")]

    /**
     *  The maximum number of digits allowed to follow the decimal point.
	 *  Can be any nonnegative integer. 
	 *  Note: Setting to <code>0</code> has the same effect
	 *  as setting <code>domain</code> to <code>"int"</code>.
	 *  A value of -1 means it is ignored.
	 *
	 *  @default -1
     */
	public var precision:int; 
	
 	//----------------------------------
	//  thousandsSeparator
	//----------------------------------

    [Inspectable(category="General", defaultValue=",")]

    /**
     *  The character used to separate thousands
	 *  in the whole part of the number.
	 *  Cannot be a digit and must be distinct from the
     *  <code>decimalSeparator</code>.
	 *
	 *  @default ","
     */
	public var thousandsSeparator:String;
	
	//--------------------------------------------------------------------------
	//
	//  Properties: Errors
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  decimalPointCountError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The decimal separator can only occur once.")]

    /**
     *  Error message when the decimal separator character occurs more than once.
	 *
	 *  @default "The decimal separator can only occur once."
     */
	public var decimalPointCountError:String;
	
	//----------------------------------
	//  exceedsMaxError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The number entered is too large.")]

    /**
     *  Error message when the value exceeds the <code>maxValue</code> property.
	 *
	 *  @default "The number entered is too large."
     */
	public var exceedsMaxError:String;
	
	//----------------------------------
	//  integerError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The number must be an integer.")]

    /**
     *  Error message when the number must be an integer, as defined 
     * by the <code>domain</code> property.
	 *
	 *  @default "The number must be an integer."
     */
	public var integerError:String;

	//----------------------------------
	//  invalidCharError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The input contains invalid characters.")]

    /**
     *  Error message when the value contains invalid characters.
	 *
	 *  @default The input contains invalid characters."
     */	
	public var invalidCharError:String;

	//----------------------------------
	//  invalidFormatCharsError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="One of the formatting parameters is invalid.")]

    /**
     *  Error message when the value contains invalid format characters, which means that 
     *  it contains a digit or minus sign (-) as a separator character, 
     *  or it contains two or more consecutive separator characters.
	 *
	 *  @default "One of the formatting parameters is invalid."
     */
	public var invalidFormatCharsError:String;

	//----------------------------------
	//  lowerThanMinError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The number entered is too small.")]

    /**
     *  Error message when the value is less than <code>minValue</code>.
	 *
	 *  @default "The number entered is too small."
     */
	public var lowerThanMinError:String;

	//----------------------------------
	//  negativeError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The number may not be negative.")]

    /**
     *  Error message when the value is negative and the 
     *  <code>allowNegative</code> property is <code>false</code>.
	 *
	 *  @default "The number may not be negative."
     */
	public var negativeError:String;

	//----------------------------------
	//  precisionError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The number entered has too many digits beyond the decimal point.")]

    /**
     *  Error message when the value has a precision that exceeds the value defined 
     *  by the precision property.
	 *
	 *  @default "The number entered has too many digits beyond the decimal point."
     */
	public var precisionError:String;

	//----------------------------------
	//  separationError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The thousands separator must be followed by three digits.")]

    /**
     *  Error message when the thousands separator is in the wrong location.
	 *
	 *  @default "The thousands separator must be followed by three digits."
     */
	public var separationError:String;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

    /**
     *  Override of the base class <code>doValidation()</code> method 
     *  to validate a number.
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
		// or if the required property is set to <code>false</code> and length is 0.
		var val:String = value ? String(value) : "";
		if (results.length > 0 || ((val.length == 0) && !required))
			return results;
		else
		    return NumberValidator.validateNumber(this, value, null);
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
		allowNegative = resourceAllowNegative;		
		decimalSeparator = resourceDecimalSeparator;
		domain = resourceDomain;	
		maxValue = resourceMaxValue;
		minValue = resourceMinValue;
		precision = resourcePrecision;
		thousandsSeparator = resourceThousandsSeparator;

		decimalPointCountError = resourceDecimalPointCountError;
		exceedsMaxError = resourceExceedsMaxError;
		integerError = resourceIntegerError;
		invalidCharError = resourceInvalidCharError;
		invalidFormatCharsError = resourceInvalidFormatCharsError;
		lowerThanMinError = resourceLowerThanMinError;
		negativeError = resourceNegativeError;
		precisionError = resourcePrecisionError;
		separationError = resourceSeparationError;
	}
}

}
