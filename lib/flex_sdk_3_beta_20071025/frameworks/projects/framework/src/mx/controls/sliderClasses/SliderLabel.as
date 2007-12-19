////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.sliderClasses
{

import flash.text.TextLineMetrics;
import mx.controls.Label;
import mx.core.mx_internal;

use namespace mx_internal;

/**
 *  The SliderLabel class defines the label used in the mx.controls.Slider component. 
 *  The class adds no additional functionality to mx.controls.Label.
 *  It is used to apply a type selector style.
 *  	
 *  @see mx.controls.HSlider
 *  @see mx.controls.VSlider
 *  @see mx.controls.sliderClasses.Slider
 *  @see mx.controls.sliderClasses.SliderDataTip
 *  @see mx.controls.sliderClasses.SliderThumb
 */
public class SliderLabel extends Label
{
	include "../../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function SliderLabel()
	{
		super();
	}
	
	/**
	 *  @private 
	 */
	override mx_internal function getMinimumText(t:String):String
	{
		 // If the text is null or empty
		// make the measured size big enough to hold
		// a capital character using the current font.
        if (!t || t.length < 1)
            t = "W";
			
		return t;	
	}
}

}
