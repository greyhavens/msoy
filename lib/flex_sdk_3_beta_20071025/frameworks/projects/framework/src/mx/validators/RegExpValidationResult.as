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

/**
 *  The RegExpValidator class dispatches the <code>valid</code>
 *  and <code>invalid</code> events. 
 *  For an <code>invalid</code> event, the event object
 *  is an instance of the ValidationResultEvent class, 
 *  and the <code>ValidationResultEvent.results</code> property
 *  contains an Array of ValidationResult objects.
 *
 *  <p>However, for a <code>valid</code> event, the 
 *  <code>ValidationResultEvent.results</code> property contains 
 *  an Array of RegExpValidationResult objects.
 *  The RegExpValidationResult class is a child class
 *  of the ValidationResult class, and contains additional properties 
 *  used with regular expressions.</p>
 *
 *  @see mx.events.ValidationResultEvent
 */
public class RegExpValidationResult extends ValidationResult
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/** 
	 *  Constructor
	 *  
     *  @param isError Pass <code>true</code> if there was a validation error.
     *
     *  @param subField Name of the subfield of the validated Object.
     *
     *  @param errorCode  Validation error code.
     *
     *  @param errorMessage Validation error message.
     *
     *  @param matchedString Matching substring.
     *
     *  @param matchedIndex Index of the matching String.
     *
     *  @param matchedSubstrings Array of substring matches.
	 */
	public function RegExpValidationResult(isError:Boolean, 
										   subField:String = "", 
										   errorCode:String = "", 
										   errorMessage:String = "",
										   matchedString:String = "",
										   matchedIndex:int = 0,
										   matchedSubstrings:Array = null)
	{
		super(isError, subField, errorCode, errorMessage);
		
		this.matchedString = matchedString;
		this.matchedIndex = matchedIndex;
		this.matchedSubstrings = matchedSubstrings ? matchedSubstrings : [];
	}
	
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//--------------------------------------------------------------------------
	//  matchedIndex
	//--------------------------------------------------------------------------

	/** 
	 *  An integer that contains the starting index
	 *  in the input String of the match.
	 */
	public var matchedIndex:int;

	//--------------------------------------------------------------------------
	//  matchedString
	//--------------------------------------------------------------------------

	/**
	 *  A String that contains the substring of the input String
	 *  that matches the regular expression.
	 */
	public var matchedString:String;

	//--------------------------------------------------------------------------
	//  matchedSubstrings
	//--------------------------------------------------------------------------

	/**
	 *  An Array of Strings that contains parenthesized
	 *  substring matches, if any. 
	 *	If no substring matches are found, this Array is of length 0.
	 *	Use <code>matchedSubStrings[0]</code> to access
	 *  the first substring match.
	 */
	public var matchedSubstrings:Array;
}

}
