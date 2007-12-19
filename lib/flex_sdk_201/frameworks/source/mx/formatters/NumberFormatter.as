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
import mx.resources.ResourceBundle;

/**
 *  The NumberFormatter class formats a valid number
 *  by adjusting the decimal rounding and precision,
 *  the thousands separator, and the negative sign.
 *
 *  <p>If you use both the <code>rounding</code> and <code>precision</code>
 *  properties, rounding is applied first, and then you set the decimal length
 *  by using the specified <code>precision</code> value.
 *  This lets you round a number and still have a trailing decimal;
 *  for example, 303.99 = 304.00.</p>
 *
 *  <p>If an error occurs, an empty String is returned and a String
 *  describing  the error is saved to the <code>error</code> property.
 *  The <code>error</code>  property can have one of the following values:</p>
 *
 *  <ul>
 *    <li><code>"Invalid value"</code> means an invalid numeric value is passed to 
 *    the <code>format()</code> method. The value should be a valid number in the 
 *    form of a Number or a String.</li>
 *    <li><code>"Invalid format"</code> means one of the parameters
 *    contain an unusable setting.</li>
 *  </ul>
 *  
 *  @mxml
 *  
 *  <p>The <code>&lt;mx:NumberFormatter&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds the following tag attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:NumberFormatter
 *    decimalSeparatorFrom="."
 *    decimalSeparatorTo="."
 *    precision="-1"
 *    rounding="none|up|down|nearest"
 *    thousandsSeparatorFrom=","
 *    thousandsSeparatorTo=","
 *    useNegativeSign="true|false"
 *    useThousandsSeparator="true|false"/>  
 *  </pre>
 *  
 *  @includeExample examples/NumberFormatterExample.mxml
 *  
 *  @see mx.formatters.NumberBase
 *  @see mx.formatters.NumberBaseRoundType
 */
public class NumberFormatter extends Formatter
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

	/**
	 *  @private
     */	
	private static var resourceDecimalSeparatorFrom:String;

	/**
	 *  @private
	 */
	private static var resourceDecimalSeparatorTo:String;

	/**
	 *  @private
	 */
	private static var resourcePrecision:int;

	/**
	 *  @private
	 */
	private static var resourceRounding:String;

	/**
	 *  @private
	 */
	private static var resourceThousandsSeparatorFrom:String;

	/**
	 *  @private
	 */
	private static var resourceThousandsSeparatorTo:String;

	/**
	 *  @private
	 */
	private static var resourceUseNegativeSign:Boolean;

	/**
	 *  @private
	 */
	private static var resourceUseThousandsSeparator:Boolean; 

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
		resourceDecimalSeparatorFrom =
			sharedResources.getString("decimalSeparatorFrom");

		resourceDecimalSeparatorTo =
			sharedResources.getString("decimalSeparatorTo");

		resourceThousandsSeparatorFrom =
			sharedResources.getString("thousandsSeparatorFrom");

		resourceThousandsSeparatorTo =
			sharedResources.getString("thousandsSeparatorTo");
			
		// packageResources was loaded by Formatter superclass.
			
		resourcePrecision =
			int(packageResources.getObject("numberFormatterPrecision"));

		resourceRounding = packageResources.getString("rounding");

		resourceUseNegativeSign =
			packageResources.getBoolean("useNegativeSign");
		 
		resourceUseThousandsSeparator =
			packageResources.getBoolean("useThousandsSeparator"); 	
	}

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function NumberFormatter()
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
	//  decimalSeparatorFrom
	//----------------------------------

    [Inspectable(category="General", defaultValue=".")]

    /**
     *  Decimal separator character to use
	 *  when parsing an input String.
	 *
	 *  @default "."
     */
	public var decimalSeparatorFrom:String;

	//----------------------------------
	//  decimalSeparatorTo
	//----------------------------------
	
    [Inspectable(category="General", defaultValue=".")]

    /**
     *  Decimal separator character to use
	 *  when outputting formatted decimal numbers.
	 *
	 *  @default "."
     */
	public var decimalSeparatorTo:String;

	//----------------------------------
	//  precision
	//----------------------------------

    [Inspectable(category="General", defaultValue="-1")]

    /**
     *  Number of decimal places to include in the output String.
	 *  You can disable precision by setting it to <code>-1</code>.
	 *  A value of <code>-1</code> means do not change the precision. For example, 
	 *  if the input value is 1.453 and <code>rounding</code> 
	 *  is set to <code>NumberBaseRoundType.NONE</code>, return a value of 1.453.
	 *  If <code>precision</code> is <code>-1</code> and you have set some form of 
	 *  rounding, return a value based on that rounding type.
	 *
	 *  @default -1
     */
	public var precision:int;

	//----------------------------------
	//  rounding
	//----------------------------------

    [Inspectable(category="General", enumeration="none,up,down,nearest", defaultValue="none")]

    /**
     *  Specifies how to round the number.
     *
	 *  <p>In ActionScript, you can use the following constants to set this property: 
	 *  <code>NumberBaseRoundType.NONE</code>, <code>NumberBaseRoundType.UP</code>,
	 *  <code>NumberBaseRoundType.DOWN</code>, or <code>NumberBaseRoundType.NEAREST</code>.
     *  Valid MXML values are "down", "nearest", "up", and "none".</p>
	 *
	 *  @default NumberBaseRoundType.NONE
 	 *
	 *  @see mx.formatters.NumberBaseRoundType
     */
	public var rounding:String;

	//----------------------------------
	//  thousandsSeparatorFrom
	//----------------------------------

    [Inspectable(category="General", defaultValue=",")]

    /**
     *  Character to use as the thousands separator
	 *  in the input String.
	 *
	 *  @default ","
     */
	public var thousandsSeparatorFrom:String;
	
	//----------------------------------
	//  thousandsSeparatorTo
	//----------------------------------

    [Inspectable(category="General", defaultValue=",")]

    /**
     *  Character to use as the thousands separator
	 *  in the output String.
	 *
	 *  @default ","
     */
	public var thousandsSeparatorTo:String;

	//----------------------------------
	//  useNegativeSign
	//----------------------------------
	
    [Inspectable(category="General", defaultValue="true")]

    /**
     *  If <code>true</code>, format a negative number 
	 *  by preceding it with a minus "-" sign.
	 *  If <code>false</code>, format the number
	 *  surrounded by parentheses, for example (400).
	 *
	 *  @default true
     */
	public var useNegativeSign:Boolean;

	//----------------------------------
	//  useThousandsSeparator
	//----------------------------------

    [Inspectable(category="General", defaultValue="true")]

    /**
     *  If <code>true</code>, split the number into thousands increments
	 *  by using a separator character.
	 *
	 *  @default true
     */
	public var useThousandsSeparator:Boolean;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

    /**
     *  Formats the number as a String.
	 *  If <code>value</code> cannot be formatted, return an empty String 
	 *  and write a description of the error to the <code>error</code> property.
	 *
     *  @param value Value to format.
	 *
     *  @return Formatted String. Empty if an error occurs.
     */
    override public function format(value:Object):String
    {
        // Reset any previous errors.
        if (error)
			error = null;

        if (useThousandsSeparator &&
			((decimalSeparatorFrom == thousandsSeparatorFrom) ||
			 (decimalSeparatorTo == thousandsSeparatorTo)))
        {
            error = defaultInvalidFormatError;
            return "";
        }

        if (decimalSeparatorTo == "" || !isNaN(Number(decimalSeparatorTo)))
        {
            error = defaultInvalidFormatError;
            return "";
        }

        var dataFormatter:NumberBase = new NumberBase(decimalSeparatorFrom,
													  thousandsSeparatorFrom,
													  decimalSeparatorTo,
													  thousandsSeparatorTo);

        // -- value --

        if (value is String)
            value = dataFormatter.parseNumberString(String(value));

        if (value === null || isNaN(Number(value)))
        {
            error = defaultInvalidValueError;
            return "";
        } 

        // -- format --

        var isNegative:Boolean = (Number(value) < 0);

        var numStr:String = value.toString();
        var numArrTemp:Array = numStr.split(".");
        var numFraction:int = numArrTemp[1] ? String(numArrTemp[1]).length : 0;

        if (precision <= numFraction)
		{
            if (rounding != "none")
			{
                numStr = dataFormatter.formatRoundingWithPrecision(
					numStr, rounding, precision);
			}
		}

        var numValue:Number = Number(numStr);
        if (Math.abs(numValue) >= 1)
        {
            numArrTemp = numStr.split(".");
            var front:String = useThousandsSeparator ?
							   dataFormatter.formatThousands(String(numArrTemp[0])) :
							   String(numArrTemp[0]);
            if (numArrTemp[1] != null && numArrTemp[1] != "")
                numStr = front + decimalSeparatorTo + numArrTemp[1];
            else
                numStr = front;
        }
        else if (Math.abs(numValue) > 0)
        {
        	// Check if the string is in scientific notation
        	if (numStr.indexOf("e") != -1)
        	{
	        	var temp:Number = Math.abs(numValue) + 1;
	        	numStr = temp.toString();
        	}
            numStr = decimalSeparatorTo +
					 numStr.substring(numStr.indexOf(".") + 1);
        }
        
        numStr = dataFormatter.formatPrecision(numStr, precision);

		// If our value is 0, then don't show -0
		if (Number(numStr) == 0)
		{
			isNegative = false;	
		}

        if (isNegative)
            numStr = dataFormatter.formatNegative(numStr, useNegativeSign);

        if (!dataFormatter.isValid)
        {
            error = defaultInvalidFormatError;
            return "";
        }

        return numStr;
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
		decimalSeparatorFrom = resourceDecimalSeparatorFrom;
		decimalSeparatorTo = resourceDecimalSeparatorTo;
		precision = resourcePrecision;
		rounding = resourceRounding;
		thousandsSeparatorFrom = resourceThousandsSeparatorFrom;
		thousandsSeparatorTo = resourceThousandsSeparatorTo;
		useNegativeSign = resourceUseNegativeSign;
		useThousandsSeparator = resourceUseThousandsSeparator;
	}
}

}
