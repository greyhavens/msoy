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

import flash.display.Graphics;

/**
 *  The Stroke class defines the properties for a line. 
 *  
 *  You can define a Stroke object in MXML, but you must attach that Stroke to
 *  another object for it to appear in your application. The following example
 *  defines two Stroke objects and then uses them in the horizontalAxisRenderer
 *  of a LineChart control:
 *  
 *  <pre>
 *  ...
 *  &lt;mx:Stroke id="ticks" color="0xFF0000" weight="1"/&gt;
 *  &lt;mx:Stroke id="mticks" color="0x0000FF" weight="1"/&gt;
 *  
 *  &lt;mx:LineChart id="mychart" dataProvider="{ndxa}"&gt;
 *  	&lt;mx:horizontalAxisRenderer&gt;
 *  		&lt;mx:AxisRenderer placement="bottom" canDropLabels="true"&gt;
 *  			&lt;mx:tickStroke&gt;{ticks}&lt;/mx:tickStroke&gt;
 *  			&lt;mx:minorTickStroke&gt;{mticks}&lt;/mx:minorTickStroke&gt;
 *  		&lt;/mx:AxisRenderer&gt;
 *  	&lt;/mx:horizontalAxisRenderer&gt;
 *  &lt;/LineChart&gt;
 *  ...
 *  </pre>
 *  
 *  @mxml
 *
 *  <p>The <code>&lt;mx:Stroke&gt;</code> tag inherits all the tag attributes
 *  of its superclass, and adds the following tag attributes:</p>
 *
 *  <pre>
 *  &lt;mx:Stroke
 *    <b>Properties</b>
 *    alpha="1.0"
 *    caps="null|none|round|square"
 *    color="0x000000"
 *    joints="null|bevel|miter|round"
 *    miterLimit="0"
 *    pixelHinting="false|true"
 *    scaleMode="normal|none|noScale|vertical"
 *    weight="1 (<i>in most cases</i>)"
 *  /&gt;
 *  </pre>
 *
 *  @see flash.display.Graphics
 */
public class Stroke implements IStroke
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
	 *  @param color Specifies the line color.
	 *
	 *  @param weight Specifies the line weight, in pixels.
	 *
	 *  @param alpha Specifies the alpha value in the range 0.0 to 1.0.
	 *
	 *  @param pixelHinting Specifies whether to hint strokes to full pixels.
	 *  This value affects both the position of anchors of a curve
	 *  and the line stroke size itself.
	 *
	 *  @param scaleMode Specifies how to scale a stroke.
	 *  Valid values are <code>"normal"</code>, <code>"none"</code>,
	 *  <code>"vertical"</code>, and <code>"noScale"</code>.
	 *
	 *  @param caps Specifies the type of caps at the end of lines.
	 *  Valid values are <code>"round"</code>, <code>"square"</code>,
	 *  and <code>"none"</code>.
	 *
	 *  @param joints Specifies the type of joint appearance used at angles.
	 *  Valid values are <code>"round"</code>, <code>"miter"</code>,
	 *  and <code>"bevel"</code>.
	 *
	 *  @param miterLimit Indicates the limit at which a miter is cut off.
	 *  Valid values range from 0 to 255.
	 */
	public function Stroke(color:uint = 0, weight:Number = 0,
						   alpha:Number = 1.0, pixelHinting:Boolean = false,
						   scaleMode:String = "normal", caps:String = null,
						   joints:String = null, miterLimit:Number = 0)
	{
		super();

		this.color = color;
		this._weight = weight;
		this.alpha = alpha;
		this.pixelHinting = pixelHinting;
		this.scaleMode = scaleMode;
		this.caps = caps;
		this.joints = joints;
		this.miterLimit = miterLimit;
	}
	
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  alpha
	//----------------------------------

    [Inspectable(category="General")]

	/**
	 *  The transparency of a line.
	 *  Possible values are 0.0 (invisible) through 1.0 (opaque). 
	 *  
	 *  @default 1.0. 
	 */
	public var alpha:Number;

	//----------------------------------
	//  caps
	//----------------------------------

	[Inspectable(category="General", enumeration="round,square,none", defaultValue="round")]

	/**
	 *  Specifies the type of caps at the end of lines.
	 *  Valid values are: <code>"round"</code>, <code>"square"</code>,
	 *  and <code>"none"</code>.
	 */
	public var caps:String = null;
	
	//----------------------------------
	//  color
	//----------------------------------

    [Inspectable(category="General", format="Color")]

	/**
	 *  The line color. 
	 *  
	 *  @default 0x000000 (black). 
	 */
	public var color:uint = 0;
	
	//----------------------------------
	//  joints
	//----------------------------------

	[Inspectable(category="General", enumeration="round,bevel,miter", defaultValue="round")]

	/**
	 *  Specifies the type of joint appearance used at angles.
	 *  Valid values are <code>"round"</code>, <code>"miter"</code>,
	 *  and <code>"bevel"</code>.
	 */
	public var joints:String = null;
	
	//----------------------------------
	//  miterLimit
	//----------------------------------

	[Inspectable(category="General")]
	
	/**
	 *  Indicates the limit at which a miter is cut off.
	 *  Valid values range from 0 to 255.
	 *  
	 *  @default 0
	 */
	public var miterLimit:Number = 0;

	//----------------------------------
	//  pixelHinting
	//----------------------------------

    [Inspectable(category="General")]
	
	/**
	 *  Specifies whether to hint strokes to full pixels.
	 *  This value affects both the position of anchors of a curve
	 *  and the line stroke size itself.
	 *  
	 *  @default false
	 */
	public var pixelHinting:Boolean = false;
	
	//----------------------------------
	//  scaleMode
	//----------------------------------

	[Inspectable(category="General", enumeration="normal,vertical,horizontal,none", defaultValue="normal")]

	/**
	 *  Specifies how to scale a stroke.
	 *  Valid values are <code>"normal"</code>, <code>"none"</code>,
	 *  <code>"vertical"</code>, and <code>"noScale"</code>.
	 *  
	 *  @default "normal"
	 */
	public var scaleMode:String = "normal";

	//----------------------------------
	//  weight
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the weight property.
	 */
	private var _weight:Number;

    [Inspectable(category="General")]

	/**
	 *  The line weight, in pixels.
	 *  For many charts, the default value is 1 pixel.
	 */
	public function get weight():Number
	{
		return _weight;
	}
	
	/**
	 *  @private
	 */
	public function set weight(value:Number):void
	{
		_weight = value;
	}
	
	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  Applies the properties to the specified Graphics object.
	 *  
	 *  @param g The Graphics object to which the Stroke's styles are applied.
	 */
	public function apply(g:Graphics):void
	{
		g.lineStyle(weight, color, alpha, pixelHinting,
					scaleMode, caps, joints, miterLimit);
	}
}

}
