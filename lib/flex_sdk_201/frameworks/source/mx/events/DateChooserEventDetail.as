////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.events
{

/**
 *  Constants that define the  values of the <code>detail</code> property
 *  of a DateChooserEvent object.
 *
 *  @see mx.events.DateChooserEvent
 */
public final class DateChooserEventDetail
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

	/**
	 *  Indicates that the user scrolled the calendar to the next month.
	 */
	public static const NEXT_MONTH:String = "nextMonth";

	/**
	 *  Indicates that the user scrolled the calendar to the next year.
	 */
	public static const NEXT_YEAR:String = "nextYear";

	/**
	 *  Indicates that the user scrolled the calendar to the previous month.
	 */
	public static const PREVIOUS_MONTH:String = "previousMonth";

	/**
	 *  Indicates that the user scrolled the calendar to the previous year.
	 */
	public static const PREVIOUS_YEAR:String = "previousYear";
}

}
