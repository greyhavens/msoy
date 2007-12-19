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
 *  The CurrencyFormatter class formats a valid number as a currency value.
 *  It adjusts the decimal rounding and precision, the thousands separator, 
 *  and the negative sign; it also adds a currency symbol.
 *  You place the currency symbol on either the left or the right side
 *  of the value with the <code>alignSymbol</code> property.
 *  The currency symbol can contain multiple characters,
 *  including blank spaces.
 *  
 *  <p>If an error occurs, an empty String is returned and a String that describes 
 *  the error is saved to the <code>error</code> property. The <code>error</code> 
 *  property can have one of the following values:</p>
 *
 *  <ul>
 *    <li><code>"Invalid value"</code> means an invalid numeric value is passed to 
 *    the <code>format()</code> method. The value should be a valid number in the 
 *    form of a Number or a String.</li>
 *    <li><code>"Invalid format"</code> means one of the parameters contains an unusable setting.</li>
 *  </ul>
 *  
 *  @mxml
 *  
 *  <p>The <code>&lt;mx:CurrencyFormatter&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds the following tag attributes:</p>
 *  
 *  <pre>
 *  &lt;mx:CurrencyFormatter
 *    alignSymbol="left|right" 
 *    currencySymbol="$"
 *    decimalSeparatorFrom="."
 *    decimalSeparatorTo="."
 *    precision="-1"
 *    rounding="none|up|down|nearest"
 *    thousandsSeparatorFrom=","
 *    thousandsSeparatorTo=","
 *    useNegativeSign="true|false"
 *    useThousandsSeparator="true|false"
 *	/>  
 *  </pre>
 *  
 *  @includeExample examples/CurrencyFormatterExample.mxml
 *  
 *  @see mx.formatters.NumberBase
 *  @see mx.formatters.NumberBaseRoundType
 */
public class CurrencyFormatter extends Formatter
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
	private static var resourceAlignSymbol:String;

	/**
	 *  @private
     */	
	private static var resourceCurrencySymbol:String;

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
		resourceAlignSymbol	= sharedResources.getString("alignSymbol");
		
		resourceCurrencySymbol = sharedResources.getString("currencySymbol");
		
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
			int(packageResources.getObject("currencyFormatterPrecision"));
		
		resourceRounding = packageResources.getString("rounding");

		resourceUseNegativeSign = packageResources.getBoolean("useNegativeSign"); 
		
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
	public function CurrencyFormatter()
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
     *  Aligns currency symbol to the left side or the right side
	 *  of the formatted number.
     *  Permitted values are <code>"left"</code> and <code>"right"</code>.
	 *
     *  @default "left"
     */
	public var alignSymbol:String;

	//----------------------------------
	//  currencySymbol
	//----------------------------------

    [Inspectable(category="General", defaultValue="$")]

    /**
     *  Character to use as a currency symbol for a formatted number.
     *  You can use one or more characters to represent the currency 
	 *  symbol; for example, "$" or "YEN".
	 *  You can also use empty spaces to add space between the 
	 *  currency character and the formatted number.
	 *  When the number is a negative value, the currency symbol
	 *  appears between the number and the minus sign or parentheses.
	 *
     *  @default "$"
     */
	public var currencySymbol:String;

	//----------------------------------
	//  decimalSeparatorFrom
	//----------------------------------

    [Inspectable(category="General", defaultValue=".")]

    /**
     *  Decimal separator character to use
	 *  when parsing an input string.
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
	 *  is set to <code>NumberBaseRoundType.NONE</code>, return 1.453.
	 *  If <code>precision</code> is -1 and you set some form of 
	 *  rounding, return a value based on that rounding type.
	 *
     *  @default  -1
     */
	public var precision:int;

	//----------------------------------
	//  rounding
	//----------------------------------

    [Inspectable(category="General", defaultValue="none")]

    /**
     *  How to round the number.
	 *  In ActionScript, the value can be <code>NumberBaseRoundType.NONE</code>, 
	 *  <code>NumberBaseRoundType.UP</code>,
	 *  <code>NumberBaseRoundType.DOWN</code>, or <code>NumberBaseRoundType.NEAREST</code>.
	 *  In MXML, the value can be <code>"none"</code>, 
	 *  <code>"up"</code>, <code>"down"</code>, or <code>"nearest"</code>.
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
	 *  in the output string.
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
     *  Formats <code>value</code> as currency.
	 *  If <code>value</code> cannot be formatted, return an empty String 
	 *  and write a description of the error to the <code>error</code> property.
	 *
     *  @param value Value to format.
	 *
     *  @return Formatted string. Empty if an error occurs.
     */
    override public function format(value:Object):String
    {
        // Reset any previous errors.
        if (error)
			error = null;

        if (useThousandsSeparator &&
			(decimalSeparatorFrom == thousandsSeparatorFrom ||
			 decimalSeparatorTo == thousandsSeparatorTo))
        {
            error = defaultInvalidFormatError;
            return "";
        }

        if (decimalSeparatorTo == "")
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
            var front:String =
				useThousandsSeparator ?
				dataFormatter.formatThousands(String(numArrTemp[0])) :
				String(numArrTemp[0]);
            if (numArrTemp[1] != null && numArrTemp[1] != "")
                numStr = front + decimalSeparatorTo + numArrTemp[1];
            else
                numStr = front;
        }
        else if (Math.abs(numValue) > 0)
        {
        	// if the value is in scientefic notation then the search for '.' 
        	// doesnot give the correct result. Adding one to the value forces 
        	// the value to normal decimal notation. 
        	// As we are dealing with only the decimal portion we need not 
        	// worry about reverting the addition
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

        // -- currency --

        if (alignSymbol == "left")
		{
            if (isNegative)
			{
                var nSign:String = numStr.charAt(0);
                var baseVal:String = numStr.substr(1, numStr.length - 1);
                numStr = nSign + currencySymbol + baseVal;
            }
			else
			{
                numStr = currencySymbol + numStr;
            }
        } 
		else if (alignSymbol == "right")
		{
            var lastChar:String = numStr.charAt(numStr.length - 1);
            if (isNegative && lastChar == ")")
			{
                baseVal = numStr.substr(0, numStr.length - 2);
                numStr = baseVal + currencySymbol + lastChar;
            }
			else
			{
                numStr = numStr + currencySymbol;
            }
        }
		else
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
		alignSymbol = resourceAlignSymbol;
		currencySymbol = resourceCurrencySymbol;
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
