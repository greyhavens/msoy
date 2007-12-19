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
 *  The SliderEventClickTarget class defines the constants for the values of 
 *  the <code>clickTarget</code> property of the SliderEvent class.
 *
 *  @see mx.events.SliderEvent
 */
public final class SliderEventClickTarget
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class constants
	//
	//--------------------------------------------------------------------------

	/**
	 *  Specifies that the Slider's track was clicked.
	 */
	public static const TRACK:String = "track";
	
	/**
	 *  Specifies that the Slider's thumb was clicked.
	 */
	public static const THUMB:String = "thumb";
}

}
