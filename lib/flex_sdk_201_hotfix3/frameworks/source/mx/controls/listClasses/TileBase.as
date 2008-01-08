////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.listClasses
{

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.geom.Point;
import flash.ui.Keyboard;
import mx.collections.CursorBookmark;
import mx.collections.ItemResponder;
import mx.collections.errors.ItemPendingError;
import mx.controls.scrollClasses.ScrollBar;
import mx.core.ClassFactory;
import mx.core.EdgeMetrics;
import mx.core.FlexShape;
import mx.core.FlexSprite;
import mx.core.IFlexDisplayObject;
import mx.core.UIComponentGlobals;
import mx.core.mx_internal;
import mx.events.CollectionEvent;
import mx.events.CollectionEventKind;
import mx.events.DragEvent;
import mx.events.FlexEvent;
import mx.events.ListEvent;
import mx.events.ScrollEvent;
import mx.events.ScrollEventDetail;
import mx.events.ScrollEventDirection;
import mx.skins.halo.ListDropIndicator;
import mx.styles.StyleManager;

use namespace mx_internal;

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="maxHorizontalScrollPosition", kind="property")]
[Exclude(name="maxVerticalScrollPosition", kind="property")]

/**
 *  The TileBase class is the base class for controls
 *  that display data items in a sequence of rows and columns.
 *  TileBase-derived classes ignore the <code>variableRowHeight</code>
 *  and <code>wordWrap</code> properties inherited from their parent class.
 *  All items in a TileList are the same width and height.
 *
 *  <p>This class is not used directly in applications.</p>
 */
public class TileBase extends ListBase
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
	public function TileBase()
	{
		super();

		itemRenderer = new ClassFactory(TileListItemRenderer);

		// Set default sizes.
		setRowHeight(50);
		setColumnWidth(50);
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	// These three keep track of the key selection that caused
	// the page fault.
	
	private var bShiftKey:Boolean = false;
	
	private var bCtrlKey:Boolean = false;
	
	private var lastKey:uint = 0;
	
	private var bSelectItem:Boolean = false;

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	//----------------------------------
	//  direction
	//----------------------------------

	/**
	 *  @private
	 *  Storage for direction property.
	 */
	private var _direction:String = TileBaseDirection.HORIZONTAL;

	[Bindable("directionChanged")]
	[Inspectable(category="General", enumeration="vertical,horizontal", defaultValue="horizontal")]

	/**
	 *  The direction in which this control lays out its children.
	 *  Possible values are <code>TileBaseDirection.HORIZONTAL</code>
	 *  and <code>TileBaseDirection.VERTICAL</code>.
	 *  The default value is <code>TileBaseDirection.HORIZONTAL</code>.
	 *
	 *  <p>If the value is <code>TileBaseDirection.HORIZONTAL</code>, the tiles are
	 *  laid out along the first row until the number of visible columns or maxColumns
	 *  is reached and then a new row is started.  If more rows are created
	 *  than can be displayed at once, the control will display a vertical scrollbar.
	 *  The opposite is true if the value is <code>TileBaseDirection.VERTICAL</code>.</p>
	 */
	public function get direction():String
	{
		return _direction;
	}

	/**
	 *  @private
	 */
	public function set direction(value:String):void
	{
		_direction = value;

		itemsSizeChanged = true;
		invalidateSize();
		invalidateDisplayList();

		dispatchEvent(new Event("directionChanged"));
	}

	//----------------------------------
	//  maxColumns
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the maxColumns property.
	 */
	private var _maxColumns:int = 0;

    [Inspectable(category="General", defaultValue="0")]

	/**
	 *  The maximum number of columns that the control can have.
	 *  If 0, then there are no limits to the number of
	 *  columns.  This value is ignored
	 *  if the direction is <code>TileBaseDirection.VERTICAL</code>
	 *  because the control will have as many columns as it needs to 
	 *  to display all the data.
	 *
	 *  <p>The default value is 0 (no limit).</p>
	 */
	public function get maxColumns():int
	{
		return _maxColumns;
	}

	/**
	 *  @private
	 */
	public function set maxColumns(value:int):void
	{
		if (_maxColumns != value)
		{
			_maxColumns = value;

			invalidateSize();
			invalidateDisplayList();
		}
	}

	//----------------------------------
	//  maxRows
	//----------------------------------

	/**
	 *  @private
	 *  Storage for the maxRows property.
	 */
	private var _maxRows:int = 0;

    [Inspectable(category="General", defaultValue="0")]

	/**
	 *  The maximum number of rows that the control can have.
	 *  If 0, then there is no limit to the number of
	 *  rows.  This value is ignored
	 *  if the direction is <code>TileBaseDirection.HORIZONTAL</code>
	 *  because the control will have as many rows as it needs to 
	 *  to display all the data.
	 *
	 *  <p>The default value is 0 (no limit).</p>
	 */
	public function get maxRows():int
	{
		return _maxRows;
	}

	/**
	 *  @private
	 */
	public function set maxRows(value:int):void
	{
		if (_maxRows != value)
		{
			_maxRows = value;

			invalidateSize();
			invalidateDisplayList();
		}
	}

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
	override protected function createChildren():void
	{
		super.createChildren();

		listContent.mask = maskShape;
	}

	/**
	 *  @private
	 */
	override protected function makeRowsAndColumns(left:Number, top:Number,
												right:Number, bottom:Number,
												firstCol:int, firstRow:int,
												byCount:Boolean = false, rowsNeeded:uint = 0):Point
	{
		// trace(this, "makeRowsAndColumns " + left + " " + top + " " + right + " " + bottom + " " + firstCol + " " + firstRow);
		
		var numRows:int;
		var numCols:int;
		var colNum:int;
		var rowNum:int;
		var xx:Number;
		var yy:Number;
		var data:Object;
		var rowData:ListData;
		var uid:String
		var oldItem:IListItemRenderer 
		var item:IListItemRenderer;
		var more:Boolean;
		var valid:Boolean;
		var i:int;
		var rh:Number;

		var bSelected:Boolean = false;
		var bHighlight:Boolean = false;
		var bCaret:Boolean = false;
		
		if (columnWidth == 0 || rowHeight == 0)
			return null;
			
		if (direction == TileBaseDirection.VERTICAL)
		{
			numRows = maxRows > 0 ? maxRows : Math.max(Math.floor(listContent.height / rowHeight), 1);
			numCols = maxColumns > 0 ? maxColumns : Math.max(Math.ceil(listContent.width / columnWidth), 1);
			setRowCount(numRows);
			setColumnCount(numCols);
			colNum = firstCol;
			xx = left;
			more = (iterator != null && !iterator.afterLast && iteratorValid);
			while (colNum < numCols)
			{
				rowNum = firstRow;
				yy = top;
				while (rowNum < numRows)
				{
					valid = more;
					data = more ? iterator.current : null;
					if (iterator && more)
					{
						try 
						{
							more = iterator.moveNext();
						}
						catch (e1:ItemPendingError)
						{
							lastSeekPending = new ListBaseSeekPending(CursorBookmark.CURRENT, 0);
							e1.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
														lastSeekPending));
							more = false;
							iteratorValid = false;
						}
					}

					if (!listItems[rowNum])
						listItems[rowNum] = [];

					if (valid && yy < bottom)
					{
						uid = itemToUID(data);
						oldItem = listItems[rowNum][colNum];
						if (oldItem)
						{
							delete rowMap[oldItem.name];
							item = oldItem;
						}
						else
						{
 							item = itemRenderer.newInstance();
 							item.owner = this;
							item.styleName = listContent;
						}
						rowData = ListData(makeListData(data, uid, rowNum, colNum));
						rowMap[item.name] = rowData;
						if (item is IDropInListItemRenderer)
							IDropInListItemRenderer(item).listData = data ? rowData : null;
						if (item.data != data)
							item.data = data;
						if (oldItem == null)
							listContent.addChild(DisplayObject(item));
						item.visible = true;
						if (uid)
							visibleData[uid] = item;
						listItems[rowNum][colNum] = item;
						UIComponentGlobals.layoutManager.validateClient(item, true);
						rh = item.getExplicitOrMeasuredHeight();
						if (item.width != columnWidth || rh != (rowHeight - cachedPaddingTop - cachedPaddingBottom))
							item.setActualSize(columnWidth, rowHeight - cachedPaddingTop - cachedPaddingBottom);
						item.move(xx, yy + cachedPaddingTop);
						bSelected = selectedData[uid] != null;
						bHighlight = highlightUID == uid;
						bCaret = caretUID == uid;
						if (uid)
							drawItem(item, bSelected, bHighlight, bCaret);
					}
					else
					{
						oldItem = listItems[rowNum][colNum];
						if (oldItem)
						{
							listContent.removeChild(DisplayObject(oldItem));
							delete rowMap[oldItem.name];
							listItems[rowNum][colNum] = null;
						}
					}
					rowInfo[rowNum] = new ListRowInfo(yy, rowHeight, uid);
					yy += rowHeight;
					rowNum++;
				}
				colNum ++;
				if (firstRow)
				{
					// we're doing a row along the bottom so we have to skip the beginning of the next column
					for (i = 0; i < firstRow; i++)
					{
						if (iterator && more)
						{
							try 
							{
								more = iterator.moveNext();
							}
							catch (e2:ItemPendingError)
							{
								lastSeekPending = new ListBaseSeekPending(CursorBookmark.CURRENT, 0);
								e2.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
														lastSeekPending));
								more = false;
								iteratorValid = false;
							}
						}
					}
				}
				xx += columnWidth;
			}
		}
		else // horizontal
		{
			numCols = maxColumns > 0 ? maxColumns : Math.max(Math.floor(listContent.width / columnWidth), 1);
			numRows = maxRows > 0 ? maxRows : Math.max(Math.ceil(listContent.height / rowHeight), 1);
			setColumnCount(numCols);
			setRowCount(numRows);
			rowNum = firstRow;
			yy = top;
			more = (iterator != null && !iterator.afterLast && iteratorValid);
			while (rowNum < numRows)
			{
				colNum = firstCol;
				xx = left;
				rowInfo[rowNum] = null;
				while (colNum < numCols)
				{
					valid = more;
					data = more ? iterator.current : null;
					if (iterator && more)
					{
						try 
						{
							more = iterator.moveNext();
						}
						catch (e3:ItemPendingError)
						{
							lastSeekPending = new ListBaseSeekPending(CursorBookmark.CURRENT, 0);
							e3.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
														lastSeekPending));
							more = false;
							iteratorValid = false;
						}
					}

					if (!listItems[rowNum])
						listItems[rowNum] = [];

					if (valid && xx < right)
					{
						uid = itemToUID(data);
						oldItem = listItems[rowNum][colNum];
						if (oldItem)
						{
							delete rowMap[oldItem.name];
							item = oldItem;
						}
						else
						{
 							item = itemRenderer.newInstance();
 							item.owner = this;
							item.styleName = listContent;
						}
						rowData = ListData(makeListData(data, uid, rowNum, colNum));
						rowMap[item.name] = rowData;
						if (item is IDropInListItemRenderer)
							IDropInListItemRenderer(item).listData = data ? rowData : null;
						if (item.data != data)
							item.data = data;
						if (oldItem == null)
							listContent.addChild(DisplayObject(item));
						item.visible = true;
						if (uid)
							visibleData[uid] = item;
						listItems[rowNum][colNum] = item;
						UIComponentGlobals.layoutManager.validateClient(item, true);
						rh = item.getExplicitOrMeasuredHeight();
						if (item.width != columnWidth || rh != (rowHeight - cachedPaddingTop - cachedPaddingBottom))
							item.setActualSize(columnWidth, rowHeight - cachedPaddingTop - cachedPaddingBottom);
						item.move(xx, yy + cachedPaddingTop);
						bSelected = selectedData[uid] != null;
						bHighlight = highlightUID == uid;
						bCaret = caretUID == uid;
						if (!rowInfo[rowNum])
							rowInfo[rowNum] = new ListRowInfo(yy, rowHeight, uid);
						if (uid)
							drawItem(item, bSelected, bHighlight, bCaret);
					}
					else
					{
						if (!rowInfo[rowNum])
							rowInfo[rowNum] = new ListRowInfo(yy, rowHeight, uid);

						oldItem = listItems[rowNum][colNum];
						if (oldItem)
						{
							listContent.removeChild(DisplayObject(oldItem));
							delete rowMap[oldItem.name];
							listItems[rowNum][colNum] = null;
						}
					}
					xx += columnWidth;
					colNum++;
				}
				rowNum ++;
				if (firstCol)
				{
					// we're doing a column along the side so we have to skip the beginning of the next column
					for (i = 0; i < firstCol; i++)
					{
						if (iterator && more)
						{
							try 
							{
								more = iterator.moveNext();
							}
							catch (e4:ItemPendingError)
							{
								lastSeekPending = new ListBaseSeekPending(CursorBookmark.CURRENT, 0)
								e4.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
														lastSeekPending));
								more = false;
								iteratorValid = false;
							}
						}
					}
				}
				yy += rowHeight;
			}
		}

		if (!byCount)
		{
			var a:Array;
			// prune excess rows and columns
			while (listItems.length > numRows)
			{
				a = listItems.pop();
				rowInfo.pop();
				for (i = 0; i < a.length; i++)
				{
					oldItem = a[i];
					if (oldItem)
					{
						listContent.removeChild(DisplayObject(oldItem));
						delete rowMap[oldItem.name];
					}
				}
			}
			if (listItems.length && listItems[0].length > numCols)
			{
				for (i = 0; i < numRows; i++)
				{
					a = listItems[i];
					while (a.length > numCols)
					{
						oldItem = a.pop();
						if (oldItem)
						{
							listContent.removeChild(DisplayObject(oldItem));
							delete rowMap[oldItem.name];
						}
					}
				}
			}
		}

		return new Point(xx, yy);
	}

	/**
	 *  @private
	 */
	override protected function configureScrollBars():void
	{
		var rowCount:int = listItems.length;
		if (rowCount == 0)
			return;
		
		var colCount:int = listItems[0].length;
		if (colCount == 0)
			return;

		if (rowCount > 1 && rowCount * rowHeight > listContent.height)
			rowCount--;
		
		if (colCount > 1 && colCount * columnWidth > listContent.width)
			colCount--;

		var oldHorizontalScrollBar:Object = horizontalScrollBar;
		var oldVerticalScrollBar:Object = verticalScrollBar;

		var numRows:int;
		var numCols:int;
		var index:int;
		
		if (direction == TileBaseDirection.VERTICAL)
		{
			// handle extra blank items at end of list.  This is the equivalent
			// of adjustVerticalScrollPositionDownward in List.as
			if (horizontalScrollPosition > 0)
			{
				var fillerCols:int = 0;
				// adjust colCount for null items
				while ((colCount > 0) && listItems[0][colCount - 1] == null)
				{
					colCount--
					fillerCols++;
				}
				if (fillerCols)
				{
					horizontalScrollPosition -= fillerCols;
					index = scrollPositionToIndex(horizontalScrollPosition, verticalScrollPosition);
					try
					{
						iterator.seek(CursorBookmark.FIRST, index);
					}
					catch (e:ItemPendingError)
					{
						lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index)
						e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler,
															lastSeekPending));
						iteratorValid = false;
					}
					updateList();
					return;
				}
			}
			numRows = maxRows > 0 ? maxRows : rowCount;
			// we take out partialColumn if there's no collection because it'll get factored back in
			// and we want the math to workout that there's no scrollbars
			numCols = collection ? Math.ceil(collection.length / numRows) : colCount;
		}
		else
		{
			// handle extra blank items at end of list.  This is the equivalent
			// of adjustVerticalScrollPositionDownward in List.as
			if (verticalScrollPosition > 0 && listItems[rowCount - 1][0] == null)
			{
				var fillerRows:int = 0;
				while ((rowCount > 0) && listItems[rowCount - 1][0] == null)
				{
					rowCount--;
					fillerRows++;
				}
				if (fillerRows)
				{
					verticalScrollPosition -= fillerRows;
					index = scrollPositionToIndex(horizontalScrollPosition, verticalScrollPosition);
					try
					{
						iterator.seek(CursorBookmark.FIRST, index);
					}
					catch (e:ItemPendingError)
					{
						lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index)
						e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler,
														lastSeekPending));
						iteratorValid = false;
					}
					updateList();
					return;
				}
			}
			numCols = maxColumns > 0 ? maxColumns : colCount;
			// we take out partialRow if there's no collection because it'll get factored back in
			// and we want the math to workout that there's no scrollbars
			numRows = collection ? Math.ceil(collection.length / numCols) : rowCount;
		}
		
		// Depending on the direction of the Tile control the colCount and
		// rowCount can be greater than numRows and numCols, resulting in 
		// negative values for H and V scroll positions. 
		// We ignore them when they are negative.
		maxHorizontalScrollPosition = Math.max(0, numCols - colCount);
		maxVerticalScrollPosition = Math.max(0, numRows - rowCount);

		setScrollBarProperties(numCols, colCount, numRows, rowCount);
	}

	/**
	 *  @private
	 *  Move any rows that don't need rerendering
	 *  Move and rerender any rows left over.
	 */
    override protected function scrollVertically(pos:int, deltaPos:int,
											  scrollUp:Boolean):void
    {
		var numRows:int;
		var numCols:int;
		var curY:Number;
		var uid:String;
		var index:int;

		// remove the clip mask that was applied to items in the last row of the list
		removeClipMask();

		var moveBlockDistance:Number = deltaPos * rowHeight;
		// toss the old rows
		for (var i:int = 0; i < deltaPos; i++)
		{
			numCols = scrollUp ? listItems[lockedRowCount + i].length : listItems[rowCount - i - 1].length;
			for (var j:int = 0; j < columnCount && j < numCols; j++)
			{
				var r:IListItemRenderer = scrollUp ? listItems[lockedRowCount + i][j] : listItems[rowCount - i - 1][j];
				if (r)
				{
					listContent.removeChild(DisplayObject(r));
					delete visibleData[rowMap[r.name].uid];
					removeIndicators(rowMap[r.name].uid);
					delete rowMap[r.name];
				}
			}
		}

		if (scrollUp)
		{

			// move the rows that don't change
			// note that we start from zero because we've taken some rows
			// off and put them on the free list already
			curY = 0;
			for (i = lockedRowCount + deltaPos; i < rowCount; i++)
			{
				numCols = listItems[i].length;
				for (j = 0; j < columnCount && j < numCols; j++)
				{
					r = listItems[i][j];
					listItems[i - deltaPos][j] = r;
					if (r)
					{
						r.y -= moveBlockDistance;
						rowMap[r.name].rowIndex -= deltaPos;
						uid = rowMap[r.name].uid;
						if (selectionIndicators[uid])
							selectionIndicators[uid].y -= moveBlockDistance;
						if (highlightUID == uid)
							highlightIndicator.y -= moveBlockDistance;
						if (caretUID == uid)
							caretIndicator.y -= moveBlockDistance;
					}
				}
				
				// when the row has less number of columns
				// we need to clean up the row.
				if (numCols < columnCount)
				{
					for (j = numCols; j < columnCount; ++j)
					{
						listItems[i-deltaPos][j] = null;
					}
				}
				rowInfo[i - deltaPos] = rowInfo[i];
				rowInfo[i - deltaPos].y -= moveBlockDistance;
				curY = rowInfo[i - deltaPos].y + rowHeight;
			}
			listItems.splice(rowCount - deltaPos);
			index = indicesToIndex(verticalScrollPosition + rowCount - deltaPos, horizontalScrollPosition);
			try
			{
				iterator.seek(CursorBookmark.FIRST, index);
			}
			catch (e:ItemPendingError)
			{
				lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index);
				e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
				iteratorValid = false;
			}
			makeRowsAndColumns(0, curY, listContent.width, listContent.height, 0, rowCount - deltaPos);
			index = indicesToIndex(verticalScrollPosition, horizontalScrollPosition);
			try
			{
				iterator.seek(CursorBookmark.FIRST, index);
			}
			catch (e:ItemPendingError)
			{
				lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index)
				e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
				iteratorValid = false;
			}
		}
		else
		{
			// move the rows that don't change
			// note that we start from zero because we've taken some rows
			// off and put them on the free list already
			curY = rowInfo[deltaPos].y;
			for (i = rowCount - deltaPos - 1; i >= 0; i--)
			{
				numCols = listItems[i].length;
				for (j = 0; j < columnCount && j < numCols; j++)
				{
					r = listItems[i][j];
					if (r)
					{
						r.y += moveBlockDistance;
						rowMap[r.name].rowIndex += deltaPos;
						uid = rowMap[r.name].uid;
						listItems[i + deltaPos][j] = r;
						if (selectionIndicators[uid])
							selectionIndicators[uid].y += moveBlockDistance;
						if (highlightUID == uid)
							highlightIndicator.y += moveBlockDistance;
						if (caretUID == uid)
							caretIndicator.y += moveBlockDistance;
					}
					else
						listItems[i + deltaPos][j] = null;
				}
				rowInfo[i + deltaPos] = rowInfo[i];
				rowInfo[i + deltaPos].y += moveBlockDistance;
			}
			for (i = 0; i < deltaPos; i++)
			{
				for (j = 0; j < columnCount; j++)
				{
					listItems[i][j] = null;
				}
			}

			index = indicesToIndex(verticalScrollPosition, horizontalScrollPosition);
			try
			{
				iterator.seek(CursorBookmark.FIRST, index);
			}
			catch (e:ItemPendingError)
			{
				lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index);
				e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
				iteratorValid = false;
			}
			makeRowsAndColumns(0, 0, listContent.width, curY, 0, 0, true);
			index = indicesToIndex(verticalScrollPosition, horizontalScrollPosition);
			try
			{
				iterator.seek(CursorBookmark.FIRST, index);
			}
			catch (e:ItemPendingError)
			{
				lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index);
				e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
				iteratorValid = false;
			}
		}

		// if needed, add a clip mask to the items in the last row of the list
		addClipMask(false);
	}

	/**
	 *  @private
	 *  Move any rows that don't need rerendering.
	 *  Move and rerender any rows left over.
	 */
    override protected function scrollHorizontally(pos:int, deltaPos:int,
											    scrollUp:Boolean):void
    {
		if (deltaPos == 0)
			return;
	
		var numRows:int;
		var numCols:int;
		var curX:Number;
		var uid:String;
		var index:int;

		// remove the clip mask that was applied to items in the last row of the list
		removeClipMask();

		var moveBlockDistance:Number = deltaPos * columnWidth;
		// toss the old rows
		for (var i:int = 0; i < deltaPos; i++)
		{
			for (var j:int = 0; j < rowCount; j++)
			{
				var r:IListItemRenderer = scrollUp ? listItems[j][lockedColumnCount + i] : listItems[j][columnCount - i - 1];
				if (r)
				{
					listContent.removeChild(DisplayObject(r));
					delete visibleData[rowMap[r.name].uid];
					removeIndicators(rowMap[r.name].uid);
					delete rowMap[r.name];
				}
			}
		}

		if (scrollUp)
		{

			// move the rows that don't change
			// note that we start from zero because we've taken some rows
			// off and put them on the free list already
			curX = 0;
			for (i = lockedColumnCount + deltaPos; i < columnCount; i++)
			{
				for (j = 0; j < rowCount; j++)
				{
					var temp:IListItemRenderer = listItems[j][i];
					if (temp)
					{
						r = temp;
						r.x -= moveBlockDistance;
						uid = rowMap[r.name].uid;
						listItems[j][i - deltaPos] = r;
						if (selectionIndicators[uid])
							selectionIndicators[uid].x -= moveBlockDistance;
						if (highlightUID == uid)
							highlightIndicator.x -= moveBlockDistance;
						if (caretUID == uid)
							caretIndicator.x -= moveBlockDistance;
					}
					else
						listItems[j][i - deltaPos] = null;
				}
				curX = r.x + r.width;
			}
			for (i = 0; i < deltaPos; i++)
			{
				for (j = 0; j < rowCount; j++)
				{
					listItems[j][columnCount - i - 1] = null;
				}
			}
			index = indicesToIndex(verticalScrollPosition, horizontalScrollPosition + columnCount - deltaPos);
			try
			{
				iterator.seek(CursorBookmark.FIRST, index);
			}
			catch (e:ItemPendingError)
			{
				lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index);
				e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
				iteratorValid = false;
			}
			makeRowsAndColumns(curX, 0, listContent.width, listContent.height, columnCount - deltaPos, 0);
			index = indicesToIndex(verticalScrollPosition, horizontalScrollPosition);
			try
			{
				iterator.seek(CursorBookmark.FIRST, index);
			}
			catch (e:ItemPendingError)
			{
				lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index);
				e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
				iteratorValid = false;
			}
		}
		else
		{
			curX = listItems[0][deltaPos -1].x + listItems[0][deltaPos -1].width;
			for (i = columnCount - deltaPos - 1; i >= 0; i--)
			{
				for (j = 0; j < rowCount; j++)
				{
					r = listItems[j][i];
					if (r)
					{
						r.x += moveBlockDistance;
						uid = rowMap[r.name].uid;
						listItems[j][i + deltaPos] = r;
						if (selectionIndicators[uid])
							selectionIndicators[uid].x += moveBlockDistance;
						if (highlightUID == uid)
							highlightIndicator.x += moveBlockDistance;
						if (caretUID == uid)
							caretIndicator.x += moveBlockDistance;
					}
				}
			}
			for (i = 0; i < deltaPos; i++)
			{
				for (j = 0; j < rowCount; j++)
				{
					listItems[j][i] = null;
				}
			}

			index = indicesToIndex(verticalScrollPosition, horizontalScrollPosition);
			try
			{
				iterator.seek(CursorBookmark.FIRST, index);
			}
			catch (e:ItemPendingError)
			{
				lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index);
				e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
				iteratorValid = false;
			}
			makeRowsAndColumns(0, 0, curX, listContent.height, 0, 0, true);
			index = indicesToIndex(verticalScrollPosition, horizontalScrollPosition);
			try
			{
				iterator.seek(CursorBookmark.FIRST, index);
			}
			catch (e:ItemPendingError)
			{
				lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, index);
				e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
				iteratorValid = false;
			}
		}

		// if needed, add a clip mask to the items in the last row of the list
		addClipMask(false);
	}

	/**
	 *  @private
	 */
	override protected function moveSelectionVertically(
									code:uint, shiftKey:Boolean, 
                                    ctrlKey:Boolean):void
	{
		var newVerticalScrollPosition:Number;
		var newHorizontalScrollPosition:Number;
		var listItem:IListItemRenderer;
		var uid:String;
		var len:int;
		var selected:Boolean;
		var bSelChanged:Boolean = false;
		var rowIndex:int;
		var colIndex:int;
		var numRows:int = maxRows > 0 ? maxRows : rowCount;
		var partialColumn:int = (listItems[0].length > columnCount - 1 && listItems[0][columnCount - 1] &&
								listItems[0][columnCount - 1].x + listItems[0][columnCount - 1].width > listContent.width) ?
								1 : 0;
		var partialRow:int = ((listItems[rowCount-1].length == 0) || listItems[rowCount-1].length && listItems[rowCount-1][0] &&
							  listItems[rowCount-1][0].y + listItems[rowCount-1][0].height > listContent.height) ?
							  1 : 0;

		if (!collection)
			return;

		showCaret = true;

		switch (code)
		{
			case Keyboard.UP:
			{
				if (caretIndex > 0)
				{
					if (direction == TileBaseDirection.VERTICAL)
						--caretIndex;
					else
					{
						rowIndex = indexToRow(caretIndex);
						colIndex = indexToColumn(caretIndex);
						if (rowIndex == 0)
						{
							colIndex--;
							rowIndex = lastRowInColumn(colIndex);
						}
						else
							rowIndex--;
						caretIndex = Math.min(indicesToIndex(rowIndex, colIndex), collection.length - 1);
					}

					rowIndex = indexToRow(caretIndex);
					colIndex = indexToColumn(caretIndex);
					// scroll up if we need to
					if (rowIndex < verticalScrollPosition)
						newVerticalScrollPosition = verticalScrollPosition - 1;

					// wrap down if we need to
					if (rowIndex > verticalScrollPosition + rowCount - partialRow)
						newVerticalScrollPosition = maxVerticalScrollPosition;

					if (colIndex < horizontalScrollPosition)
						newHorizontalScrollPosition = horizontalScrollPosition - 1;
				}
				break;
			}

			case Keyboard.DOWN:
			{
				if (caretIndex < collection.length - 1)
				{
					if (direction == TileBaseDirection.VERTICAL
							|| caretIndex == -1)
					{
						++caretIndex;
					}
					else
					{
						rowIndex = indexToRow(caretIndex);
						colIndex = indexToColumn(caretIndex);
						if (rowIndex == lastRowInColumn(colIndex))
						{
							rowIndex = 0;
							colIndex++;
						}
						else
						{
							rowIndex++;
						}
						caretIndex = Math.min(indicesToIndex(rowIndex, colIndex), collection.length - 1);
					}

					rowIndex = indexToRow(caretIndex);
					colIndex = indexToColumn(caretIndex);

					if (rowIndex >= verticalScrollPosition + rowCount - partialRow &&
						verticalScrollPosition < maxVerticalScrollPosition)
					{
						newVerticalScrollPosition = verticalScrollPosition + 1;
					}

					if (rowIndex < verticalScrollPosition)
						newVerticalScrollPosition = rowIndex;

					if (colIndex > horizontalScrollPosition + columnCount - 1)
						newHorizontalScrollPosition = horizontalScrollPosition + 1;

				}
				break;
			}

			case Keyboard.PAGE_UP:
			{
				if (caretIndex < 0)
					caretIndex = scrollPositionToIndex(horizontalScrollPosition, verticalScrollPosition);
					
				rowIndex = indexToRow(caretIndex);
				colIndex = indexToColumn(caretIndex);
				if (verticalScrollPosition > 0)
				{
					if (rowIndex == verticalScrollPosition)
						newVerticalScrollPosition = rowIndex = Math.max(verticalScrollPosition - (rowCount - partialRow), 0);
					else
						rowIndex = verticalScrollPosition;

					caretIndex = indicesToIndex(rowIndex, colIndex);
					// this break is here so we fall throught to .HOME otherwise
					break;
				}
			}

			case Keyboard.HOME:
			{
				if (collection.length)
				{
					caretIndex = 0;
					newVerticalScrollPosition = 0;
					newHorizontalScrollPosition = 0;
				}
				break;
			}

			case Keyboard.PAGE_DOWN:
			{
				if (caretIndex < 0)
					caretIndex = scrollPositionToIndex(horizontalScrollPosition, verticalScrollPosition);

				rowIndex = indexToRow(caretIndex);
				colIndex = indexToColumn(caretIndex);
				
				if (rowIndex < maxVerticalScrollPosition)
				{
					if (rowIndex == verticalScrollPosition + (rowCount - partialRow))
					{
						newVerticalScrollPosition = Math.min(verticalScrollPosition + rowCount - partialRow, maxVerticalScrollPosition);
						rowIndex = verticalScrollPosition + rowCount;
					}
					else
					{
						rowIndex = Math.min(verticalScrollPosition + rowCount - partialRow, indexToRow(collection.length - 1));
						if (rowIndex == verticalScrollPosition + rowCount - partialRow)
							newVerticalScrollPosition = Math.min(verticalScrollPosition + rowCount - partialRow, maxVerticalScrollPosition);
					}

					caretIndex = Math.min(indicesToIndex(rowIndex, colIndex), collection.length - 1);
					// this break is here so we fall through to .END otherwise
					break;
				}
			}

			case Keyboard.END:
			{
				if (caretIndex < collection.length)
				{
					caretIndex = collection.length - 1;
					newVerticalScrollPosition = maxVerticalScrollPosition;
					newHorizontalScrollPosition = maxHorizontalScrollPosition;
				}
				break;
			}
		}

		var scrollEvent:ScrollEvent;

		if (!isNaN(newVerticalScrollPosition))
		{
			if (newVerticalScrollPosition != verticalScrollPosition)
			{
				scrollEvent = new ScrollEvent(ScrollEvent.SCROLL);
				scrollEvent.detail = ScrollEventDetail.THUMB_POSITION;
				scrollEvent.direction = ScrollEventDirection.VERTICAL;
				scrollEvent.delta = newVerticalScrollPosition - verticalScrollPosition;
				scrollEvent.position = newVerticalScrollPosition;
				verticalScrollPosition = newVerticalScrollPosition;
				dispatchEvent(scrollEvent);
			}
		}

		if (iteratorValid)
		{
			if (!isNaN(newHorizontalScrollPosition))
			{
				if (newHorizontalScrollPosition != horizontalScrollPosition)
				{
					scrollEvent = new ScrollEvent(ScrollEvent.SCROLL);
					scrollEvent.detail = ScrollEventDetail.THUMB_POSITION;
					scrollEvent.direction = ScrollEventDirection.HORIZONTAL;
					scrollEvent.delta = newHorizontalScrollPosition - horizontalScrollPosition;
					scrollEvent.position = newHorizontalScrollPosition;
					horizontalScrollPosition = newHorizontalScrollPosition;
					dispatchEvent(scrollEvent);
				}
			}
		}

		if (!iteratorValid)
		{
			keySelectionPending = true;
			return;
		}

		bShiftKey = shiftKey;
		bCtrlKey = ctrlKey;
		lastKey = code;

		finishKeySelection();
	}

	/**
	 *  @private
	 */
	override protected function moveSelectionHorizontally(
								code:uint, shiftKey:Boolean,
								ctrlKey:Boolean):void
	{
		var newVerticalScrollPosition:Number;
		var newHorizontalScrollPosition:Number;
		var listItem:IListItemRenderer;
		var uid:String;
		var len:int;
		var selected:Boolean;
		var rowIndex:int;
		var colIndex:int;
		var numCols:int = maxColumns > 0 ? maxColumns : columnCount;
		var partialColumn:int = (listItems[0].length > columnCount - 1 && listItems[0][columnCount - 1] &&
								listItems[0][columnCount - 1].x + listItems[0][columnCount - 1].width > listContent.width) ?
								1 : 0;
		var partialRow:int = (listItems[rowCount-1].length && listItems[rowCount-1][0] &&
							  listItems[rowCount-1][0].y + listItems[rowCount-1][0].height > listContent.height) ?
							  1 : 0;

		if(!collection)
			return;

		showCaret = true;

		switch (code)
		{
			case Keyboard.LEFT:
			{
				if (caretIndex > 0)
				{
					if (direction == TileBaseDirection.HORIZONTAL)
					{
						--caretIndex;
					}
					else
					{
						rowIndex = indexToRow(caretIndex);
						colIndex = indexToColumn(caretIndex);
						if (colIndex == 0)
						{
							rowIndex--;
							colIndex = lastColumnInRow(rowIndex);
						}
						else
						{
							colIndex--;
						}
						caretIndex = Math.min(indicesToIndex(rowIndex, colIndex), collection.length - 1);
					}

					rowIndex = indexToRow(caretIndex);
					colIndex = indexToColumn(caretIndex);
					if (direction == TileBaseDirection.HORIZONTAL)
					{
						// scroll up if we need to
						if (rowIndex < verticalScrollPosition)
						{
							newVerticalScrollPosition = verticalScrollPosition - 1;
						}
						// wrap down if we need to
						else if (rowIndex > verticalScrollPosition + rowCount - partialRow)
						{
							newVerticalScrollPosition = maxVerticalScrollPosition;
						}
					}
					else
					{
						// scroll left if we need to
						if (colIndex < horizontalScrollPosition)
						{
							newHorizontalScrollPosition = horizontalScrollPosition - 1;
						}
						// wrap right if we need to
						else if (colIndex > horizontalScrollPosition + columnCount - 1 - partialColumn)
						{
							newHorizontalScrollPosition = maxHorizontalScrollPosition;
						}
					}
				}
				break;
			}

			case Keyboard.RIGHT:
			{
				if (caretIndex < collection.length - 1)
				{
					if (direction == TileBaseDirection.HORIZONTAL
							|| caretIndex == -1)
					{
						++caretIndex;
					}
					else
					{
						rowIndex = indexToRow(caretIndex);
						colIndex = indexToColumn(caretIndex);
						if (colIndex == lastColumnInRow(rowIndex))
						{
							colIndex = 0;
							rowIndex++;
						}
						else
						{
							colIndex++;
						}
						caretIndex = Math.min(indicesToIndex(rowIndex, colIndex), collection.length - 1);
					}

					rowIndex = indexToRow(caretIndex);
					colIndex = indexToColumn(caretIndex);
					if (direction == TileBaseDirection.HORIZONTAL)
					{
						if (rowIndex >= verticalScrollPosition + rowCount - partialRow &&
							verticalScrollPosition < maxVerticalScrollPosition)
						{
							newVerticalScrollPosition = verticalScrollPosition + 1;
						}
						if (rowIndex < verticalScrollPosition)
						{
							newVerticalScrollPosition = rowIndex;
						}
					}
					else
					{
						if (colIndex >= horizontalScrollPosition + columnCount - partialColumn &&
							horizontalScrollPosition < maxHorizontalScrollPosition)
						{
							newHorizontalScrollPosition = horizontalScrollPosition + 1;
						}
						if (colIndex < horizontalScrollPosition)
						{
							newHorizontalScrollPosition = colIndex;
						}
					}
				}
				break;
			}

			case Keyboard.PAGE_UP:
			{
				if (caretIndex < 0)
					caretIndex = scrollPositionToIndex(horizontalScrollPosition, verticalScrollPosition);
				rowIndex = indexToRow(caretIndex);
				colIndex = indexToColumn(caretIndex);
				if (colIndex > 0)
				{
					newHorizontalScrollPosition = colIndex = Math.max(horizontalScrollPosition - (columnCount - partialColumn), 0);

					caretIndex = indicesToIndex(rowIndex, colIndex);
				}
				break;
			}
		
			case Keyboard.PAGE_DOWN:
			{
				if (caretIndex < 0)
					caretIndex = scrollPositionToIndex(horizontalScrollPosition, verticalScrollPosition);
				rowIndex = indexToRow(caretIndex);
				colIndex = indexToColumn(caretIndex);
				if (colIndex < maxHorizontalScrollPosition)
				{
					colIndex = Math.min(horizontalScrollPosition + columnCount - partialColumn, indexToColumn(collection.length - 1));

					if (colIndex > horizontalScrollPosition)
						newHorizontalScrollPosition = colIndex;

					caretIndex = indicesToIndex(rowIndex, colIndex);
				}
				break;
			}

			case Keyboard.HOME:
			{
				if (collection.length)
				{
					caretIndex = 0;
					newHorizontalScrollPosition = 0;
					newVerticalScrollPosition = 0;
				}
				break;
			}

			case Keyboard.END:
			{
				if (caretIndex < collection.length)
				{
					caretIndex = collection.length - 1;
					newHorizontalScrollPosition = maxHorizontalScrollPosition;
					newVerticalScrollPosition = maxVerticalScrollPosition;
				}
				break;
			}
		}

		var scrollEvent:ScrollEvent;

		if (!isNaN(newVerticalScrollPosition))
		{
			if (newVerticalScrollPosition != verticalScrollPosition)
			{
				scrollEvent = new ScrollEvent(ScrollEvent.SCROLL);
				scrollEvent.detail = ScrollEventDetail.THUMB_POSITION;
				scrollEvent.direction = ScrollEventDirection.VERTICAL;
				scrollEvent.delta = newVerticalScrollPosition - verticalScrollPosition;
				scrollEvent.position = newVerticalScrollPosition;
				verticalScrollPosition = newVerticalScrollPosition;
				dispatchEvent(scrollEvent);
			}
		}

		if (iteratorValid)
		{
			if (!isNaN(newHorizontalScrollPosition))
			{
				if (newHorizontalScrollPosition != horizontalScrollPosition)
				{
					scrollEvent = new ScrollEvent(ScrollEvent.SCROLL);
					scrollEvent.detail = ScrollEventDetail.THUMB_POSITION;
					scrollEvent.direction = ScrollEventDirection.HORIZONTAL;
					scrollEvent.delta = newHorizontalScrollPosition - horizontalScrollPosition;
					scrollEvent.position = newHorizontalScrollPosition;
					horizontalScrollPosition = newHorizontalScrollPosition;
					dispatchEvent(scrollEvent);
				}
			}
		}

		if (!iteratorValid)
		{
			keySelectionPending = true;
			return;
		}

		bShiftKey = shiftKey;
		bCtrlKey = ctrlKey;
		lastKey = code;

		finishKeySelection();
	}
		
	/**
	 *  @private
	 */
	override protected function finishKeySelection():void
	{
        var uid:String;
		var bSelChanged:Boolean = false;
		var rowIndex:int;
		var colIndex:int;
		var listItem:IListItemRenderer;

		if (caretIndex < 0)
			return;

		rowIndex = indexToRow(caretIndex);
		colIndex = indexToColumn(caretIndex);

		listItem = listItems[rowIndex - verticalScrollPosition + lockedRowCount][colIndex - horizontalScrollPosition + lockedColumnCount];
		if (!bCtrlKey)
		{
			selectItem(listItem, bShiftKey, bCtrlKey);
			bSelChanged = true;
		}
		if (bCtrlKey)
		{
			//tkr - is this correct to assign this to our UID?
			uid = itemToUID(listItem.data);
			drawItem(visibleData[uid], selectedData[uid] != null, false, true);
		}

		if (bSelChanged)
        {
			var evt:ListEvent = new ListEvent(ListEvent.CHANGE);
			evt.itemRenderer = listItem;
			evt.rowIndex = rowIndex;
			evt.columnIndex = colIndex;
			dispatchEvent(evt);
        }
	}

	/**
	 *  @private
	 */
	override public function itemRendererToIndex(item:IListItemRenderer):int
	{
		var	uid:String = itemToUID(item.data);
		
		var n:int = listItems.length;
		for (var i:int = 0; i < listItems.length; i++)
		{
			var m:int = listItems[i].length;
			for (var j:int = 0; j < m; j++)
			{
				if (listItems[i][j] && rowMap[listItems[i][j].name].uid == uid)
				{
					if (direction == TileBaseDirection.VERTICAL)
						return (j + horizontalScrollPosition) * Math.max(maxRows, rowCount) + i;
					else
						return (i + verticalScrollPosition) * Math.max(maxColumns, columnCount) + j;
				}
			}
		}

		return -1;
	}

	/**
	 *  @private
	 */
    override public function indexToItemRenderer(index:int):IListItemRenderer
    {
        // XXarielb: theoretically these lock variables mean something
        // right now they're always 0, so we're not quite sure what to
        // do with them.  come back an revisit
        var	rowIndex:int = indexToRow(index);
        
		if (rowIndex < verticalScrollPosition + lockedRowCount ||
            rowIndex >= verticalScrollPosition + lockedRowCount + rowCount)
        {
            return null;
        }

        var	colIndex:int = indexToColumn(index);
        
		if (colIndex < horizontalScrollPosition + lockedColumnCount ||
            colIndex >= horizontalScrollPosition + lockedColumnCount + columnCount)
        {
            return null;
        }
        
		return listItems[rowIndex - verticalScrollPosition + lockedRowCount]
						[colIndex - horizontalScrollPosition + lockedColumnCount];
    }

    /**
     *  @private
     */
    override public function calculateDropIndex(event:DragEvent = null):int
    {
		if (event)
		{
			var item:IListItemRenderer;
			var pt:Point = new Point(event.localX, event.localY);
			pt = DisplayObject(event.target).localToGlobal(pt);
			pt = listContent.globalToLocal(pt);

			var rc:int = listItems.length;
			for (var i:int = 0; i < rc; i++)
			{
				if (rowInfo[i].y < pt.y && pt.y < rowInfo[i].y + rowInfo[i].height)
				{
					var cc:int = listItems[i].length;
					for (var j:int = 0; j < cc; j++)
					{
						if (listItems[i][j] && listItems[i][j].x < pt.x
							&& pt.x < listItems[i][j].x + listItems[i][j].width)
						{
							item = listItems[i][j];
    						if (!DisplayObject(item).visible)
        						item = null;
            				break;
						}
					}
					break;
				}
			}

			if (item)
				lastDropIndex = itemRendererToIndex(item);
			else
				lastDropIndex = collection ? collection.length : 0;
		}

        return lastDropIndex;
    }

	/**
	 *  @private
	 */
    override public function showDropFeedback(event:DragEvent):void
    {
        if (!dropIndicator)
        {
            var dropIndicatorClass:Class = getStyle("dropIndicatorSkin");
            if (!dropIndicatorClass)
                dropIndicatorClass = ListDropIndicator;
            dropIndicator = IFlexDisplayObject(new dropIndicatorClass());

            var vm:EdgeMetrics = viewMetrics;

            drawFocus(true);

            dropIndicator.x = 2;
			if (direction == TileBaseDirection.HORIZONTAL)
			{
				dropIndicator.setActualSize(rowHeight - 4, 4);
				DisplayObject(dropIndicator).rotation = 90;
			}
			else
				dropIndicator.setActualSize(columnWidth - 4, 4);
            dropIndicator.visible = true;
            listContent.addChild(DisplayObject(dropIndicator));

            if (collection)
                dragScroll();
        }

        var dropIndex:int = calculateDropIndex(event);
		var rowNum:int = indexToRow(dropIndex);
		var colNum:int = indexToColumn(dropIndex);

        if (rowNum >= lockedRowCount)
         	rowNum -= verticalScrollPosition;

        if (colNum >= lockedColumnCount)
         	colNum -= horizontalScrollPosition;

		var rc:Number = listItems.length;
        if (rowNum >= rc)
            rowNum = rc - 1;

		var cc:Number = rc ? listItems[0].length : 0;
        if (colNum > cc)
            colNum = cc;

        dropIndicator.x = cc && listItems[rowNum].length && listItems[rowNum][colNum] ? listItems[rowNum][colNum].x : colNum * columnWidth;
        dropIndicator.y = rc && listItems[rowNum].length && listItems[rowNum][0] ? listItems[rowNum][0].y : rowNum * rowHeight;
    }

	/**
	 *  @private
	 */
	override public function measureWidthOfItems(index:int = -1,
											count:int = 0):Number
	{
		var item:IListItemRenderer;
		var w:Number;
		var rowData:ListData;
		var needSize:Boolean = false;

		if (collection && collection.length)
		{
			var data:Object = iterator.current;
			
			item = IListItemRenderer(listContent.getChildByName("hiddenItem"));
			
			if (!item)
			{
				item = itemRenderer.newInstance();
				item.owner = this;
				item.name = "hiddenItem";
				item.visible = false;
				item.styleName = listContent;
				listContent.addChild(DisplayObject(item));
				needSize = true;
			}
			
			rowData = ListData(makeListData(data, uid, 0, 0));
			
			if (item is IDropInListItemRenderer)
				IDropInListItemRenderer(item).listData = data ? rowData : null;
			
			item.data = data;
			
			UIComponentGlobals.layoutManager.validateClient(item, true);
			
			w = item.getExplicitOrMeasuredWidth();
			if (needSize)
			{
				item.setActualSize(w, item.getExplicitOrMeasuredHeight());
				needSize = false;
			}
		}
		
		if (isNaN(w) || w == 0)
			w = 50;
		
		return w * count;
	}

    /**
     *  @private
     */
    mx_internal function getMeasuringRenderer():IListItemRenderer
    {
		var item:IListItemRenderer =
			IListItemRenderer(listContent.getChildByName("hiddenItem"));
		if (!item)
		{
			item = itemRenderer.newInstance();
			item.owner = this;
			item.name = "hiddenItem";
			item.visible = false;
			item.styleName = listContent;
			listContent.addChild(DisplayObject(item));
		}
		
		return item;
    }

    /**
     *  @private
     */
    mx_internal function setupRendererFromData(item:IListItemRenderer, data:Object):void
    {
		var rowData:ListData = ListData(makeListData(data, itemToUID(data), 0, 0));
		
		if (item is IDropInListItemRenderer)
			IDropInListItemRenderer(item).listData = data ? rowData : null;
		
		item.data = data;
		
		UIComponentGlobals.layoutManager.validateClient(item, true);
    }

	/**
	 *  @private
	 */
	override public function measureHeightOfItems(index:int = -1,
											 count:int = 0):Number
	{
		var h:Number;
		var needSize:Boolean = false;

		if (collection && collection.length)
		{
			var data:Object = iterator.current;
			
		    var item:IListItemRenderer =
			    IListItemRenderer(listContent.getChildByName("hiddenItem"));

			if (item == null)
			{
    			item = getMeasuringRenderer();
    			needSize = true;
	        }
			
			setupRendererFromData(item, data);
			
			h = item.getExplicitOrMeasuredHeight();
			if (needSize)
			{
				item.setActualSize(item.getExplicitOrMeasuredWidth(), h);
				needSize = false;
			}
		}
		
		if (isNaN(h) || h == 0)
			h = 50;

		var paddingTop:Number = getStyle("paddingTop");
		var paddingBottom:Number = getStyle("paddingBottom");

		h += paddingTop + paddingBottom;
		
		return h * count;
	}

	/**
	 *  @private
	 */
	override protected function scrollPositionToIndex(horizontalScrollPosition:int, verticalScrollPosition:int):int
	{
		if (iterator)
		{
			var startIndex:int;

			if (direction == TileBaseDirection.HORIZONTAL)
				startIndex = verticalScrollPosition * columnCount + horizontalScrollPosition;
			else
				startIndex = horizontalScrollPosition * rowCount + verticalScrollPosition;

			return startIndex;
		}
		return -1;
	}

	/**
	 *  @private
	 */
	override protected function get dragImageOffsets():Point
	{
		var pt:Point = new Point(8192, 8192);
		var found:Boolean = false;
		var rC:int = listItems.length;
		
		for (var s:String in visibleData)
		{
			if (selectedData[s])
			{
				pt.x = Math.min(pt.x, visibleData[s].x);
				pt.y = Math.min(pt.y, visibleData[s].y);
				found = true;
			}
		}
		
		if (found)
			return pt;
		
		return new Point(0, 0);
	}

	/**
	 *  @private
	 *
	 *  see ListBase.as
	 */
	override mx_internal function addClipMask(layoutChanged:Boolean):void
	{
	}
	
	/**
	 *  @private
	 *
	 *  Undo the effects of the addClipMask function (above)
	 */
	override mx_internal function removeClipMask():void
	{
	}

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
     *  Creates a new ListData instance and populates the fields based on
     *  the input data provider item. 
     *  
     *  @param data The data provider item used to populate the ListData.
     *  @param uid The UID for the item.
     *  @param rowNum The index of the item in the data provider.
     *  @param columnNum The columnIndex associated with this item. 
     *  
     *  @return A newly constructed ListData object.
     */
	protected function makeListData(data:Object, uid:String, 
		rowNum:int, columnNum:int):BaseListData
	{
		return new ListData(itemToLabel(data), itemToIcon(data), labelField, uid, 
			this, rowNum, columnNum);
	}

	/**
	 *  @private
	 *  Assumes horizontal.
	 */
	private function lastRowInColumn(index:int):int
	{
		var numCols:int = maxColumns > 0 ? maxColumns : columnCount;
		var numRows:int = Math.floor((collection.length - 1) / numCols);
		if (index * numRows > collection.length)
			numRows--;
		return numRows;
	}

	/**
	 *  @private
	 *  Assumes vertical.
	 */
	private function lastColumnInRow(index:int):int
	{
		var numRows:int = maxRows > 0 ? maxRows : rowCount;
		var numCols:int = Math.floor((collection.length - 1) / numRows);
		if (indicesToIndex(index, numCols) >= collection.length)
			numCols--;
		return numCols;
	}

	/**
	 *  @private
	 */
	override protected function indexToRow(index:int):int
	{
		if (direction == TileBaseDirection.VERTICAL)
		{
			var numRows:int = maxRows > 0 ? maxRows : rowCount;
			return index % numRows;
		}

		var numCols:int = maxColumns > 0 ? maxColumns : columnCount;
		return Math.floor(index / numCols);
	}

	/**
	 *  @private
	 */
	override protected function indexToColumn(index:int):int
	{
		if (direction == TileBaseDirection.VERTICAL)
		{
			var numRows:int = maxRows > 0 ? maxRows : rowCount;
			return Math.floor(index / numRows);
		}

		var numCols:int = maxColumns > 0 ? maxColumns : columnCount;
		return index % numCols;
	}

	/**
	 *  @private
	 */
	override public function indicesToIndex(rowIndex:int, columnIndex:int):int
	{
		if (direction == TileBaseDirection.VERTICAL)
		{
			var numRows:int = maxRows > 0 ? maxRows : rowCount;
			return columnIndex * numRows + rowIndex;
		}

		var numCols:int = maxColumns > 0 ? maxColumns : columnCount;
		return rowIndex * numCols + columnIndex;
	}

	//--------------------------------------------------------------------------
	//
	//  Overridden event handlers
	//
	//--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function collectionChangeHandler(event:Event):void
    {
		if (event is CollectionEvent)
		{
			var ce:CollectionEvent = CollectionEvent(event);

			if (ce.location == 0 || ce.kind == CollectionEventKind.REFRESH)
			{
				itemsNeedMeasurement = true;
				invalidateProperties();
			}

			if (ce.kind == CollectionEventKind.REMOVE)
			{
				var oldIndex:int = indicesToIndex(verticalScrollPosition, horizontalScrollPosition);
				if (ce.location < oldIndex)
				{
					oldIndex -= ce.items.length;
					super.collectionChangeHandler(event);
					super.verticalScrollPosition = indexToRow(oldIndex);
					super.horizontalScrollPosition = indexToColumn(oldIndex);
					return;
				}
			}
		}

		super.collectionChangeHandler(event);
    }

    /**
     *  @private
     */
    override protected function commitProperties():void
    {
		super.commitProperties();

		if (itemsNeedMeasurement)
		{
			itemsNeedMeasurement = false;
			if (isNaN(explicitRowHeight))
				setRowHeight(measureHeightOfItems(0, 1));
			if (isNaN(explicitColumnWidth))
				setColumnWidth(measureWidthOfItems(0, 1));
		}
	}

    /**
     *  @private
     */
	override protected function updateDisplayList(unscaledWidth:Number,
												  unscaledHeight:Number):void
    {
		// setup the tile sizes before calling the base class
		if (explicitColumnCount > 0 && isNaN(explicitColumnWidth))
		{
			// enforce that we can see the right number of columns
			// even if we squeeze the columns
			setColumnWidth(Math.floor((width - viewMetrics.left - viewMetrics.right) / explicitColumnCount));

		}
		if (explicitRowCount > 0 && isNaN(explicitRowHeight))
		{
			// enforce that we can see the right number of columns
			// even if we squeeze the columns
			setRowHeight(Math.floor((height - viewMetrics.top - viewMetrics.bottom) / explicitRowCount));

		}

		super.updateDisplayList(unscaledWidth, unscaledHeight);

		drawTileBackgrounds();
	}

	/**
	 *  Draws the backgrounds, if any, behind all of the tiles.
	 *  This implementation makes a Sprite names "tileBGs" if
	 *  it doesn't exist, adds it to the back
	 *  of the z-order in the <code>listContent</code>, and
	 *  calls <code>drawTileBackground()</code> for each visible
	 *  tile.
	 */
	protected function drawTileBackgrounds():void
	{
        var tileBGs:Sprite = Sprite(listContent.getChildByName("tileBGs"));
		if (!tileBGs)
		{
			tileBGs = new FlexSprite();
			tileBGs.mouseEnabled = false;
			tileBGs.name = "tileBGs";
			listContent.addChildAt(tileBGs, 0)
		}

		var colors:Array;

		colors = getStyle("alternatingItemColors");

		if (!colors)
			return;

		StyleManager.getColorNames(colors);

		var curItem:int = 0;
		for (var i:int = 0; i < rowCount; i++)
		{
			for (var j:int = 0; j < columnCount; j++)
			{
				// Height is usually as tall is the items in the row,
				// but not if it would extend below the bottom of listContent.
				var height:Number = (i < rowCount - 1) ? rowHeight :
								Math.min(rowHeight,
										 listContent.height - ((rowCount - 1) * rowHeight));

				var width:Number = (j < columnCount - 1) ? columnWidth : 
								Math.min(columnWidth,
										 listContent.width - ((columnCount - 1) * columnWidth));
				var bg:DisplayObject = drawTileBackground(tileBGs, i, j, width, height, colors[(i * columnCount + j) % colors.length], listItems[i][j]);
				bg.y = i * rowHeight;
				bg.x = j * columnWidth;

			}
		}

		var n:int = rowCount * columnCount;
		while (tileBGs.numChildren > n)
		{
			tileBGs.removeChildAt(tileBGs.numChildren - 1);
		}
	}

	/**
	 *  Draws the background for an individual tile. 
	 *  Takes a Sprite object, applies the background dimensions
	 *  and color, and returns the sprite with the values applied.
	 *  
	 *  @param s The Sprite that contains the individual tile backgrounds.
	 *  @param rowIndex The index of the row that contains the tile.
	 *  @param columnIndex The index of the column that contains the tile.
	 *  @param width The width of the background.
	 *  @param height The height of the background.
	 *  @param color The fill color for the background.
	 *  @param item The item renderer for the tile.
	 * 
	 *  @return The background Sprite.
	 * 
	 */
	protected function drawTileBackground(s:Sprite, rowIndex:int, columnIndex:int, width:Number, height:Number,  
																	color:uint, item:IListItemRenderer):DisplayObject
	{
		// trace("drawTileBackground " + rowIndex + " " + col);

		var tileBGIndex:int = rowIndex * columnCount + columnIndex;

		var bg:Shape;
		if (tileBGIndex < s.numChildren)
		{
			bg = Shape(s.getChildAt(tileBGIndex));
		}
		else
		{
			bg = new FlexShape();
			bg.name = "tileBackground";
			s.addChild(bg);
		}

		var g:Graphics = bg.graphics;
		g.clear();
		g.beginFill(color, getStyle("backgroundAlpha"));
		g.drawRect(0, 0, width, height);
		g.endFill();

		return bg;
	}

	/**
	 *  @private
	 */
	override protected function keyDownHandler(event:KeyboardEvent):void
	{
		var selectedListItem:IListItemRenderer;

		if (!iteratorValid) return;

		if (!collection) return;

		switch (event.keyCode)
		{
			case Keyboard.UP:
			case Keyboard.DOWN:
			{
                moveSelectionVertically(event.keyCode,
                                        event.shiftKey, 
                                        event.ctrlKey);
				event.stopPropagation();
				break;
			}

			case Keyboard.LEFT:
			case Keyboard.RIGHT:
			{
                moveSelectionHorizontally(event.keyCode, 
                                          event.shiftKey, 
                                          event.ctrlKey);
				event.stopPropagation();
				break;
			}

			case Keyboard.END:
			case Keyboard.HOME:
			case Keyboard.PAGE_UP:
			case Keyboard.PAGE_DOWN:
			{
				if (direction == TileBaseDirection.VERTICAL)
				{
					moveSelectionHorizontally(event.keyCode, 
                                              event.shiftKey, 
                                              event.ctrlKey);
				}
				else
				{
					moveSelectionVertically(event.keyCode, 
                                            event.shiftKey, 
                                            event.ctrlKey);
				}
				event.stopPropagation();
				break;
			}

			case Keyboard.SPACE:
			{
				if (caretIndex < 0)
					break;
				var	rowIndex:int = indexToRow(caretIndex);
				var	colIndex:int = indexToColumn(caretIndex);
				selectedListItem = listItems
					[rowIndex - verticalScrollPosition + lockedRowCount]
					[colIndex - horizontalScrollPosition + lockedColumnCount];
				selectItem(selectedListItem, event.shiftKey, event.ctrlKey);
				break;
			}

			default:
			{
				if (findKey(event.keyCode))
					event.stopPropagation();
			}
		}
	}

	/**
	 *  @private
	 */
	override protected function scrollHandler(event:Event):void
	{
		// TextField.scroll bubbles so you might see it here
		if (event is ScrollEvent)
		{
			if (!liveScrolling &&
				ScrollEvent(event).detail == ScrollEventDetail.THUMB_TRACK)
			{
				return;
			}

			var scrollBar:ScrollBar = ScrollBar(event.target);
			var pos:Number = scrollBar.scrollPosition;
			var delta:int;
			var startIndex:int;
			var o:EdgeMetrics;
			var bookmark:CursorBookmark;

			if (scrollBar == verticalScrollBar)
			{
				delta = pos - verticalScrollPosition;
				
				super.scrollHandler(event);
				
				if (Math.abs(delta) >= listItems.length - lockedRowCount)
				{
					startIndex = indicesToIndex(pos, horizontalScrollPosition);
					try
					{
						iterator.seek(CursorBookmark.FIRST, startIndex);
					}
					catch (e:ItemPendingError)
					{
						lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, startIndex);
						e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
						// trace("IPE in UpdateDisplayList");
						iteratorValid = false;
						// don't do anything, we'll repaint when the data arrives
					}
					bookmark = iterator.bookmark;
					 //if we scrolled more than the number of scrollable rows
					clearIndicators();
					visibleData = {};
					makeRowsAndColumns(0, 0, listContent.width, listContent.height, 0, 0);
					iterator.seek(bookmark, 0);
					drawRowBackgrounds();
				}
				else if (delta != 0)
				{
					scrollVertically(pos, Math.abs(delta), delta > 0);
				}
			}
			else
			{
				delta = pos - horizontalScrollPosition;
				
				super.scrollHandler(event);
				
				if (Math.abs(delta) >= listItems[0].length - lockedColumnCount)
				{
					startIndex = indicesToIndex(verticalScrollPosition, pos);
					try
					{
						iterator.seek(CursorBookmark.FIRST, startIndex);
					}
					catch (e:ItemPendingError)
					{
						lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, startIndex);
						e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
												lastSeekPending));
						// trace("IPE in UpdateDisplayList");
						iteratorValid = false;
						// don't do anything, we'll repaint when the data arrives
					}
					bookmark = iterator.bookmark;
					 //if we scrolled more than the number of scrollable rows
					clearIndicators();
					visibleData = {};
					makeRowsAndColumns(0, 0, listContent.width, listContent.height, 0, 0);
					iterator.seek(bookmark, 0);
					drawRowBackgrounds();
				}
				else if (delta != 0)
				{
					scrollHorizontally(pos, Math.abs(delta), delta > 0);
				}
			}
		}
	}

    /**
     *  @private 
     */
	override public function scrollToIndex(index:int):Boolean
	{
		var newVPos:int;
		var newHPos:int;

		if (direction == TileBaseDirection.HORIZONTAL)
			if (index < lockedRowCount * columnCount)
				return false;

		if (direction == TileBaseDirection.VERTICAL)
			if (index < lockedColumnCount * rowCount)
				return false;

		var firstIndex:int = scrollPositionToIndex(horizontalScrollPosition, verticalScrollPosition);
		var numItemsVisible:int = (listItems.length - lockedRowCount) * (listItems[0].length - lockedColumnCount);
		if (index >= firstIndex + numItemsVisible || index < firstIndex)
		{
			newVPos = Math.min(indexToRow(index), maxVerticalScrollPosition);
			newHPos = Math.min(indexToColumn(index), maxHorizontalScrollPosition);
		
			try
			{
				iterator.seek(CursorBookmark.FIRST, scrollPositionToIndex(horizontalScrollPosition, verticalScrollPosition));
				super.horizontalScrollPosition = newHPos;
				super.verticalScrollPosition = newVPos;
			}
			catch (e:ItemPendingError)
			{
			}
			return true;
		}
		return false;
	}
}

}
