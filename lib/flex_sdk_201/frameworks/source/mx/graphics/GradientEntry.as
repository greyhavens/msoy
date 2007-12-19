////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.graphics
{

/**
 *  The GradientEntry class defines the objects
 *  that control a transition as part of a gradient fill. 
 *  You use this class with the LinearGradient and RadialGradient classes
 *  to define a gradient fill. 
 *  
 *  @mxml
 *
 *  <p>The <code>&lt;mx:GradientEntry&gt;</code> tag inherits all the tag attributes
 *  of its superclass, and adds the following tag attributes:</p>
 *
 *  <pre>
 *  &lt;mx:GradientEntry
 *    <b>Properties</b>
 *    alpha="1.0"
 *    color="0x000000"
 *    ratio="-1"
 *  /&gt;
 *  </pre>
 *  
 *  @see mx.graphics.LinearGradient 
 *  @see mx.graphics.RadialGradient
 */
public class GradientEntry
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 *
	 *  @param color The color for this gradient entry.
	 *
	 *  @param ratio Where in the graphical element to start
	 *  the transition to the associated color.
	 *  Flex uniformly spaces any GradientEntries
	 *  with missing ratio values.
	 *
	 *  @param alpha The alpha value for this entry in the gradient. 
	 *  This parameter is optional. The default value is 1.
	 */
	public function GradientEntry(color:uint = 0, ratio:Number = -1,
								  alpha:Number = 1)
	{
		super();

		this.color = color;
		
		if (ratio >= 0)
			this.ratio = ratio;
		
		this.alpha = alpha;
	}

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  alpha
	//----------------------------------

    [Inspectable(category="General", defaultValue="1")]

	/**
	 *  The transparency of a gradient fill.
	 *  Possible values are 0.0 (invisible) through 1.0 (opaque). 
	 *  
	 *  @default 1 
	 */
	public var alpha:Number = 1;
	
	//----------------------------------
	//  color
	//----------------------------------

    [Inspectable(category="General", format="Color")]

	/**
	 *  The color value for a gradient fill. 
	 */
	public var color:uint;
	
	//----------------------------------
	//  ratio
	//----------------------------------

    [Inspectable(category="General")]

	/**
	 *  Where in the graphical element, as a percentage from 0 to 1,
	 *  Flex starts the transition to the associated color. 
	 *  For example, a ratio of .33 means Flex begins the transition
	 *  to that color 33% of the way through the graphical element. 
	 */
	public var ratio:Number;
}

}
