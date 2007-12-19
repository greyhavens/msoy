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
 *  The DateValidator class validates that a String or Object contains a 
 *  proper date and matches a specified format. Users can enter a single 
 *  digit or two digits for month, day, and year. 
 *  By default, the validator ensures the following formats:
 *
 *  <ul>
 *    <li>The month is between 1 and 12</li>
 *    <li>The day is between 1 and 31</li>
 *    <li>The year is a number</li>
 *  </ul>
 *
 *  <p>You can specify the date in the DateValidator class in two ways:</p>
 *  <ul>
 *    <li>Single String containing the date - Use the <code>source</code>
 *    and <code>property</code> properties to specify the String.
 *    The String can contain digits and the formatting characters
 *    specified by the <code>allowedFormatChars</code> property,
 *    which include the "/\-. " characters. 
 *    By default, the input format of the date in a String field
 *    is "MM/DD/YYYY" where "MM" is the month, "DD" is the day,
 *    and "YYYY" is the year. 
 *    You can use the <code>inputFormat</code> property
 *    to specify a different format.</li>
 *    <li>Object or multiple fields containing the day, month, and year.  
 *    Use all of the following properties to specify the day, month,
 *    and year inputs: <code>daySource</code>, <code>dayProperty</code>,
 *    <code>monthSource</code>, <code>monthProperty</code>,
 *    <code>yearSource</code>, and <code>yearProperty</code>.</li>
 *  </ul>
 *  
 *  @mxml
 *
 *  <p>The <code>&lt;mx:DateValidator&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds the following tag attributes:</p>  
 *  
 *  <pre>
 *  &lt;mx:DateValidator 
 *    allowedFormatChars="/\-. " 
 *    dayListener="<i>Object specified by daySource</i>"
 *    dayProperty="<i>No default</i>"
 *    daySource="<i>No default</i>"
 *    formatError= "Configuration error: Incorrect formatting string." 
 *    inputFormat="MM/DD/YYYY" 
 *    invalidCharError="Invalid characters in your date." 
 *    monthListener="<i>Object specified by monthSource</i>"
 *    monthProperty="<i>No default</i>"
 *    monthSource="<i>No default</i>"
 *    validateAsString="true|false"
 *    wrongDayError="Please enter a valid day for the month." 
 *    wrongLengthError="Please type the date in the format <i>inputFormat</i>." 
 *    wrongMonthError="Please enter a month between 1 and 12." 
 *    wrongYearError="Please enter a year between 0 and 9999." 
 *    yearListener="<i>Object specified by yearSource</i>"
 *    yearProperty="<i>No default</i>"
 *    yearSource="<i>No default</i>"
 *  /&gt;
 *  </pre>
 *  
 *  @includeExample examples/DateValidatorExample.mxml
 */
public class DateValidator extends Validator
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
	private static var resourceInputFormat:String;

    /**
	 *  @private    
     */	
	private static var resourceAllowedFormatChars:String;

    /**
	 *  @private    
     */	
	private static var resourceValidateAsString:Boolean;		

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
	private static var resourceWrongMonthError:String;

    /**
	 *  @private    
     */
	private static var resourceWrongDayError:String;

    /**
	 *  @private    
     */
	private static var resourceWrongYearError:String;

    /**
	 *  @private    
     */
	private static var resourceFormatError:String;
	
	/**
	 *  @private    
     */	
	private static var resourceInvalidFormatChars:String;	

	/**
	 *  @private    
     */	
	private static var resourceDSAttribute:String;

	/**
	 *  @private    
     */	
	private static var resourceMSAttribute:String;

	/**
	 *  @private    
     */	
	private static var resourceYSAttribute:String;

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
		resourceInputFormat = sharedResources.getString("dateFormat");
		
		resourceAllowedFormatChars =
			packageResources.getString("dateValidatorAllowedFormatChars");

		resourceValidateAsString =
			packageResources.getBoolean("validateAsString");

		resourceInvalidFormatChars =
			packageResources.getString("invalidFormatChars");
				
		resourceInvalidFormatChars =
			packageResources.getString("invalidFormatChars");
			
		resourceInvalidCharError = packageResources.getString("invalidCharErrorDV");

		resourceWrongLengthError = packageResources.getString("wrongLengthErrorDV");

		resourceWrongMonthError = packageResources.getString("wrongMonthError");

		resourceWrongDayError = packageResources.getString("wrongDayError");

		resourceWrongYearError = packageResources.getString("wrongYearError");

		resourceFormatError = packageResources.getString("formatError");

		resourceDSAttribute = packageResources.getString("DSAttribute");

		resourceMSAttribute = packageResources.getString("MSAttribute");

		resourceYSAttribute = packageResources.getString("YSAttribute");		
	}

	/**
	 *  Convenience method for calling a validator
	 *  from within a custom validation function.
	 *  Each of the standard Flex validators has a similar convenience method.
	 *
	 *  @param validator The DateValidator instance.
	 *
	 *  @param value A field to validate.
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
	public static function validateDate(validator:DateValidator,
									    value:Object,
										baseField:String):Array
	{
		var results:Array = [];
	
		var validInput:String =  DECIMAL_DIGITS + validator.allowedFormatChars;
		
		var dateObj:Object = {};
		dateObj.month = "";
		dateObj.day = "";
		dateObj.year = "";
		
		var dayProp:String = baseField;
		var yearProp:String = baseField;
		var monthProp:String = baseField;

		var advanceValueCounter:Boolean = true;
		var monthRequired:Boolean = false;
		var dayRequired:Boolean = false
		var yearRequired:Boolean = false;
		var valueIsString:Boolean = false;
		var foundMonth:Boolean = false;
		var foundYear:Boolean = false;
		
		var objValue:Object;
		var stringValue:Object;
	
		var n:int;
		var i:int;
		var temp:String;
		
		n = validator.allowedFormatChars.length;
		for (i = 0; i < n; i++)
		{
			if (DECIMAL_DIGITS.indexOf(validator.allowedFormatChars.charAt(i)) != -1)
				throw new Error(resourceInvalidFormatChars);
		}
		
		if (value is String)
		{
			valueIsString = true;
			stringValue = String(value);
		}
		else if (value is Number)
		{
			valueIsString = true;
			stringValue = String(value);
		}
		else
		{
			objValue = value;
		}

		// Check if the validator is an object or a string.
		if (!validator.validateAsString || !valueIsString)
		{
    		var baseFieldDot:String = baseField ? baseField + "." : "";
			dayProp = baseFieldDot + "day";
			yearProp = baseFieldDot + "year";
			monthProp = baseFieldDot + "month";
			
            if (validator.required && (!objValue.month || objValue.month == ""))
            {
                results.push(new ValidationResult(
					true, monthProp,"requiredField",
					validator.requiredFieldError));
            }
            else if (isNaN(objValue.month))
			{
				results.push(new ValidationResult(
					true, monthProp, "wrongMonth",
					validator.wrongMonthError));
			}
			else
			{
				monthRequired = true;
			}
			
            if (validator.required && (!objValue.year || objValue.year == ""))
            {
                results.push(new ValidationResult(
					true, yearProp, "requiredField",
					validator.requiredFieldError));
            }
            else if (isNaN(objValue.year))
			{
				results.push(new ValidationResult(
					true, yearProp, "wrongYear",
					validator.wrongYearError));
			}
			else
			{
				yearRequired = true;
			}
			
			var dayMissing:Boolean = (!objValue.day || objValue.day == "");
			var dayInvalid:Boolean = dayMissing || isNaN(objValue.day);
			var dayWrong:Boolean = !dayMissing && isNaN(objValue.day);
			var dayOptional:Boolean = yearRequired && monthRequired;			
			
			// If the validator is required and there is no day specified			
            if (validator.required && dayMissing)
            {
                results.push(new ValidationResult(
					true, dayProp, "requiredField",
					validator.requiredFieldError));
            }
			else if (!dayInvalid) // The day is valid (a number).
			{
				dayRequired = true;
			}
			else if (!dayOptional || dayWrong) // Day is not optional and is NaN.
			{
				results.push(new ValidationResult(
					true, dayProp, "wrongDay",
					validator.wrongDayError));
			}

			dateObj.month = objValue.month ? String(objValue.month) : "";
			dateObj.day = objValue.day ? String(objValue.day) : "";
			dateObj.year = objValue.year ? String(objValue.year) : "";
		}
		else
		{
			var result:ValidationResult = DateValidator.validateFormatString(
					validator, validator.inputFormat, baseField);
			if (result != null)
			{
				results.push(result);
				return results;
			}
			else
			{
				var len:Number = stringValue.length;
				if (len > validator.inputFormat.length ||
					len + 2 < validator.inputFormat.length)
				{
					results.push(new ValidationResult(
						true, baseField, "wrongLength",
						validator.wrongLengthError + " " +
						validator.inputFormat));
					return results;
				}
 
 				var j:int = 0;
				n = validator.inputFormat.length;
				for (i = 0; i < n; i++)
				{
					temp = "" + stringValue.substring(j, j + 1);
					var mask:String = "" +
									  validator.inputFormat.substring(i, i + 1);
					
					// Check each character to see if it is allowed.
					if (validInput.indexOf(temp) == -1)
					{
						results.push(new ValidationResult(
							true, baseField, "invalidChar",
							validator.invalidCharError));
						return results;
					}
					if (mask == "m" || mask == "M")
					{
						monthRequired = true;
						if (isNaN(Number(temp)))
							advanceValueCounter = false;
						else
							dateObj.month += temp;
					}
					else if (mask == "d" || mask == "D")
					{
						dayRequired = true;
						if (isNaN(Number(temp)))
							advanceValueCounter = false;
						else
							dateObj.day += temp;
					}
					else if (mask == "y" || mask == "Y")
					{
						yearRequired = true;
						if (isNaN(Number(temp)))
						{
							results.push(new ValidationResult(
								true, baseField, "wrongLength", 
								validator.wrongLengthError + " " +
								validator.inputFormat));
							return results;
						}
						else
						{
							dateObj.year += temp;
						}
					}
					else if (validator.allowedFormatChars.indexOf(temp) == -1)
					{
						results.push(new ValidationResult(
								true, baseField, "invalidChar", 
								validator.invalidCharError));
							return results;
					}
					
					if (advanceValueCounter)
						j++;
					advanceValueCounter = true;	
				}
				
				if ((monthRequired && dateObj.month == "") ||
					(dayRequired && dateObj.day == "") ||
					(yearRequired && dateObj.year == "") ||
					(j != len))
				 {
				 	results.push(new ValidationResult(
						true, baseField, "wrongLength", 
						validator.wrongLengthError + " " +
						validator.inputFormat));
					return results;
				 }
			}
		}

		// Now, validate the sub-elements, which may have been set directly.
		n = dateObj.month.length;
		for (i = 0; i < n; i++)
		{
			temp = "" + dateObj.month.substring(i, i + 1);
			if (DECIMAL_DIGITS.indexOf(temp) == -1)
			{
				results.push(new ValidationResult(
					true, monthProp, "invalidChar",
					validator.invalidCharError));
			}
		}
		n = dateObj.day.length;
		for (i = 0; i < n; i++)
		{
			temp = "" + dateObj.day.substring(i, i + 1);
			if (DECIMAL_DIGITS.indexOf(temp) == -1)
			{
				results.push(new ValidationResult(
					true, dayProp, "invalidChar",
					validator.invalidCharError));
			}
		}
		n = dateObj.year.length;
		for (i = 0; i < n; i++)
		{
			temp = "" + dateObj.year.substring(i, i + 1);
			if (DECIMAL_DIGITS.indexOf(temp) == -1)
			{
				results.push(new ValidationResult(
					true, yearProp, "invalidChar",
					validator.invalidCharError));
			}
		}

		if (results.length > 0)
			return results;
		
		var monthNum:Number = Number(dateObj.month);
		var dayNum:Number = Number(dateObj.day);
		var yearNum:Number = Number(dateObj.year).valueOf();

		if (monthNum > 12 || monthNum < 1)
		{
			results.push(new ValidationResult(
				true, monthProp, "wrongMonth",
				validator.wrongMonthError));
			return results;
		}

		var maxDay:Number = 31;

		if (monthNum == 4 || monthNum == 6 ||
			monthNum == 9 || monthNum == 11)
		{
			maxDay = 30;
		}
		else if (monthNum == 2)
		{
			if (yearNum % 4 > 0)
				maxDay = 28;
			else if (yearNum % 100 == 0 && yearNum % 400 > 0)
				maxDay = 28;
			else
				maxDay = 29;
		}

		if (dayRequired && (dayNum > maxDay || dayNum < 1))
		{
			results.push(new ValidationResult(
				true, dayProp, "wrongDay",
				validator.wrongDayError));
			return results;
		}

		if (yearRequired && (yearNum > 9999 || yearNum < 0))
		{
			results.push(new ValidationResult(
				true, yearProp, "wrongYear",
				validator.wrongYearError));
			return results;
		}

		return results;
	}

	/**
	 *  @private
	 */
	private static function validateFormatString(
								validator:DateValidator,
								format:String,
								baseField:String):ValidationResult
	{
		var monthCounter:Number = 0;
		var dayCounter:Number = 0;
		var yearCounter:Number = 0;
		
		var n:int = format.length;
		for (var i:int = 0; i < n; i++)
		{
			var mask:String = "" + format.substring(i, i + 1);
			
			// Check for upper and lower case to maintain backwards compatibility.
			if (mask == "m" || mask == "M")
				monthCounter++;
			else if (mask == "d" || mask == "D")
				dayCounter++;
			else if (mask == "y" || mask == "Y")
				yearCounter++;
		}

		if ((monthCounter == 2 &&
			 (yearCounter == 2 || yearCounter == 4)) ||
			(monthCounter == 2 && dayCounter == 2 &&
			 (yearCounter == 0 || yearCounter == 2 || yearCounter == 4)))
		{
			return null; // Passes format validation
		}
		else
		{
			return new ValidationResult(
				true, baseField, "format",
				validator.formatError);
		}
	}

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------
	
	/**
	 *  Constructor.
	 */
	public function DateValidator()
	{
		super();
		
		bundleChanged();
		
		subFields = [ "day", "month", "year" ];
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
	 *  for the day, month and year subfields.
	 */
	override protected function get actualListeners():Array
	{
		var results:Array = [];
	
		var dayResult:Object;
		if (_dayListener)
			dayResult = _dayListener;
		else if (_daySource)
			dayResult = _daySource;
			
		if (dayResult)
		{
			results.push(dayResult);
			if (dayResult is IValidatorListener)
				IValidatorListener(dayResult).validationSubField = "day";
		}
		
		var monthResult:Object;
		if (_monthListener)
			monthResult = _monthListener;
		else if (_monthSource)
			monthResult = _monthSource;
			
		if (monthResult)
		{
			results.push(monthResult);
			if (monthResult is IValidatorListener)
				IValidatorListener(monthResult).validationSubField = "month";
		}
		
		var yearResult:Object;
		if (_yearListener)
			yearResult = _yearListener;
		else if (_yearSource)
			yearResult = _yearSource;
			
		if (yearResult)
		{
			results.push(yearResult);
			if (yearResult is IValidatorListener)
				IValidatorListener(yearResult).validationSubField = "year";
		}
		
		if (results.length > 0 && listener)
			results.push(listener);
		else
			results = results.concat(super.actualListeners);
		
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

	[Inspectable(category="General", defaultValue="\\/-. ")]

	/** 
	 *  The set of formatting characters allowed for separating
	 *  the month, day, and year values.
	 *
	 *  @default "/\-. "
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
	//  dayListener
	//----------------------------------
	
	/**
	 *  @private
	 *  Storage for the dayListener property.
	 */
	private var _dayListener:IValidatorListener;
	
	[Inspectable(category="General")]

	/** 
	 *  The component that listens for the validation result
	 *  for the day subfield.
	 *  If none is specified, use the value specified
	 *  for the <code>daySource</code> property.
	 */
	public function get dayListener():IValidatorListener
	{
		return _dayListener;
	}
	
	/**
	 *  @private
	 */
	public function set dayListener(value:IValidatorListener):void
	{
		if (_dayListener == value)
			return;
			
		removeListenerHandler();	
			
		_dayListener = value;
		
		addListenerHandler();
	}
	
	//----------------------------------
	//  dayProperty
	//----------------------------------
	
	[Inspectable(category="General")]
	
	/**
	 *  Name of the day property to validate. 
	 *  This property is optional, but if you specify the
	 *  <code>daySource</code> property, you should also set this property.
	 */
	public var dayProperty:String;
	
	//----------------------------------
	//  daySource
	//----------------------------------
	
	/**
	 *  @private
	 *  Storage for the daySource property.
	 */
	private var _daySource:Object;
	
	[Inspectable(category="General")]

	/** 
	 *  Object that contains the value of the day field.
	 *  If you specify a value for this property, you must also
	 *  specify a value for the <code>dayProperty</code> property. 
	 *  Do not use this property if you set the <code>source</code> 
	 *  and <code>property</code> properties. 
	 */
	public function get daySource():Object
	{
		return _daySource;
	}
	
	/**
	 *  @private
	 */
	public function set daySource(value:Object):void
	{
		if (_daySource == value)
			return;
		
		if (value is String)
			throw new Error(StringUtil.substitute(resourceDSAttribute, value));
			
		removeListenerHandler();	
			
		_daySource = value;
		
		addListenerHandler();
	}
	
	//----------------------------------
	//  inputFormat
	//----------------------------------

	[Inspectable(category="General", defaultValue="MM/DD/YYYY")]

	/** 
	 *  The date format to validate the value against.
	 *  "MM" is the month, "DD" is the day, and "YYYY" is the year.
	 *  This String is case-sensitive.
	 *
	 *  @default "MM/DD/YYYY"
	 */
	public var inputFormat:String;

	//----------------------------------
	//  monthListener
	//----------------------------------		
	
	/**
	 *  @private
	 *  Storage for the monthListener property.
	 */
	private var _monthListener:IValidatorListener; 
	
	[Inspectable(category="General")]

	/** 
	 *  The component that listens for the validation result
	 *  for the month subfield. 
	 *  If none is specified, use the value specified
	 *  for the <code>monthSource</code> property.
	 */
	public function get monthListener():IValidatorListener
	{
		return _monthListener;
	}
	
	/**
	 *  @private
	 */
	public function set monthListener(value:IValidatorListener):void
	{
		if (_monthListener == value)
			return;
			
		removeListenerHandler();	
			
		_monthListener = value;
		
		addListenerHandler();
	}
	
	//----------------------------------
	//  monthProperty
	//----------------------------------	

	[Inspectable(category="General")]

	/**
	 *  Name of the month property to validate. 
	 *  This property is optional, but if you specify the
	 *  <code>monthSource</code> property, you should also set this property.
	 */
	public var monthProperty:String;
		
	//----------------------------------
	//  monthSource
	//----------------------------------	
	
	/**
	 *  @private
	 *  Storage for the monthSource property.
	 */
	private var _monthSource:Object;
	
	[Inspectable(category="General")]

	/** 
	 *  Object that contains the value of the month field.
	 *  If you specify a value for this property, you must also specify
	 *  a value for the <code>monthProperty</code> property. 
	 *  Do not use this property if you set the <code>source</code> 
	 *  and <code>property</code> properties. 
	 */
	public function get monthSource():Object
	{
		return _monthSource;
	}
	
	/**
	 *  @private
	 */
	public function set monthSource(value:Object):void
	{
		if (_monthSource == value)
			return;
		
		if (value is String)
			throw new Error(StringUtil.substitute(resourceMSAttribute, value));
			
		removeListenerHandler();	
			
		_monthSource = value;
		
		addListenerHandler();
	}
		
	//----------------------------------
	//  validateAsString
	//----------------------------------

	[Inspectable(category="General", defaultValue="true")]

	/** 
	 *  Determines how to validate the value.
	 *  If set to <code>true</code>, the validator evaluates the value
	 *  as a String, unless the value has a <code>month</code>,
	 *  <code>day</code>, or <code>year</code> property.
	 *  If <code>false</code>, the validator evaluates the value
	 *  as a Date object. 
	 *
	 *  @default true	 
	 */
	public var validateAsString:Boolean;

	//----------------------------------
	//  yearListener
	//----------------------------------
	
	/**
	 *  @private
	 *  Storage for the yearListener property.
	 */
	private var _yearListener:IValidatorListener; 
	
	[Inspectable(category="General")]

	/** 
	 *  The component that listens for the validation result
	 *  for the year subfield. 
	 *  If none is specified, use the value specified
	 *  for the <code>yearSource</code> property.
	 */
	public function get yearListener():IValidatorListener
	{
		return _yearListener;
	}
	
	/**
	 *  @private
	 */
	public function set yearListener(value:IValidatorListener):void
	{
		if (_yearListener == value)
			return;
			
		removeListenerHandler();	
			
		_yearListener = value;
		
		addListenerHandler();
	}
	
	//----------------------------------
	//  yearProperty
	//----------------------------------

	[Inspectable(category="General")]

	/**
	 *  Name of the year property to validate. 
	 *  This property is optional, but if you specify the
	 *  <code>yearSource</code> property, you should also set this property.
	 */
	 public var yearProperty:String;
	
	//----------------------------------
	//  yearSource
	//----------------------------------
	
	/**
	 *  @private
	 *  Storage for the yearSource property.
	 */
	private var _yearSource:Object;
	
	[Inspectable(category="General")]

	/** 
	 *  Object that contains the value of the year field.
	 *  If you specify a value for this property, you must also specify
	 *  a value for the <code>yearProperty</code> property. 
	 *  Do not use this property if you set the <code>source</code> 
	 *  and <code>property</code> properties. 
	 */
	public function get yearSource():Object
	{
		return _yearSource;
	}

	/**
	 *  @private
	 */
	public function set yearSource(value:Object):void
	{
		if (_yearSource == value)
			return;
		
		if (value is String)
			throw new Error(StringUtil.substitute(resourceYSAttribute, value));
			
		removeListenerHandler();	
			
		_yearSource = value;
		
		addListenerHandler();
	}
		
	//--------------------------------------------------------------------------
	//
	//  Properties: Errors
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  formatError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Configuration error: Incorrect formatting string")]

	/** 
	 *  Error message when the <code>inputFormat</code> property
	 *  is not in the correct format.
	 *
	 *  @default "Configuration error: Incorrect formatting string." 
	 */
	public var formatError:String;
	
	//----------------------------------
	//  invalidCharError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Invalid characters in your date.")]

	/** 
	 *  Error message when there are invalid characters in the date.
	 *
	 *  @default "Invalid characters in your date."
	 */
	public var invalidCharError:String;
	
	//----------------------------------
	//  wrongDayError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Please enter a valid day for the month")]

	/** 
	 *  Error message when the day is invalid.
	 *
	 *  @default "Please enter a valid day for the month." 
	 */
	public var wrongDayError:String;
	
	//----------------------------------
	//  wrongLengthError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Please type the date in the format")]

	/** 
	 *  Error message when the length of the date
	 *  doesn't match that of the <code>inputFormat</code> property.
	 *
	 *  @default "Please type the date in the format <i>inputFormat</i>." 
	 */
	public var wrongLengthError:String;

	//----------------------------------
	//  wrongMonthError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Please enter a month between 1 and 12")]

	/** 
	 *  Error message when the month is invalid.
	 *
	 *  @default "Please enter a month between 1 and 12."
	 */
	public var wrongMonthError:String;
	
	//----------------------------------
	//  wrongYearError
	//----------------------------------

	[Inspectable(category="Errors", defaultValue="Please enter a year between 0 and 9999")]

	/** 
	 *  Error message when the year is invalid.
	 *
	 *  @default "Please enter a year between 0 and 9999."
	 */	
	public var wrongYearError:String;
	
	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
     *  Override of the base class <code>doValidation()</code> method
     *  to validate a date.
	 *
	 *  <p>You do not call this method directly;
	 *  Flex calls it as part of performing a validation.
	 *  If you create a custom validator class, you must implement this method. </p>
	 *
	 *  @param value Either a String or an Object to validate.
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
		    return DateValidator.validateDate(this, value, null);
    }
	
	/**
	 *  @private
	 *  Grabs the data for the validator from three different sources.
	 */
	override protected function getValueFromSource():Object
	{
		var useValue:Boolean = false;
	
		var value:Object = {};
		
		if (daySource && dayProperty)
		{
			value.day = daySource[dayProperty];
			useValue = true;
		}
		
		if (monthSource && monthProperty)
		{
			value.month = monthSource[monthProperty];
			useValue = true;
		}
		
		if (yearSource && yearProperty)
		{
			value.year = yearSource[yearProperty];
			useValue = true;
		}
		
		return useValue ? value : super.getValueFromSource();
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
		inputFormat = resourceInputFormat;
		allowedFormatChars = resourceAllowedFormatChars;
		validateAsString = resourceValidateAsString;
				
		invalidCharError = resourceInvalidCharError;
		wrongLengthError = resourceWrongLengthError;
		wrongMonthError = resourceWrongMonthError;
		wrongDayError = resourceWrongDayError;
		wrongYearError = resourceWrongYearError;
		formatError = resourceFormatError;
	}
}

}
