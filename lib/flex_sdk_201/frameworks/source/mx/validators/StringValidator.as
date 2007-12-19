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

/**
 *  The StringValidator class validates that the length of a String 
 *  is within a specified range. 
 *  
 *  @mxml
 *  
 *  <p>The <code>&lt;mx:StringValidator&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and add the following tag attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:StringValidator
 *    maxLength="NaN" 
 *    minLength="NaN" 
 *    tooLongError="This string is longer than the maximum allowed length." 
 *    tooShortError="This string is shorter than the minimum allowed length." 
 *  /&gt;
 *  </pre>
 *  
 *  @includeExample examples/StringValidatorExample.mxml
 */
public class StringValidator extends Validator
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
	private static var resourceMaxLength:Number;

    /**
	 *  @private    
     */
	private static var resourceMinLength:Number;

    /**
	 *  @private    
     */
	private static var resourceTooLongError:String;

    /**
	 *  @private    
     */
	private static var resourceTooShortError:String;

    /**
	 *  @private    
     */
	private var isTooLongErrorSet:Boolean;

    /**
	 *  @private    
     */
	private var isTooShortErrorSet:Boolean;

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
		resourceMaxLength = packageResources.getNumber("maxLength");

		resourceMinLength = packageResources.getNumber("minLength");

		resourceTooLongError = packageResources.getString("tooLongError");

		resourceTooShortError = packageResources.getString("tooShortError");
	}
	
	/**
	 *  Convenience method for calling a validator.
	 *  Each of the standard Flex validators has a similar convenience method.
	 *
	 *  @param validator The StringValidator instance.
	 *
	 *  @param value A field to validate.
	 *
	 *  @param baseField Text representation of the subfield
	 *  specified in the <code>value</code> parameter.
	 *  For example, if the <code>value</code> parameter specifies
	 *  value.mystring, the <code>baseField</code> value
	 *  is <code>"mystring"</code>.
     *
	 *  @return An Array of ValidationResult objects, with one
	 *  ValidationResult  object for each field examined by the validator. 
	 *
	 *  @see mx.validators.ValidationResult
	 */
	public static function validateString(validator:StringValidator,
										  value:Object,
										  baseField:String = null):Array
	{
		var results:Array = [];
		
		var val:String = value != null ? String(value) : "";

		if (!isNaN(validator.maxLength) && (val.length > validator.maxLength))
		{
			results.push(new ValidationResult(
				true, baseField, "tooLong",
				validator.isTooLongErrorSet ? validator.tooLongError :
				StringUtil.substitute(validator.tooLongError, validator.maxLength)));
			return results;
		}

		if (!isNaN(validator.minLength) && (val.length < validator.minLength))
		{
			results.push(new ValidationResult(
				true, baseField, "tooShort",
				validator.isTooShortErrorSet ? validator.tooShortError :
				StringUtil.substitute(validator.tooShortError, validator.minLength)));
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
	public function StringValidator()
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
	//  maxLength
	//----------------------------------

	[Inspectable(category="General", defaultValue="NaN")]

	/** 
	 *  Maximum length for a valid String. 
	 *  A value of NaN means this property is ignored.
	 *
	 *  @default NaN
	 */
	public var maxLength:Number;
	
	//----------------------------------
	//  minLength
	//----------------------------------

	[Inspectable(category="General", defaultValue="NaN")]

	/** 
	 *  Minimum length for a valid String.
	 *  A value of NaN means this property is ignored.
	 *
	 *  @default NaN
	 */
	public var minLength:Number;

	//--------------------------------------------------------------------------
	//
	//  Properties: Errors
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  tooLongError
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the tooLongError property.
	 */
	private var _tooLongError:String;

    [Inspectable(category="Errors", defaultValue="This string is longer than the maximum allowed length. This must be less than {0} characters long.")]

	/** 
	 *  Error message when the String is longer
	 *  than the <code>maxLength</code> property.
	 *
	 *  @default "This String is longer than the maximum allowed length."
	 */
	public function get tooLongError():String 
	{
		return _tooLongError;
	}

	/*
	 *  @private
	 */
	public function set tooLongError(value:String):void
    {
		isTooLongErrorSet = true;
        _tooLongError = value;
    }

	//----------------------------------
	//  tooShortError
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the tooShortError property.
	 */
	private var _tooShortError:String;
	
	[Inspectable(category="Errors", defaultValue="This string is shorter than the minimum allowed length. This must be at least {0} characters long.")]

	/** 
	 *  Error message when the string is shorter
	 *  than the <code>minLength</code> property.
	 *
	 *  @default "This String is shorter than the minimum allowed length."
	 */
	public function get tooShortError():String 
	{
		return _tooShortError;
	}

	/*
	 *  @private
	 */
	public function set tooShortError(value:String):void
    {
		isTooShortErrorSet = true;
        _tooShortError = value;
    }
	
	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
     *  Override of the base class <code>doValidation()</code> method
     *  to validate a String.
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
		// or if the required property is set to false and length is 0.
		var val:String = value ? String(value) : "";
		if (results.length > 0 || ((val.length == 0) && !required))
			return results;
		else
		    return StringValidator.validateString(this, value, null);
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
		maxLength = resourceMaxLength;
		minLength = resourceMinLength;
		_tooLongError = resourceTooLongError;
		_tooShortError = resourceTooShortError;			
		isTooLongErrorSet = false;
		isTooShortErrorSet = false;
	}
}

}
