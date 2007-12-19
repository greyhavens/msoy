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

import mx.controls.ToolTip;

/**
 *  The SliderDataTip class defines the tooltip used in the mx.controls.Slider control. 
 *  The class adds no additional functionality to mx.controls.ToolTip.
 *  It is used only to apply a type selector style.
 *  	
 *  @see mx.controls.HSlider
 *  @see mx.controls.VSlider
 *  @see mx.controls.sliderClasses.Slider
 *  @see mx.controls.sliderClasses.SliderLabel
 *  @see mx.controls.sliderClasses.SliderThumb
 */
public class SliderDataTip extends ToolTip
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
	public function SliderDataTip()
	{
		super();
	}
}

}
