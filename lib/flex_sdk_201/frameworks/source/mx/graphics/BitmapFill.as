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

import flash.display.Bitmap;
import flash.display.BitmapData;
import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.geom.Matrix;
import flash.geom.Rectangle;
import flash.utils.getDefinitionByName;

/** 
 *  Defines a set of values used to fill an area on screen with a bitmap or other DisplayObject.
 *  
 *  @see mx.graphics.IFill
 *  @see flash.display.Bitmap
 *  @see flash.display.BitmapData
 *  @see flash.display.DisplayObject
 */
public class BitmapFill implements IFill
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
	public function BitmapFill()
 	{
		super();
	}
	
	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private var _matrix:Matrix;	
	
	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  centerX
	//----------------------------------

	[Inspectable(category="General")]	
	
	/**
	 *  The horizontal origin for the bitmap fill. The bitmap fill is offset so that this point appears at the origin. Scaling and rotation of the bitmap are performed around this point.  
	 */
	public var originX:Number = 0;
	
	//----------------------------------
	//  centerY
	//----------------------------------

	[Inspectable(category="General")]	
	
	/**
	 *  The vertical origin for the bitmap fill. The bitmap fill is offset so that this point appears at the origin. Scaling and rotation of the bitmap are performed around this point.  
	 */
	public var originY:Number = 0;

	//----------------------------------
	//  offsetX
	//----------------------------------

	[Inspectable(category="General")]	
	
	/**
	 *  How far the bitmap is horizontally offset from the origin. This adjustment is performed after rotation and scaling.
	 */
	public var offsetX:Number = 0;

	//----------------------------------
	//  offsetY
	//----------------------------------

	[Inspectable(category="General")]	
	
	/**
	 *  How far the bitmap is vertically offset from the origin. This adjustment is performed after rotation and scaling.
	 */
	public var offsetY:Number = 0;

	//----------------------------------
	//  repeat
	//----------------------------------

	[Inspectable(category="General")]	
	
	/**
	 *  Whether the bitmap is repeated to fill the area. Set to <code>true</code> to cause the fill to tile outward to 
	 *  the edges of the filled region. Set to <code>false</code> to end the fill at the edge of the region.
	 */
	public var repeat:Boolean = true;

	//----------------------------------
	//  rotation
	//----------------------------------

	[Inspectable(category="General")]	
	
	/**
	 *  The number of degrees to rotate the bitmap. Valid values range from 0 to 360. The default value is 0.
	 */
	public var rotation:Number = 0;

	//----------------------------------
	//  scaleX
	//----------------------------------

	[Inspectable(category="General")]	
	
	/**
	 *  The percent to horizontally scale the bitmap when filling, from 0 to 1. If 1, the bitmap is filled at its natural size.  
	 *  The default value is 1.
	 */
	public var scaleX:Number = 1;

	//----------------------------------
	//  scaleY
	//----------------------------------
	
	[Inspectable(category="General")]	
	
	/**
	 *  The percent to vertically scale the bitmap when filling, from 0 to 1. If 1, the bitmap is filled at its natural size.  
	 *  The default value is 1.
	 */
	public var scaleY:Number = 1;

	//----------------------------------
	//  source
	//----------------------------------

	/**
	 *  @private
	 */
	private var _bmp:BitmapData;

    [Inspectable(category="General")]

	/**
	 *  The source used for the bitmap fill. The fill can render from various graphical sources, including the following: 
	 *  <ul>
	 *   <li>A Bitmap or BitmapData instance.</li>
	 *   <li>A class representing a subclass of DisplayObject. The BitmapFill instantiates 
	 *       the class and creates a bitmap rendering of it.</li>
	 *   <li>An instance of a DisplayObject. The BitmapFill copies it into a Bitmap for filling.</li>
	 *   <li>The name of a subclass of DisplayObject. The BitmapFill loads the class, instantiates it, 
	 *       and creates a bitmap rendering of it.</li>
	 *  </ul>
	 */
	public function get source():Object 
	{
		return _bmp;
	}
	
	/**
	 *  @private
	 */
	public function set source(v:Object):void
	{
		var tmpSprite:DisplayObject;
		
		if (v is BitmapData)
		{
			_bmp = BitmapData(v);
			return;
		}

		if (v is Class)
		{
			var cls:Class = Class(v);
			tmpSprite = new cls();
		}
		else if (v is Bitmap)
		{
			_bmp = v.bitmapData;
		}
		else if (v is DisplayObject)
		{
			tmpSprite;
		}
		else if (v is String)
		{
			var tmpClass:Class = Class(getDefinitionByName(String(v)));
			tmpSprite = new tmpClass();
		}
		else
		{
			return;
		}
			
		if(_bmp == null && tmpSprite != null)
		{
			_bmp = new BitmapData(tmpSprite.width, tmpSprite.height);
			_bmp.draw(tmpSprite, new Matrix());
		}
	}

	//----------------------------------
	//  smooth
	//----------------------------------

	[Inspectable(category="General")]	
	
	/**
	 *  A flag indicating whether to smooth the bitmap data when filling with it.
	 */
	public var smooth:Boolean = false;

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private function buildMatrix():void
	{
		_matrix = new Matrix();

		_matrix.translate(-originX,-originY);
		_matrix.scale(scaleX,scaleY);
		_matrix.rotate(rotation);
		_matrix.translate(offsetX,offsetY);
	}

	/**
	* @private
	 */
	public function begin(target:Graphics, rc:Rectangle):void
	{
		buildMatrix();
		if (_bmp == null)
			return;
		target.beginBitmapFill(_bmp, _matrix, repeat, smooth);
	}
	
	/**
	* @private
	 */
	public function end(target:Graphics):void
	{
		target.endFill();
	}
}

}
