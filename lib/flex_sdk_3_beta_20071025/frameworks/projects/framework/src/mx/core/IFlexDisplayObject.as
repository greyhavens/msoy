////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.IBitmapDrawable;
import flash.events.IEventDispatcher;

/**
 *  The IFlexDisplayObject interface defines the interface for skin elements.
 *  At a minimum, a skin must be a DisplayObject and implement this interface.
 */
public interface IFlexDisplayObject extends IBitmapDrawable, IEventDispatcher
{
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  alpha
	//----------------------------------

	/**
	 *  The transparency of this object.
	 *  The value is a Number from 0.0 to 1.0, 
	 *  where 0.0 means transparent and 1.0 means fully opaque.
	 *
	 *  @see flash.display.DisplayObject#alpha
	 */
	function get alpha():Number;
	
	/**
	 *  @private
	 */
	function set alpha(value:Number):void;

	//----------------------------------
	//  height
	//----------------------------------

	/**
	 *  The height of this object, in pixels.
	 */
	function get height():Number;
	
	/**
	 *  @private
	 */
	function set height(value:Number):void;

	//----------------------------------
	//  mask
	//----------------------------------

	/**
	 *  The DisplayObject used to mask this object
	 *  so that only part of it is actually drawn.
	 *  The <code>mask</code> object itself is not drawn.
	 *
	 *  <p>Set <code>mask</code> to <code>null</code> to remove the mask.</p>
	 *
	 *  <p>To ensure that masking works when the stage is scaled, 
	 *  the <code>mask</code> display object must be in an active
	 *  part of the display list.</p>
	 *
	 *  @see flash.display.DisplayObject#mask
	 */
	function get mask():DisplayObject;
	
	/**
	 *  @private
	 */
	function set mask(value:DisplayObject):void;

	//----------------------------------
	//  measuredHeight
	//----------------------------------

	/**
	 *  The measured height of this object.
	 *
	 *  <p>This is typically hard-coded for graphical skins
	 *  because this number is simply the number of pixels in the graphic.
	 *  For code skins, it can also be hard-coded
	 *  if you expect to be drawn at a certain size.
	 *  If your size can change based on properties, you may want
	 *  to also be an ILayoutManagerClient so a <code>measure()</code>
	 *  method will be called at an appropriate time,
	 *  giving you an opportunity to compute a <code>measuredHeight</code>.</p>
	 */
	function get measuredHeight():Number;

	//----------------------------------
	//  measuredWidth
	//----------------------------------

	/**
	 *  The measured width of this object.
	 *
	 *  <p>This is typically hard-coded for graphical skins
	 *  because this number is simply the number of pixels in the graphic.
	 *  For code skins, it can also be hard-coded
	 *  if you expect to be drawn at a certain size.
	 *  If your size can change based on properties, you may want
	 *  to also be an ILayoutManagerClient so a <code>measure()</code>
	 *  method will be called at an appropriate time,
	 *  giving you an opportunity to compute a <code>measuredHeight</code>.</p>
	 */
	function get measuredWidth():Number;

	//----------------------------------
	//  name
	//----------------------------------

	/**
	 *  The instance name of this object.
	 *
	 *  <p>Flash classes such as Sprite, TextField, etc.
	 *  initialize the instance name to <code>"instanceN"</code>,
	 *  where N is a unique integer.
	 *  Flex classes such as FlexSprite, FlexTextField, etc.
	 *  initialize it to a string that combines the class name
	 *  with a unique integer, such as <code>"Button5"</code>.
	 *  (This name is produced by the
	 *  <code>NameUtil.createUniqueName()</code> method.)
	 *  The Flex framework assigns instance names to some objects
	 *  that it creates, such as <code>"upSkin"</code> for the
	 *  "up"-state skin of a Button.
	 *  If you have assigned an MXML id to this object,
	 *  then the instance name is set to that id.</p>
	 *
	 *  <p>You can use the DisplayObjectContainer method
	 *  <code>getChildByName()</code> to get an object by name
	 *  from its parent container.</p>
	 */
	function get name():String;
	
	/**
	 *  @private
	 */
	function set name(value:String):void;

	//----------------------------------
	//  parent
	//----------------------------------

	/**
	 *  The parent of this object.
	 *
	 *  <p>This property can be <code>null</code> if the object has not yet
	 *  ben added to a DisplayObjectContainer with <code>addChild()</code>
	 *  or <code>addChildAt()</code>, or if it has been removed from a
	 *  DisplayObjectContainer with <code>removeChild()</code>
	 *  or <code>removeChildAt()</code>.</p>
	 */
	function get parent():DisplayObjectContainer;
	
	//----------------------------------
	//  scaleX
	//----------------------------------

	/**
	 *  The horizontal scaling factor for this object.
	 *  The value is a Number where 1.0 means the object isn't scaled
	 *  horziontally, 2.0 means that it is stretched to twice its
	 *  normal width, and 0.5 means that it is compressed to half
	 *  its normal width.
	 */
	function get scaleX():Number;
	
	/**
	 *  @private
	 */
	function set scaleX(value:Number):void;

	//----------------------------------
	//  scaleY
	//----------------------------------

	/**
	 *  The vertical scaling factor for this object.
	 *  The value is a Number where 1.0 means the object isn't scaled
	 *  vertically, 2.0 means that it is stretched to twice its
	 *  normal height, and 0.5 means that it is compressed to half
	 *  its normal height.
	 */
	function get scaleY():Number;
	
	/**
	 *  @private
	 */
	function set scaleY(value:Number):void;

	//----------------------------------
	//  x
	//----------------------------------

	/**
	 *  The horizontal position of this object relative to its parent,
	 *  in pixel coordinates.
	 */
	function get x():Number;
	
	/**
	 *  @private
	 */
	function set x(value:Number):void;

	//----------------------------------
	//  y
	//----------------------------------

	/**
	 *  The vertical position of this object relative to its parent,
	 *  in pixel coordinates.
	 */
	function get y():Number;
	
	/**
	 *  @private
	 */
	function set y(value:Number):void;

	//----------------------------------
	//  visible
	//----------------------------------

	/**
	 *  A flag that indicates whether this object is visible.
	 */
	function get visible():Boolean;
	
	/**
	 *  @private
	 */
	function set visible(value:Boolean):void;

	//----------------------------------
	//  width
	//----------------------------------

	/**
	 *  The width of this object, in pixels.
	 */
	function get width():Number;
	
	/**
	 *  @private
	 */
	function set width(value:Number):void;

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  Moves this object to the specified x and y coordinates.
	 * 
	 *  @param x The new x-position for this object.
	 * 
	 *  @param y The new y-position for this object.
	 */
	function move(x:Number, y:Number):void;

	/**
	 *  Sets the actual size of this object.
	 *
	 *  <p>This method is mainly for use in implementing the
	 *  <code>updateDisplayList()</code> method, which is where
	 *  you compute this object's actual size based on
	 *  its explicit size, parent-relative (percent) size,
	 *  and measured size.
	 *  You then apply this actual size to the object
	 *  by calling <code>setActualSize()</code>.</p>
	 *
	 *  <p>In other situations, you should be setting properties
	 *  such as <code>width</code>, <code>height</code>,
	 *  <code>percentWidth</code>, or <code>percentHeight</code>
	 *  rather than calling this method.</p>
	 * 
	 *  @param newWidth The new width for this object.
	 * 
	 *  @param newHeight The new height for this object.
	 */
	function setActualSize(newWidth:Number, newHeight:Number):void;
}

}
