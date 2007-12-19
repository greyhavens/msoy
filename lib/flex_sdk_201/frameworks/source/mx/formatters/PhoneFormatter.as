////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.formatters
{

import mx.managers.ISystemManager;
import mx.managers.SystemManager;

/**
 *  The PhoneFormatter class formats a valid number into a phone number format,
 *  including international configurations.
 *
 *  <p>A shortcut is provided for the United States seven-digit format.
 *  If the <code>areaCode</code> property contains a value
 *  and you use the seven-digit format string, (###-####),
 *  a seven-digit value to format automatically adds the area code
 *  to the returned String.
 *  The default format for the area code is (###). 
 *  You can change this using the <code>areaCodeFormat</code> property. 
 *  You can format the area code any way you want as long as it contains 
 *  three number placeholders.</p>
 *
 *  <p>If an error occurs, an empty String is returned and a String
 *  that describes the error is saved to the <code>error</code> property.
 *  The <code>error</code> property can have one of the following values:</p>
 *
 *  <ul>
 *    <li><code>"Invalid value"</code> means an invalid numeric value is passed 
 *    to the <code>format()</code> method. The value should be a valid number 
 *    in the form of a Number or a String, or the value contains a different 
 *    number of digits than what is specified in the format String.</li>
 *    <li> <code>"Invalid format"</code> means any of the characters in the 
 *    <code>formatString</code> property do not match the allowed characters 
 *    specified in the <code>validPatternChars</code> property, 
 *    or the <code>areaCodeFormat</code> property is specified but does not
 *    contain exactly three numeric placeholders.</li>
 *  </ul>
 *  
 *  @mxml
 *  
 *  <p>The <code>&lt;mx:PhoneFormatter&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds the following tag attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:PhoneFormatter
 *    areaCode "-1"
 *    areaCodeFormat "(###)"
 *    formatString="(###) ###-####"
 *    validPatternChars "+()#-. "
 *  />
 *  </pre>
 *  
 *  @includeExample examples/PhoneFormatterExample.mxml
 *  
 *  @see mx.formatters.SwitchSymbolFormatter
 */
public class PhoneFormatter extends Formatter
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
		
    /**
	 *  @private    
     */	
	private static var resourceAreaCode:Number;

    /**
	 *  @private    
     */	
	private static var resourceAreaCodeFormat:String;

    /**
	 *  @private    
     */	
	private static var resourceFormatString:String;

    /**
	 *  @private    
     */	
	private static var resourceValidPatternChars:String;
	
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
		// packageResources was loaded by Formatter superclass.

		resourceAreaCode = packageResources.getNumber("areaCode");

		resourceAreaCodeFormat = packageResources.getString("areaCodeFormat");

		resourceFormatString = packageResources.getString("phoneNumberFormat");

		resourceValidPatternChars =
			packageResources.getString("validPatternChars");
	}
	
	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function PhoneFormatter()
	{
		super();

		bundleChanged();
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  areaCode
	//----------------------------------
	
	[Inspectable(category="General", defaultValue="-1")]

	/**
	 *  Area code number added to a seven-digit United States
	 *  format phone number to form a 10-digit phone number.
	 *  A value of <code>-1</code> means do not  
	 *  prepend the area code.
	 *
	 *  @default -1	 
	 */
	public var areaCode:int;

	//----------------------------------
	//  areaCodeFormat
	//----------------------------------

	[Inspectable(category="General", defaultValue="(###) ")]

	/**
	 *  Default format for the area code when the <code>areacode</code>
	 *  property is rendered by a seven-digit format.
	 *
	 *  @default "(###) "
	 */
	public var areaCodeFormat:String;

	//----------------------------------
	//  formatString
	//----------------------------------

	[Inspectable(category="General", defaultValue="(###) ###-####")]
	
	/**
	 *  String that contains mask characters
	 *  that represent a specified phone number format.
	 *
	 *  @default "(###) ###-####"
	 */
	public var formatString:String;

	//----------------------------------
	//  validPatternChars
	//----------------------------------

	[Inspectable(category="General", defaultValue="+()#-. ")]

	/**
	 *  List of valid characters that can be used
	 *  in the <code>formatString</code> property.
	 *  This property is used during validation
	 *  of the <code>formatString</code> property.
	 *
	 *  @default "+()#- ."
	 */
	public var validPatternChars:String;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------
	
	/**
	 *  Formats the String as a phone number.
	 *  If the value cannot be formatted, return an empty String 
	 *  and write a description of the error to the <code>error</code> property.
	 *
	 *  @param value Value to format.
	 *
	 *  @return Formatted String. Empty if an error occurs. A description 
	 *  of the error condition is written to the <code>error</code> property.
	 */
	override public function format(value:Object):String
	{
		// Reset any previous errors.
		if (error)
			error = null;

		// --value--

		if (!value || String(value).length == 0 || isNaN(Number(value)))
		{
			error = defaultInvalidValueError;
			return "";
		}

		// --length--

		var fStrLen:int = 0;
		var letter:String;
		var n:int;
		var i:int;
		
		n = formatString.length;
		for (i = 0; i < n; i++)
		{
			letter = formatString.charAt(i);
			if (letter == "#")
			{
				fStrLen++;
			}
			else if (validPatternChars.indexOf(letter) == -1)
			{
				error = defaultInvalidFormatError;
				return "";
			}
		}

		if (String(value).length != fStrLen)
		{
			error = defaultInvalidValueError;
			return "";
		}

		// --format--

		var fStr:String = formatString;

		if (fStrLen == 7 && areaCode != -1)
		{
			var aCodeLen:int = 0;
			n = areaCodeFormat.length;
			for (i = 0; i < n; i++)
				if (areaCodeFormat.charAt(i) == "#")
					aCodeLen++;
			if (aCodeLen == 3 && String(areaCode).length == 3)
			{
				fStr = String(areaCodeFormat).concat(fStr);
				value = String(areaCode).concat(value);
			}
		}

		var dataFormatter:SwitchSymbolFormatter = new SwitchSymbolFormatter();

		return dataFormatter.formatValue(fStr, value);
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
		areaCode = resourceAreaCode;
		areaCodeFormat = resourceAreaCodeFormat;
		formatString = resourceFormatString;
		validPatternChars = resourceValidPatternChars;
	}
}

}
