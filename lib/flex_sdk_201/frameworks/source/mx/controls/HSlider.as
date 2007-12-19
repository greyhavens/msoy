////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls
{

import mx.controls.sliderClasses.Slider;
import mx.controls.sliderClasses.SliderDirection;

//--------------------------------------
//  Styles
//--------------------------------------

/**
 *  The location of the data tip relative to the thumb.
 *  Possible values are <code>"left"</code>, <code>"right"</code>,
 *  <code>"top"</code>, and <code>"bottom"</code>.
 *
 *  @default "top"
 */
[Style(name="dataTipPlacement", type="String", inherit="no")]

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="direction", kind="property")]

//--------------------------------------
//  Other metadata
//--------------------------------------

[DefaultBindingProperty(source="value", destination="labels")]

[DefaultTriggerEvent("change")]

[IconFile("HSlider.png")]

/**	
 *  The HSlider control lets users select a value by moving
 *  a slider thumb between the end points of the slider track.
 *  The current value of the slider is determined by the relative
 *  location of the thumb between the end points of the slider,
 *  corresponding to the slider's minimum and maximum values.
 *
 *  <p>The slider may allow a continuous range of values between its
 *  minimum and maximum values or it may be restricted to values
 *  at specific intervals between the minimum and maximum value.
 *  It may show tick marks at specified intervals along the track. These
 *  tick marks are independent of the allowed values of the slider. The slider
 *  may also use a data tip to display its current value.</p>
 *  	
 *  <p>The HSlider control has a horizontal direction.
 *  The slider track stretches from left to right, and the labels
 *  and tick marks are placed at the top or bottom of the track.</p>
 *  
 *  @mxml
 *  
 *  <p>The <code>&lt;mx:HSlider&gt;</code> tag inherits all of the tag attributes
 *  of its superclass, and adds the following tag attribute:</p>
 * 
 *  <pre>
 *  &lt;mx:HSlider
 *    <strong>Styles</strong>
 *    dataTipPlacement="top"
 *  /&gt;
 *  </pre>
 *  </p>
 *  
 *  @includeExample examples/SimpleImageHSlider.mxml
 *  	
 *  @see mx.controls.VSlider
 *  @see mx.controls.sliderClasses.Slider
 *  @see mx.controls.sliderClasses.SliderThumb
 *  @see mx.controls.sliderClasses.SliderDataTip
 *  @see mx.controls.sliderClasses.SliderLabel
 */
public class HSlider extends Slider
{
	include "../core/Version.as";
		
	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function HSlider()
	{
		super();
		
		// Slider variables.
		direction = SliderDirection.HORIZONTAL;
	}
}

}
