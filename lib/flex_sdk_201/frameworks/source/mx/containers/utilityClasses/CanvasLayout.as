////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.containers.utilityClasses
{

import flash.display.DisplayObject;
import flash.geom.Rectangle;
import mx.core.Container;
import mx.core.EdgeMetrics;
import mx.core.IUIComponent;
import mx.core.mx_internal;
import mx.events.ChildExistenceChangedEvent;
import mx.events.MoveEvent;
import mx.styles.IStyleClient;

[ExcludeClass]

/**
 *  @private
 *  The CanvasLayout class is for internal use only.
 */
public class CanvasLayout extends Layout
{
	include "../../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Class variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private static var r:Rectangle = new Rectangle();

	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  Restrict a number to a particular min and max.
	 */
	private function bound(a:Number, min:Number, max:Number):Number
	{
		if (a < min)
			a = min;
		else if (a > max)
			a = max;
		else
			a = Math.floor(a);

		return a;
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	private var _contentArea:Rectangle;

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  Constructor.
	 */
	public function CanvasLayout()
	{
		super();
	}

	//--------------------------------------------------------------------------
	//
	//  Overridden properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  target
	//----------------------------------

	/**
	 *  @private
	 */
	override public function set target(value:Container):void
	{
		var target:Container = super.target;

		if (value != target)
		{
			var i:int;
			var n:int;

			if (target)
			{
				// Start listening for child existence events.
				// We want to track the movement of children
				// so we can update our size every time a
				// child moves.

				target.removeEventListener(
						ChildExistenceChangedEvent.CHILD_ADD,
						target_childAddHandler);
				target.removeEventListener(
						ChildExistenceChangedEvent.CHILD_REMOVE,
						target_childRemoveHandler);

				n = target.numChildren;
				for (i = 0; i < n; i++)
				{
					DisplayObject(target.getChildAt(i)).removeEventListener(
						MoveEvent.MOVE, child_moveHandler);
				}
			}

			if (value)
			{
				value.addEventListener(
						ChildExistenceChangedEvent.CHILD_ADD,
						target_childAddHandler);
				value.addEventListener(
						ChildExistenceChangedEvent.CHILD_REMOVE,
						target_childRemoveHandler);

				n = value.numChildren;
				for (i = 0; i < n; i++)
				{
					DisplayObject(value.getChildAt(i)).addEventListener(
						MoveEvent.MOVE, child_moveHandler);
				}
			}

			super.target = value;
		}
	}

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  Measure container as per Canvas layout rules.
	 */
	override public function measure():void
	{
		var target:Container = super.target;

		var w:Number = 0;
		var h:Number = 0;

		var vm:EdgeMetrics = target.viewMetrics;
		
		_contentArea = null;
		var contentArea:Rectangle = measureContentArea();

		// Only add viewMetrics padding
		// if children are bigger than existing size.
		target.measuredWidth = contentArea.width + vm.left + vm.right;
		target.measuredHeight = contentArea.height + vm.top + vm.bottom;
	}

	/**
	 *  @private
	 *  Lay out children as per Canvas layout rules.
	 */
	override public function updateDisplayList(unscaledWidth:Number,
											   unscaledHeight:Number):void
	{
		var target:Container = super.target;	
		
		// viewMetrics include scrollbars during updateDisplayList, but not
		// during measure. In order to avoid a race condition when the 
		// scrollable area is within a scrollbar's width of the view metrics,
		// we use the non-update viewMetrics, which don't include scrollbars.
		target.mx_internal::doingLayout = false;
		var vm:EdgeMetrics = target.viewMetrics;
		target.mx_internal::doingLayout = true;
		
		var viewableWidth:Number = unscaledWidth - vm.left - vm.right;
		var viewableHeight:Number = unscaledHeight - vm.top - vm.bottom;

		// Apply the CSS styles left, top, right, bottom,
		// horizontalCenter, and verticalCenter;
		// these override x, y, width, and height if specified.
		var n:int = target.numChildren;
		for (var i:int = 0; i < n; i++)
		{
			var child:IUIComponent = target.getChildAt(i) as IUIComponent;
			
			applyAnchorStylesDuringUpdateDisplayList(
				viewableWidth, viewableHeight, 
				child);
		}
	}

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	private function applyAnchorStylesDuringMeasure(child:IUIComponent,
													r:Rectangle):void
	{
		var styleableChild:IStyleClient = child as IStyleClient;
		if (!styleableChild)
			return;

		var left:Number = styleableChild.getStyle("left");
		var right:Number = styleableChild.getStyle("right");
		var horizontalCenter:Number =
			styleableChild.getStyle("horizontalCenter");

		if (!isNaN(horizontalCenter))
		{
			r.x = Math.round((target.width - child.width) / 2 + horizontalCenter);
		}
		else if (!isNaN(left) && !isNaN(right))
		{
			r.x = left;
			r.width += right;
		}
		else if (!isNaN(left))
		{
			r.x = left;
		}
		else if (!isNaN(right))
		{
			r.x = 0;
			r.width += right;
		}

		var top:Number = styleableChild.getStyle("top");
		var bottom:Number = styleableChild.getStyle("bottom");
		var verticalCenter:Number = styleableChild.getStyle("verticalCenter");

		if (!isNaN(verticalCenter))
		{
			r.y = Math.round((target.height - child.height) / 2 + verticalCenter);
		}
		else if (!isNaN(top) && !isNaN(bottom))
		{
			r.y = top;
			r.height += bottom;
		}
		else if (!isNaN(top))
		{
			r.y = top;
		}
		else if (!isNaN(bottom))
		{
			r.y = 0;
			r.height += bottom;
		}
	}

	/**
	 *  @private
	 *  Here is a description of the layout algorithm.
	 *  It is described in terms of horizontal coordinates,
	 *  but the vertical ones are similar.
	 *
	 *  1. First the actual width for the child is determined.
	 *
	 *  1a. If both left and right anchors are specified,
	 *  the actual width is determined by them.
	 *  However, the actual width is subject to the child's
	 *  minWidth.
	 *
	 *  1b. Otherwise, if a percentWidth was specified,
	 *  this percentage is applied to the parent's content width
	 *  (the widest specified point of content, or the width of
	 *  the parent, whichever is greater).
	 *  The actual width is subject to the child's
	 *  minWidth and maxWidth.
	 *
	 *  1c. Otherwise, if an explicitWidth was specified,
	 *  this is used as the actual width.
	 *
	 *  1d. Otherwise, the measuredWidth is used is used as the
	 *  actual width.
	 *
	 *  2. Then the x coordinate of the child is determined.
	 *
	 *  2a. If a horizonalCenter anchor is specified,
	 *  the center of the child is placed relative to the center
	 *  of the parent.
	 *
	 *  2b. Otherwise, if a left anchor is specified,
	 *  the left edge of the child is placed there.
	 *
	 *  2c. Otherwise, if a right anchor is specified,
	 *  the right edge of the child is placed there.
	 *
	 *  2d. Otherwise, the child is left at its previously set
	 *  x coordinate.
	 *
	 *  3. If the width is a percentage, try to make sure it
	 *  doesn't overflow the content width (while still honoring
	 *  minWidth). We need to wait
	 *  until after the x coordinate is set to test this.
	 */
	private function applyAnchorStylesDuringUpdateDisplayList(
							availableWidth:Number,
							availableHeight:Number,
							child:IUIComponent):void
	{
		var styleableChild:IStyleClient = child as IStyleClient;
		if (!styleableChild)
			return;

		var left:Number = styleableChild.getStyle("left");
		var right:Number = styleableChild.getStyle("right");
		var horizontalCenter:Number =
			styleableChild.getStyle("horizontalCenter");

		var top:Number = styleableChild.getStyle("top");
		var bottom:Number = styleableChild.getStyle("bottom");
		var verticalCenter:Number =
			styleableChild.getStyle("verticalCenter");

		var w:Number;
		var h:Number;
		
		var x:Number;
		var y:Number;
		
		var checkWidth:Boolean = false;
		var checkHeight:Boolean = false;

		// If a percentage size is specified for a child,
		// it specifies a percentage of the parent's content size
		// minus any specified left, top, right, or bottom
		// anchors for this child.
		// Also, respect the child's minimum and maximum sizes.
		
		if (!isNaN(left) && !isNaN(right))
		{
			w = availableWidth - left - right;
			if (w < child.minWidth)
				w = child.minWidth;
		}
		else if (!isNaN(child.percentWidth))
		{
			w = child.percentWidth / 100 * availableWidth;
			w = bound(w, child.minWidth, child.maxWidth);
			
			checkWidth = true;
		}
		else
		{
			w = child.getExplicitOrMeasuredWidth();
		}

		if (!isNaN(top) && !isNaN(bottom))
		{
			h = availableHeight - top - bottom;
			if (h < child.minHeight)
				h = child.minHeight;
		}
		else if (!isNaN(child.percentHeight))
		{
			h = child.percentHeight / 100 * availableHeight;
			h = bound(h, child.minHeight, child.maxHeight);
			
			checkHeight = true;
		}
		else
		{
			h = child.getExplicitOrMeasuredHeight();
		}
		
		// The left, right, and horizontalCenter styles
		// affect the child's x and/or its actual width.
		
		if (!isNaN(horizontalCenter))
		{
			x = Math.round((availableWidth - w) / 2 + horizontalCenter);
		}
		else if (!isNaN(left))
		{
			x = left;
		}
		else if (!isNaN(right))
		{
			x = availableWidth - right - w;
		}

		// The top, bottom, and verticalCenter styles
		// affect the child's y and/or its actual height.

		if (!isNaN(verticalCenter))
		{
			y = Math.round((availableHeight - h) / 2 + verticalCenter);
		}
		else if (!isNaN(top))
		{
			y = top;
		}
		else if (!isNaN(bottom))
		{
			y = availableHeight - bottom - h;
		}
		
		x = isNaN(x) ? child.x : x;
		y = isNaN(y) ? child.y : y;
		
		child.move(x, y);
		
		// One last test here. If the width/height is a percentage,
		// limit the width/height to the available content width/height, 
		// but honor the minWidth/minHeight.
		if (checkWidth)
		{
			if (x + w > availableWidth)
				w = Math.max(availableWidth - x, child.minWidth);
		}
		
		if (checkHeight)
		{
			if (y + h > availableHeight)
				h = Math.max(availableHeight - y, child.minHeight);
		}
		
		if (!isNaN(w) && !isNaN(h))
			child.setActualSize(w, h);
	}
	
	/** 
	 *  @private
	 *  This function measures the bounds of the content area.
	 *  It looks at each child included in the layout, and determines
	 *  right and bottom edge.
	 *
	 *  When we are laying out the children, we use the larger of the
	 *  content area and viewable area to determine percentages and 
	 *  the edges for constraints.
	 *  
	 *  If the child has a percentageWidth or both left and right values
	 *  set, the minWidth is used for determining its area. Otherwise
	 *  the explicit or measured width is used. The same rules apply in 
	 *  the vertical direction.
	 */
	private function measureContentArea():Rectangle
	{
		if (_contentArea)
			return _contentArea;
		
		_contentArea = new Rectangle();

		var n:int = target.numChildren;
		for (var i:int = 0; i < n; i++)
		{
			var child:IUIComponent = target.getChildAt(i) as IUIComponent;
			var styleableChild:IStyleClient = child as IStyleClient;
		
			if (!child.includeInLayout)
				continue;
				
			var cx:Number = child.x;
			var cy:Number = child.y;
			var pw:Number = child.getExplicitOrMeasuredWidth();
			var ph:Number = child.getExplicitOrMeasuredHeight();
			
			if (!isNaN(child.percentWidth) ||
					(styleableChild && 
						!isNaN(styleableChild.getStyle("left")) && 
						!isNaN(styleableChild.getStyle("right"))))
			{
				pw = child.minWidth;
			}
			
			if (!isNaN(child.percentHeight) ||
					(styleableChild && 
						!isNaN(styleableChild.getStyle("top")) && 
						!isNaN(styleableChild.getStyle("bottom"))))
			{
				ph = child.minHeight;
			}
			
			r.x = cx
			r.y = cy
			r.width = pw;
			r.height = ph;
			applyAnchorStylesDuringMeasure(child, r);
			cx = r.x;
			cy = r.y;
			pw = r.width;
			ph = r.height;

			if (isNaN(cx))
				cx = child.x;
			if (isNaN(cy))
				cy = child.y;

			var rightEdge:Number = cx;
			var bottomEdge:Number = cy;

			if (isNaN(pw))
				pw = child.width;

			if (isNaN(ph))
				ph = child.height;

			rightEdge += pw;
			bottomEdge += ph;

			_contentArea.right = Math.max(_contentArea.right, rightEdge);
			_contentArea.bottom = Math.max(_contentArea.bottom, bottomEdge);
		}
		
		return _contentArea;
	}

	//--------------------------------------------------------------------------
	//
	//  Event handlers
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 *  If a child has been added, listen for its move event.
	 */
	private function target_childAddHandler(
								event:ChildExistenceChangedEvent):void
	{
		DisplayObject(event.relatedObject).addEventListener(
			MoveEvent.MOVE, child_moveHandler);
	}

	/**
	 *  @private
	 *  If a child has been removed, stop listening for its move event.
	 */
	private function target_childRemoveHandler(
								event:ChildExistenceChangedEvent):void
	{
		DisplayObject(event.relatedObject).removeEventListener(
			MoveEvent.MOVE, child_moveHandler);
	}

	/**
	 *  @private
	 *  If a child's position has changed, then the measured preferred
	 *  size of this canvas may have changed.
	 */
	private function child_moveHandler(event:MoveEvent):void
	{
		if (event.target is IUIComponent)
			if (!(IUIComponent(event.target).includeInLayout))
				return;

		var target:Container = super.target;
		if (target)
		{
			target.invalidateSize();
			target.invalidateDisplayList();
			_contentArea = null;
		}
	}
}

}
