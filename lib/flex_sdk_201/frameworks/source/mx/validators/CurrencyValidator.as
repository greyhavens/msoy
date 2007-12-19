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
 *  The CurrencyValidator class ensures that a String
 *  represents a valid currency expression.
 *  It can make sure the input falls within a given range
 *  (specified by <code>minValue</code> and <code>maxValue</code>),
 *  is non-negative (specified by <code>allowNegative</code>),
 *  and does not exceed the specified <code>precision</code>. The 
 *  CurrencyValidator class correctly validates formatted and unformatted
 *  currency expressions, e.g., "$12,345.00" and "12345".
 *  You can customize the <code>currencySymbol</code>, <code>alignSymbol</code>,
 *  <code>thousandsSeparator</code>, and <code>decimalSeparator</code>
 *  properties for internationalization.
 *
 *  @mxml
 *
 *  <p>The <code>&lt;mx:CurrencyValidator&gt;</code> tag
 *  inherits all of the tag properties of its superclass,
 *  and adds the following tag properties:</p>
 *
 *  <pre>
 *  &lt;mx:CurrencyValidator
 *    alignSymbol="left|right|any"
 *    allowNegative="true|false"
 *    currencySymbol="$"
 *    currencySymbolError="The currency symbol occurs in an invalid location."
 *    decimalPointCountError="The decimal separator can only occur once."
 *    decimalSeparator="."
 *    exceedsMaxError="The amount entered is too large."
 *    invalidCharError="The input contains invalid characters."
 *    invalidFormatCharsError="One of the formatting parameters is invalid."
 *    lowerThanMinError="The amount entered is too small."
 *    maxValue="NaN"
 *    minValue="NaN"
 *    negativeError="The amount may not be negative."
 *    precision="2"
 *    precisionError="The amount entered has too many digits beyond the decimal point."
 *    separationError="The thousands separator must be followed by three digits."
 *    thousandsSeparator=","
 *  /&gt;
 *  </pre>
 *
 *  @see mx.validators.CurrencyValidatorAlignSymbol
 *
 *  @includeExample examples/CurrencyValidatorExample.mxml
 */
public class CurrencyValidator extends Validator
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
	 *  Formatting characters for negative values.
	 */
	private static const NEGATIVE_FORMATTING_CHARS:String = "-()";

	//--------------------------------------------------------------------------
	//
	//  Class resources
	//
	//--------------------------------------------------------------------------

	[ResourceBundle("SharedResources")]

    /**
	 *  @private
	 */
	private static var sharedResources:ResourceBundle;

	[ResourceBundle("validators")]

    /**
	 *  @private
	 */
	private static var packageResources:ResourceBundle;

    /**
	 *  @private
	 */
	private static var resourceAlignSymbol:String;

    /**
	 *  @private
	 */
	private static var resourceAllowNegative:Boolean;

    /**
	 *  @private
	 */
	private static var resourceCurrencySymbol:String;

    /**
	 *  @private
	 */
	private static var resourceDecimalSeparator:String;

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
	private static var resourceCurrencySymbolError:String;

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
		resourceAlignSymbol = sharedResources.getString("alignSymbol");

		resourceCurrencySymbol = sharedResources.getString("currencySymbol");
		
		resourceAllowNegative = packageResources.getBoolean("allowNegative");

		resourceDecimalSeparator =
			packageResources.getString("decimalSeparator");

		resourceMaxValue = packageResources.getNumber("maxValue");

		resourceMinValue = packageResources.getNumber("minValue");

		resourcePrecision =
			packageResources.getNumber("currencyValidatorPrecision");

		resourceThousandsSeparator =
			packageResources.getString("thousandsSeparator");

		resourceCurrencySymbolError =
			packageResources.getString("currencySymbolError");

		resourceDecimalPointCountError = 
			packageResources.getString("decimalPointCountError");

		resourceExceedsMaxError = packageResources.getString("exceedsMaxErrorCV");

		resourceInvalidCharError = packageResources.getString("invalidCharError");

		resourceInvalidFormatCharsError =
			packageResources.getString("invalidFormatCharsError");

		resourceLowerThanMinError =
			packageResources.getString("lowerThanMinError");

		resourceNegativeError = packageResources.getString("negativeError");

		resourcePrecisionError = packageResources.getString("precisionError");

		resourceSeparationError = packageResources.getString("separationError");
	}
	
    /**
     *  Convenience method for calling a validator.
	 *  Each of the standard Flex validators has a similar convenience method.
	 *
     *  @param validator The CurrencyValidator instance.
	 *
	 *  @param value The object to validate.
	 *
	 *  @param baseField Text representation of the subfield
	 *  specified in the <code>value</code> parameter.
	 *  For example, if the <code>value</code> parameter specifies value.currency,
	 *  the baseField value is "currency".
	 *
	 *  @return An Array of ValidationResult objects, with one ValidationResult 
	 *  object for each field examined by the validator. 
	 *
	 *  @see mx.validators.ValidationResult
     */
    public static function validateCurrency(validator:CurrencyValidator,
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
		// are not digits or negative formatting characters,
		// and that the separators are one character.
        var invalidFormChars:String = DECIMAL_DIGITS + NEGATIVE_FORMATTING_CHARS;

        if (validator.currencySymbol == validator.thousandsSeparator ||
            validator.currencySymbol == validator.decimalSeparator ||
            validator.decimalSeparator == validator.thousandsSeparator ||
            invalidFormChars.indexOf(validator.currencySymbol) != -1 ||
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
        var validChars:String = DECIMAL_DIGITS + NEGATIVE_FORMATTING_CHARS +
								validator.currencySymbol +
								validator.decimalSeparator +
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
            // Check if negative input is allowed.
            if (!validator.allowNegative)
            {
                results.push(new ValidationResult(
					true, baseField, "negative",
					validator.negativeError));
				return results;
            }

            // Strip off the negative formatting and update some variables.
            input = input.substring(1);
            len--;
            isNegative = true;
        }

        else if (input.charAt(0) == "(")
        {
            // Make sure the last character is a closed parenthesis.
            if (input.charAt(len - 1) != ")")
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

            // Strip off the negative formatting and update some variables.
            input = input.substring(1,len-2);
            len -= 2;
            isNegative = true;
        }

        // Find the currency symbol if it exists,
		// then make sure that it's in the right place
		// and that there is only one.
        if ((input.charAt(0) == validator.currencySymbol &&
			 validator.alignSymbol == CurrencyValidatorAlignSymbol.RIGHT) ||
            (input.charAt(len - 1) == validator.currencySymbol &&
			 validator.alignSymbol == CurrencyValidatorAlignSymbol.LEFT) ||
            (len > 2 &&
			 input.substring(1, len - 2).indexOf(validator.currencySymbol) != -1) ||
            (input.indexOf(validator.currencySymbol) !=
			 input.lastIndexOf(validator.currencySymbol)))
        {
            results.push(new ValidationResult(
				true, baseField, "currencySymbol",
				validator.currencySymbolError));
			return results;
        }

        // Now that we know it's in the right place,
		// strip off the currency symbol if it exists.
        var currencySymbolIndex:int = input.indexOf(validator.currencySymbol);
        if (currencySymbolIndex != -1)
        {
            if (currencySymbolIndex) // if it's at the end
                input = input.substring(0, len - 2);
            else // it's at the beginning
                input = input.substring(1);
            len--;
        }

        // Make sure there is only one decimal point.
        if (input.indexOf(validator.decimalSeparator) !=
			input.lastIndexOf(validator.decimalSeparator))
        {
            results.push(new ValidationResult(
				true, baseField, "decimalPointCount",
				validator.decimalPointCountError));
			return results;
        }

        // Make sure that every character after the decimal point
		// is a digit and that the precision is not exceeded.
        var decimalSeparatorIndex:int = input.indexOf(validator.decimalSeparator);
        var numDigitsAfterDecimal:int = 0;

        // If there is no decimal separator, act like there is one at the end.
        if (decimalSeparatorIndex == -1)
          decimalSeparatorIndex = len;

        for (i = decimalSeparatorIndex + 1; i < len; i++)
        {
			if (DECIMAL_DIGITS.indexOf(input.charAt(i)) == -1)
			{
				results.push(new ValidationResult(
					true, baseField, "invalidChar",
					validator.invalidCharError));
				return results;
			}

			++numDigitsAfterDecimal;

			// Make sure precision is not exceeded.
			if (validator.precision != -1 && numDigitsAfterDecimal > validator.precision)
			{
				results.push(new ValidationResult(
					true, baseField, "precision",
					validator.precisionError));
				return results;
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
        // If it's a thousands separator, make sure it's followed
		// by three consecutive digits, and then make sure the next character
		// is valid (i.e., either thousands separator, decimal separator,
		// or nothing).
        var validGroupEnder:String = validator.thousandsSeparator +
									 validator.decimalSeparator;
        for (i = 1; i<decimalSeparatorIndex; i++)
        {
            c = input.charAt(i);

            if (c == validator.thousandsSeparator)
            {
                if (input.substring(i + 1, i + 4).length < 3 ||
                    DECIMAL_DIGITS.indexOf(input.charAt(i + 1)) == -1 ||
                    DECIMAL_DIGITS.indexOf(input.charAt(i + 2)) == -1 ||
                    DECIMAL_DIGITS.indexOf(input.charAt(i + 3)) == -1 ||
                    validGroupEnder.indexOf(input.charAt(i + 4)) == -1)
                {
                	results.push(new ValidationResult(
						true, baseField, "separation",
						validator.separationError));
					return results;
                }
            }
            else if (DECIMAL_DIGITS.indexOf(c) == -1)
            {
                results.push(new ValidationResult(
					true, baseField, "invalidChar",
					validator.invalidCharError));
				return results;
            }
        }

        // Make sure the input is within the specified range.
        if (!isNaN(validator.minValue) || !isNaN(validator.maxValue))
        {
            // First strip off the thousands separators.
            for (i = 0; i < decimalSeparatorIndex; i++)
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
    public function CurrencyValidator()
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
	//  alignSymbol
	//----------------------------------

	[Inspectable(category="General", defaultValue="left")]

    /**
     *  Specifies the alignment of the <code>currencySymbol</code>
	 *  relative to the rest of the expression.
	 *  Acceptable values in ActionScript are <code>CurrencyValidatorAlignSymbol.LEFT</code>, 
	 *  <code>CurrencyValidatorAlignSymbol.RIGHT</code>, and 
	 *  <code>CurrencyValidatorAlignSymbol.ANY</code>.
	 *  Acceptable values in MXML are <code>"left"</code>, 
	 *  <code>"right"</code>, and 
	 *  <code>"any"</code>.
	 * 
	 *  @default CurrencyValidatorAlignSymbol.LEFT
     *
     *  @see mx.validators.CurrencyValidatorAlignSymbol
     */
	public var alignSymbol:String;

	//----------------------------------
	//  allowNegative
	//----------------------------------
	
    [Inspectable(category="General", defaultValue="true")]

    /**
     *  Specifies whether negative numbers are permitted.
	 *  Can be <code>true</code> or <code>false</code>.
	 *  
	 *  @default true
     */
	public var allowNegative:Boolean;

	//----------------------------------
	//  currencySymbol
	//----------------------------------

	[Inspectable(category="General", defaultValue="$")]

    /**
     *  The single-character String used to specify the currency symbol, 
     *  such as "$" or "&#163;".
	 *  Cannot be a digit and must be distinct from the
     *  <code>thousandsSeparator</code> and the <code>decimalSeparator</code>.
     *
	 *  @default "$"
     */
	public var currencySymbol:String;

	//----------------------------------
	//  decimalSeparator
	//----------------------------------

    [Inspectable(category="General", defaultValue=".")]

    /**
     *  The character used to separate the whole
	 *  from the fractional part of the number.
	 *  Cannot be a digit and must be distinct from the
     *  <code>currencySymbol</code> and the <code>thousandsSeparator</code>.
	 *  
	 *  @default "."
     */	
	public var decimalSeparator:String;

	//----------------------------------
	//  maxValue
	//----------------------------------

    [Inspectable(category="General", defaultValue="NaN")]

    /**
     *  Maximum value for a valid number.
	 *  A value of NaN means it is ignored.
	 *  
	 *  @default NaN
     */
	public var maxValue:Number;

	//----------------------------------
	//  minValue
	//----------------------------------

    [Inspectable(category="General", defaultValue="NaN")]

    /**
     *  Minimum value for a valid number.
	 *  A value of NaN means it is ignored.
	 *  
	 *  @default NaN
     */
	public var minValue:Number;

	//----------------------------------
	//  precision
	//----------------------------------

    [Inspectable(category="General", defaultValue="2")]

    /**
     *  The maximum number of digits allowed to follow the decimal point.
	 *  Can be any non-negative integer.
	 *  Note: Setting to <code>0</code>
     *  has the same effect as setting <code>NumberValidator.domain</code>
	 *  to <code>int</code>.
	 *	Setting it to -1, means it is ignored.
	 * 
	 *  @default 2
     */
	public var precision:int;

	//----------------------------------
	//  thousandsSeparator
	//----------------------------------

    [Inspectable(category="General", defaultValue=",")]

    /**
     *  The character used to separate thousands.
	 *  Cannot be a digit and must be distinct from the
     *  <code>currencySymbol</code> and the <code>decimalSeparator</code>.
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
	//  currencySymbolError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The currency symbol occurs in an invalid location.")]

    /**
     *  Error message when the currency symbol, defined by <code>currencySymbol</code>,
     *  is in the wrong location.
	 *  
	 *  @default "The currency symbol occurs in an invalid location."
     */
	public var currencySymbolError:String;

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

    [Inspectable(category="Errors", defaultValue="The amount entered is too large.")]

    /**
     *  Error message when the value is greater than <code>maxValue</code>.
	 *  
	 *  @default "The amount entered is too large."
     */
	public var exceedsMaxError:String;

	//----------------------------------
	//  invalidCharError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The input contains invalid characters.")]

    /**
     *  Error message when the currency contains invalid characters.
	 *  
	 *  @default "The input contains invalid characters."
     */	
	public var invalidCharError:String;

	//----------------------------------
	//  invalidFormatCharsError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="One of the formatting parameters is invalid.")]

    /**
     *  Error message when the value contains an invalid formatting character.
	 *  
	 *  @default "One of the formatting parameters is invalid."
     */
	public var invalidFormatCharsError:String;

	//----------------------------------
	//  lowerThanMinError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The amount entered is too small.")]

    /**
     *  Error message when the value is less than <code>minValue</code>.
	 *  
	 *  @default "The amount entered is too small."
     */
	public var lowerThanMinError:String;

	//----------------------------------
	//  negativeError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The amount may not be negative.")]

    /**
     *  Error message when the value is negative and
     *  the <code>allowNegative</code> property is <code>false</code>.
	 *  
	 *  @default "The amount may not be negative."
     */
	public var negativeError:String;

	//----------------------------------
	//  precisionError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The amount entered has too many digits beyond the decimal point.")]

    /**
     *  Error message when the value has a precision that exceeds the value
     *  defined by the <code>precision</code> property.
	 *  
	 *  @default "The amount entered has too many digits beyond 
	 *  the decimal point."
     */
	public var precisionError:String;

	//----------------------------------
	//  separationError
	//----------------------------------

    [Inspectable(category="Errors", defaultValue="The thousands separator must be followed by three digits.")]

    /**
     *  Error message when the thousands separator is incorrectly placed.
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
     *  to validate a currency expression.
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
		    return CurrencyValidator.validateCurrency(this, value, null);
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
		alignSymbol = resourceAlignSymbol;
		allowNegative = resourceAllowNegative;		
		currencySymbol = resourceCurrencySymbol;
		decimalSeparator = resourceDecimalSeparator;
		maxValue = resourceMaxValue;
		minValue = resourceMinValue;
		precision = resourcePrecision;
		thousandsSeparator = resourceThousandsSeparator;
		
		currencySymbolError = resourceCurrencySymbolError;
		decimalPointCountError = resourceDecimalPointCountError;
		exceedsMaxError = resourceExceedsMaxError;
		invalidCharError = resourceInvalidCharError;
		invalidFormatCharsError = resourceInvalidFormatCharsError;
		lowerThanMinError = resourceLowerThanMinError;
		negativeError = resourceNegativeError;
		precisionError = resourcePrecisionError;
		separationError = resourceSeparationError;
	}
}

}
