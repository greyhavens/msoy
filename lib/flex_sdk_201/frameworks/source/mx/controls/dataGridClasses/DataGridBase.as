////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.dataGridClasses
{

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.ui.Keyboard;
import flash.utils.Dictionary;
import mx.collections.CursorBookmark;
import mx.collections.ItemResponder;
import mx.collections.errors.ItemPendingError;
import mx.controls.listClasses.BaseListData;
import mx.controls.listClasses.IDropInListItemRenderer;
import mx.controls.listClasses.IListItemRenderer;
import mx.controls.listClasses.ListBase;
import mx.controls.listClasses.ListBaseSeekPending;
import mx.controls.listClasses.ListRowInfo;
import mx.core.IInvalidating;
import mx.core.UIComponentGlobals;
import mx.core.mx_internal;
import mx.events.ListEvent;
import mx.events.ScrollEvent;
import mx.events.ScrollEventDetail;
import mx.events.ScrollEventDirection;

use namespace mx_internal;

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="maxHorizontalScrollPosition", kind="property")]
[Exclude(name="maxVerticalScrollPosition", kind="property")]

/**
 *  The DataGridBase class is the base class for controls
 *  that display lists of items in multiple columns.
 *  It is not used directly in applications.
 *  
 *  @mxml
 *  
 *  <p>The DataGridBase class inherits all the properties of its parent classes
 *  and adds the following properties:</p>
 *  
 *  <pre>
 *  &lt;mx:<i>tagname</i>
 *    headerHeight="depends on styles and header renderer"
 *    showHeaders="true|false"
 *  /&gt;
 *  </pre>
 */
public class DataGridBase extends ListBase
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
	public function DataGridBase()
	{
		super();

		listType = "vertical";
		
		lockedRowCount = 1;
		defaultRowCount = 7;	// default number of rows is 7
		columnMap = {};
		freeItemRenderersTable = new Dictionary(false);
	}

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

    /**
	 *  A map of item renderes to columns.
	 *  Like <code>ListBase.rowMap</code>, this property contains 
	 *  a hash map of item renderers and the columns they belong to.
	 *  Item renderers are indexed by their DisplayObject name
	 *
	 *  @see mx.controls.listClasses.ListBase#rowMap
	 */
	protected var columnMap:Object;

    /**
	 *  A per-column table of unused item renderers. 
	 *  Most list classes recycle item renderers that they have already created 
	 *  as they scroll off screen. 
	 *  The recycled renderers are stored here.
	 *  The table is a Dictionary where the entries are Arrays indexed
	 *  by the actual DataGridColumn (not the column's dataField or other
	 *  properties), and each array is a stack of currently unused renderers
	 */
	protected var freeItemRenderersTable:Dictionary;

    /**
	 *  The set of visible columns.
	 */
	mx_internal var visibleColumns:Array;

    /**
	 *  Flag specifying that the set of visible columns and/or their sizes needs to
	 *  be recomputed.
	 */
	mx_internal var columnsInvalid:Boolean = true;

	// these three keep track of the key selection that caused
	// the page fault
	private var bShiftKey:Boolean = false;
	private var bCtrlKey:Boolean = false;
	private var lastKey:uint = 0;

	private var bSelectItem:Boolean = false;

	//--------------------------------------------------------------------------
	//
	//  Overridden properties
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
	private var lockedRowCountResetShowHeaders:Boolean = false;

    //----------------------------------
    //  lockedRowCount
    //----------------------------------

    /**
	 *  @private
	 */
    override public function get lockedRowCount():int
    {
		var lrc:int = super.lockedRowCount;

		if (showHeaders && headerHeight == 0)
			lrc--;

		return lrc;
    }

    /**
	 *  @private
	 */
    override public function set lockedRowCount(value:int):void
    {
        if (showHeaders && value == 0)
        {
        	showHeaders = false;
        	lockedRowCountResetShowHeaders = true;
        }
        
        if (!showHeaders && value > 0 && lockedRowCountResetShowHeaders)
        {
        	showHeaders = true;
        	lockedRowCountResetShowHeaders = false;
        }
        
		super.lockedRowCount = value;
    }

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

    //----------------------------------
    //  headerHeight
    //----------------------------------

	/**
	 *  @private
	 *  Storage for the headerHeight property.
	 */
	mx_internal var _headerHeight:Number = 22;
	
	/**
	 *  @private
	 */
	mx_internal var _explicitHeaderHeight:Boolean;

    [Bindable("resize")]
    [Inspectable(category="General", defaultValue="22")]

	/**
	 *  The height of the header cell of the column, in pixels.
	 *  If set explicitly, that height will be used for all of
	 *  the headers.  If not set explicitly, 
	 *  the height will based on style settings and the header
	 *  renderer.  
	 */
    public function get headerHeight():Number
    {
        return _headerHeight;
    }

	/**
	 *  @private
	 */
	public function set headerHeight(value:Number):void
    {
        _headerHeight = value;
		_explicitHeaderHeight = true;
		itemsSizeChanged = true;
        
		invalidateDisplayList();
    }

    //----------------------------------
    //  showHeaders
    //----------------------------------

	/**
	 *  @private
	 *  Storage for the showHeaders property.
	 */
	private var _showHeaders:Boolean = true;

    [Bindable("showHeadersChanged")]
    [Inspectable(category="General", defaultValue="true")]

	/**
	 *  A flag that indicates whether the control should show
	 *  column headers.
	 *  If <code>true</code>, the control shows column headers. 
	 *
	 *  @default true
	 */
    public function get showHeaders():Boolean
    {
        return _showHeaders;
    }

    /**
	 *  @private
	 */
	public function set showHeaders(value:Boolean):void
    {
		// If showing then lockedRowCount increases by 1.
        if (value != _showHeaders)
        {
			_showHeaders = value;
			_lockedRowCount += value ? 1 : -1;
		}
		itemsSizeChanged = true;
        
		invalidateDisplayList();

        dispatchEvent(new Event("showHeadersChanged"));
    }

    /**
	 *  @private
	 *  headers are not renderered if showHeaders = false
	 *  or headerheight = 0, so this test is whether row0 is
	 *  a header or not.
	 */
	mx_internal function get headerVisible():Boolean
	{
		return showHeaders && (headerHeight > 0);
	}

	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
	override protected function makeRowsAndColumns(left:Number, top:Number,
												right:Number, bottom:Number,
											    firstCol:int, firstRow:int,
												byCount:Boolean = false, rowsNeeded:uint = 0):Point
	{
		// trace(this, "makeRowsAndColumns " + left + " " + top + " " + right + " " + bottom + " " + firstCol + " " + firstRow);
		
		var xx:Number;
		var yy:Number;
		var ww:Number;
		var hh:Number;
		var rh:Number;

		var bSelected:Boolean = false;
		var bHighlight:Boolean = false;
		var bCaret:Boolean = false;
		var rowData:DataGridListData;

		var i:int;
		var j:int;

		var colNum:int = lockedColumnCount;
		var rowNum:int = lockedRowCount;
		var rowsMade:int = 0;

		var item:IListItemRenderer;
		var extraItem:IListItemRenderer;

		// bail if we have no columns
		if (!visibleColumns || visibleColumns.length == 0)
		{
			while (listItems.length)
			{
				rowNum = listItems.length - 1;
				while (listItems[rowNum].length)
				{
					// remove extra columns
					item = listItems[rowNum].pop();
					addToFreeItemRenderers(item);
				}
				listItems.pop();
			}
			visibleData = {};
			return new Point(0,0);
		}

		invalidateSizeFlag = true;

		var data:Object;
		var uid:String;
		var c:DataGridColumn;
		var more:Boolean = true;
		var valid:Boolean = true;

			yy = top;
			// if we have headers or other locked items and either haven't created the
			// items for those items or we're redrawing the items above the locked region...
			var numLocked:int = lockedRowCount;
			if (lockedRowCount > 0 && (!listItems[lockedRowCount - 1] || !listItems[lockedRowCount - 1][0] || (top < listItems[lockedRowCount - 1][0].y + listItems[lockedRowCount - 1][0].height)))
			{
				rowNum = 0;
				var maxHeaderHeight:Number = 0;
				if (headerVisible)
				{
					xx = left;
					hh = 0;
					colNum = 0;	// visible columns compensate for firstCol offset
					while (xx < right && colNum < visibleColumns.length)
					{
						c = visibleColumns[colNum];
						item = columnItemRenderer(c, true);
						rowData = DataGridListData(makeListData(c, uid, rowNum, c.colNum, c));
						rowMap[item.name] = rowData;
						if (item is IDropInListItemRenderer)
							IDropInListItemRenderer(item).listData = rowData;
						item.data = c;
						item.styleName = c;
						listContent.addChild(DisplayObject(item));
						if (!listItems[rowNum])
							listItems[rowNum] = [];
						var oldHeader:DisplayObject = listItems[rowNum][colNum];
						if (oldHeader)
						{
							delete rowMap[oldHeader.name];
							listContent.removeChild(oldHeader);
						}
						listItems[rowNum][colNum] = item;
						// set prefW so we can compute prefH
						item.explicitWidth = ww = c.width;
						UIComponentGlobals.layoutManager.validateClient(item, true);
						// but size it regardless of what prefW is
						rh = item.getExplicitOrMeasuredHeight();
						item.setActualSize(ww, _explicitHeaderHeight ?
							_headerHeight - cachedPaddingTop - cachedPaddingBottom : rh);
						item.move(xx, yy + cachedPaddingTop);
						xx += ww;
						colNum++;
						hh = Math.ceil(Math.max(hh, _explicitHeaderHeight ?
							_headerHeight : rh + cachedPaddingBottom + cachedPaddingTop));
						maxHeaderHeight = Math.max(maxHeaderHeight, _explicitHeaderHeight ?
							_headerHeight - cachedPaddingTop - cachedPaddingBottom : rh);
					}
					if (listItems[rowNum])
					{
						// expand all headers to be of maximum height
						for (i = 0; i < listItems[rowNum].length; i++)
							listItems[rowNum][i].setActualSize(listItems[rowNum][i].width, maxHeaderHeight);

						while (listItems[rowNum].length > colNum)
						{
							// remove extra columns
							extraItem = listItems[rowNum].pop();
							delete rowMap[DisplayObject(extraItem).name];
							listContent.removeChild(DisplayObject(extraItem));
						}
					}
					rowInfo[rowNum] = new ListRowInfo(yy, hh, uid);
					yy += item ? hh : 0;
					if (!_explicitHeaderHeight)
						_headerHeight = item ? hh : 0;
					numLocked--;
					rowNum++;
					rowsMade++;
				}
				var bookmark:CursorBookmark;
				if (numLocked && iterator)
				{
					bookmark = iterator.bookmark;
					try 
					{
						iterator.seek(CursorBookmark.FIRST);
					}
					catch (e:ItemPendingError)
					{
						lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, 0);
						e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
														lastSeekPending));
						iteratorValid = false;
					}

				}
				more = (iterator != null && !iterator.afterLast && iteratorValid);
				for (i = 0; i < numLocked; i++)
				{
					valid = more;
					data = more ? iterator.current : null;
					if (iterator && more)
					{
						try 
						{
							more = iterator.moveNext();
						}
						catch (e:ItemPendingError)
						{
							lastSeekPending = new ListBaseSeekPending(CursorBookmark.CURRENT, 0);
							e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
														lastSeekPending));
							more = false;
							iteratorValid = false;
						}
					}

					xx = left;
					hh = 0;
					colNum = 0;
					uid = null;
					
					if (!listItems[rowNum])
						listItems[rowNum] = [];

					if (valid)
					{
						while (xx < right && colNum < visibleColumns.length)
						{
							c = visibleColumns[colNum];						
							item = listItems[rowNum][colNum];
							uid = itemToUID(data);					
							if (!item || itemToUID(item.data) != uid
								|| columnMap[item.name] != c)
							{
								if (freeItemRenderersTable[c] && freeItemRenderersTable[c].length)
								{
									item = freeItemRenderersTable[c].pop();
									// trace("reused " + item);
								}
								else
								{
									item = columnItemRenderer(c, false);
									item.styleName = c;
									// trace("created " + item);
									listContent.addChild(DisplayObject(item));
								}
								rowData = DataGridListData(makeListData(data, uid, rowNum, c.colNum, c));
								rowMap[item.name] = rowData;
								columnMap[item.name] = c;
								if (item is IDropInListItemRenderer)
									IDropInListItemRenderer(item).listData = data ? rowData : null;
								item.data = data;
								if (listItems[rowNum][colNum])
									addToFreeItemRenderers(listItems[rowNum][colNum]);
								listItems[rowNum][colNum] = item;
							}
							item.visible = true;
							if (uid && colNum == 0)
								visibleData[uid] = item;
							item.explicitWidth = ww = getWidthOfItem(item, c);
							UIComponentGlobals.layoutManager.validateClient(item, true);
							rh = item.getExplicitOrMeasuredHeight();
							item.setActualSize(ww, variableRowHeight
									? rh
									: rowHeight - cachedPaddingTop - cachedPaddingBottom);
							item.move(xx, yy + cachedPaddingTop);
							xx += ww;
							colNum++;
							hh = Math.ceil(Math.max(hh, variableRowHeight ? rh + cachedPaddingTop + cachedPaddingBottom : rowHeight));
						}
					}
					else
					{
						// if we've run out of data, we dont make renderers
						// and we inherit the previous row's height or rowHeight
						// if it is the first row.
						hh = rowNum > 1 ? rowInfo[rowNum - 1].height : rowHeight;
					}
					while (listItems[rowNum].length > colNum)
					{
						// remove extra columns
						extraItem = listItems[rowNum].pop();
						addToFreeItemRenderers(extraItem);
					}
					if (valid && variableRowHeight)
					{
						hh = Math.ceil(calculateRowHeight(data, hh, true));
					}
					if (listItems[rowNum])
					{
						for (j = 0; j < listItems[rowNum].length; j++)
							listItems[rowNum][j].setActualSize(
								listItems[rowNum][j].width,
									hh - cachedPaddingTop - cachedPaddingBottom);
					}
					if (cachedVerticalAlign != "top")
					{
						if (cachedVerticalAlign == "bottom")
						{
							for (j = 0; j < colNum; j++)
							{
								item = listItems[rowNum][j];
								item.move(item.x, yy + hh - cachedPaddingBottom - item.getExplicitOrMeasuredHeight());
							}
						}
						else
						{
							for (j = 0; j < colNum; j++)
							{
								item = listItems[rowNum][j];
								item.move(item.x, yy + cachedPaddingTop + (hh - cachedPaddingBottom - cachedPaddingTop - item.getExplicitOrMeasuredHeight()) / 2);
							}
						}

					}
					bSelected = selectedData[uid] != null;
					bHighlight = highlightUID == uid;
					bCaret = caretUID == uid;
					rowInfo[rowNum] = new ListRowInfo(yy, hh, uid);
					if (valid && visibleData[uid])
						drawItem(visibleData[uid], bSelected, bHighlight, bCaret);
					yy += hh;
					rowNum++;
					rowsMade++;
				}
				if (bookmark)
				{
					try 
					{
						iterator.seek(bookmark, numLocked);
					}
					catch (e:ItemPendingError)
					{
						lastSeekPending = new ListBaseSeekPending(CursorBookmark.CURRENT, 0)
						e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
														lastSeekPending));
						iteratorValid = false;
					}
				}
			}
			else
			{
				rowNum = firstRow;
			}
			more = (iterator != null && !iterator.afterLast && iteratorValid);
			while ((!byCount && yy < bottom) || (byCount && rowsNeeded > 0))
			{
				if (byCount)
					rowsNeeded--;

				valid = more;
				data = more ? iterator.current : null;
				if (iterator && more)
				{
					try
					{
						more = iterator.moveNext();
					}
					catch (e:ItemPendingError)
					{
						lastSeekPending = new ListBaseSeekPending(CursorBookmark.CURRENT, 0)
						e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
														lastSeekPending));
						more = false;
						iteratorValid = false;
					}
				}

				xx = left;
				hh = 0;
				colNum = 0;
				uid = null;
				if (!listItems[rowNum])
					listItems[rowNum] = [];
				if (valid)
				{
					while (xx < right && colNum < visibleColumns.length)
					{
						c = visibleColumns[colNum];
						item = listItems[rowNum][colNum];
						uid = itemToUID(data);
						if (!item || itemToUID(item.data) != uid
							|| columnMap[item.name] != c)
						{
							if (freeItemRenderersTable[c] && freeItemRenderersTable[c].length)
							{
								item = freeItemRenderersTable[c].pop();
								// trace("reused " + item);
							}
							else
							{
								item = columnItemRenderer(c, false);
								item.styleName = c;
								// trace("created " + item);
								listContent.addChild(DisplayObject(item));
							}
							// a space is used if no data so text widgets get some default size
							columnMap[item.name] = c;
							if (listItems[rowNum][colNum])
								addToFreeItemRenderers(listItems[rowNum][colNum]);
							listItems[rowNum][colNum] = item;
						}

						//[Matt] moved from inside the block above to outside because
						//the item definitely exists at this point, and always needs 
						//its data refreshed
						rowData = DataGridListData(makeListData(data, uid, rowNum, c.colNum, c));
						rowMap[item.name] = rowData;
						if (item is IDropInListItemRenderer)
							IDropInListItemRenderer(item).listData = data ? rowData : null;
						item.data = data;
						item.visible = true;
						if (uid && colNum == 0)
							visibleData[uid] = item;
						item.explicitWidth = ww = getWidthOfItem(item, c);
						UIComponentGlobals.layoutManager.validateClient(item, true);
						rh = item.getExplicitOrMeasuredHeight();
						item.setActualSize(ww, variableRowHeight
								? item.getExplicitOrMeasuredHeight()
								: rowHeight - cachedPaddingTop - cachedPaddingBottom);
						item.move(xx, yy + cachedPaddingTop);
						xx += ww;
						colNum++;
						hh = Math.ceil(Math.max(hh, variableRowHeight ? rh + cachedPaddingTop + cachedPaddingBottom : rowHeight));
					}
				}
				else
				{
					hh = rowNum > 1 ? rowInfo[rowNum - 1].height : rowHeight;
				}
				while (listItems[rowNum].length > colNum)
				{
					// remove extra columns
					extraItem = listItems[rowNum].pop();
					addToFreeItemRenderers(extraItem);
				}
				if (valid && variableRowHeight)
				{
					hh = Math.ceil(calculateRowHeight(data, hh, true));
				}
				if (listItems[rowNum])
				{
					for (j = 0; j < listItems[rowNum].length; j++)
						listItems[rowNum][j].setActualSize(
							listItems[rowNum][j].width,
								hh - cachedPaddingTop - cachedPaddingBottom);
				}
				if (cachedVerticalAlign != "top")
				{
					if (cachedVerticalAlign == "bottom")
					{
						for (j = 0; j < colNum; j++)
						{
							item = listItems[rowNum][j];
							item.move(item.x, yy + hh - cachedPaddingBottom - item.getExplicitOrMeasuredHeight());
						}
					}
					else
					{
						for (j = 0; j < colNum; j++)
						{
							item = listItems[rowNum][j];
							item.move(item.x, yy + cachedPaddingTop + (hh - cachedPaddingBottom - cachedPaddingTop - item.getExplicitOrMeasuredHeight()) / 2);
						}
					}

				}
				bSelected = selectedData[uid] != null;
				bHighlight = highlightUID == uid;
				bCaret = caretUID == uid;
				rowInfo[rowNum] = new ListRowInfo(yy, hh, uid);
				// trace("rowNum = " + rowNum);
				if (valid && visibleData[uid])
					drawItem(visibleData[uid], bSelected, bHighlight, bCaret);
				if (hh == 0) // hh can be zero if we had zero width
					hh = rowHeight;
				yy += hh;
				rowNum++;
				rowsMade++;
			}
			// byCount means we're making rows and wont get all the way to the bottom
			// so we skip this cleanup pass
			if (!byCount)
			{
				// trace("MakeRowsAndColumns rowNum = " + rowNum);
				// delete extra rows
				while (rowNum < listItems.length)
				{
					var rr:Array = listItems.pop();
					rowInfo.pop();
					while (rr.length)
					{
						item = rr.pop();
						addToFreeItemRenderers(item);
					}
				}
			}
			// trace("MakeRowsAndColumns listItems.length = " + listItems.length);

		invalidateSizeFlag = false;

		return new Point(colNum, rowsMade);
	}

    /**
	 *  @private
	 */
	override protected function drawItem(item:IListItemRenderer,
									  selected:Boolean = false,
									  highlighted:Boolean = false,
									  caret:Boolean = false,
									  transition:Boolean = false):void
	{
		if (!item)
			return;

		super.drawItem(item, selected, highlighted, caret, transition);
		
		var rowIndex:int = rowMap[item.name].rowIndex;
		for (var i:int = 0; i < visibleColumns.length; i++)
		{
			var r:IListItemRenderer = listItems[rowIndex][i];
			if (r is IInvalidating)
			{
				var ui:IInvalidating = IInvalidating(r);
				ui.invalidateDisplayList();
				ui.validateNow();
			}
		}
	}

    /**
     *  @private
	 *  Everywhere rowCount or lockedRowCount is used, subtract headerShift.
	 *  Also add headerShift in index while getting from listItems.
	 *
     *  @return The newly selected item or <code>null</code> if the selection
     *  has not changed.
     */
    override protected function moveSelectionVertically(
									code:uint, shiftKey:Boolean,
									ctrlKey:Boolean):void
    {
        var newVerticalScrollPosition:Number;
		var listItem:IListItemRenderer;
        var uid:String;
        var len:int;

        showCaret = true;

        var rowCount:int = listItems.length;

		var partialRow:int = 0;
        if (rowInfo[rowCount - 1].y +
			rowInfo[rowCount - 1].height > listContent.height)
		{
            partialRow++;
		}

		var headerShift:int = headerVisible ? 1 : 0;

		var bUpdateVerticalScrollPosition:Boolean = false;
		bSelectItem = false;

        switch (code)
        {
			case Keyboard.UP:
			{
				if (caretIndex > 0)
				{
					caretIndex--;
					bUpdateVerticalScrollPosition = true;
					bSelectItem = true;
				}
				break;
			}

			case Keyboard.DOWN:
			{
				if (caretIndex < collection.length - 1)
				{
					caretIndex++;
					bUpdateVerticalScrollPosition = true;
					bSelectItem = true;
				}
				else if ((caretIndex == collection.length - 1) && partialRow)
				{
					if (verticalScrollPosition < maxVerticalScrollPosition)
						newVerticalScrollPosition = verticalScrollPosition + 1;
				}
				break;
			}

			case Keyboard.PAGE_UP:
			{
				if (caretIndex < lockedRowCount - headerShift)
				{
					newVerticalScrollPosition = 0;
					caretIndex = 0;
				}
				// if the caret is on-screen, but not at the top row
				// just move the caret to the top row
				else if (caretIndex > verticalScrollPosition + lockedRowCount - headerShift &&
					caretIndex < verticalScrollPosition + rowCount - headerShift)
				{
					caretIndex = verticalScrollPosition + lockedRowCount - headerShift;
				}
				else
				{
					// paging up is really hard because we don't know how many
					// rows to move because of variable row height.  We would have
					// to double-buffer a previous screen in order to get this exact
					// so we just guess for now based on current rowCount
					caretIndex = Math.max(caretIndex - rowCount + lockedRowCount, 0);
					newVerticalScrollPosition = Math.max(caretIndex - lockedRowCount + headerShift,0)
				}
				bSelectItem = true;

				break;
			}

			case Keyboard.PAGE_DOWN:
			{
				if (caretIndex < lockedRowCount - headerShift)
				{
					newVerticalScrollPosition = 0;
				}
				// if the caret is on-screen, but not at the bottom row
				// just move the caret to the bottom row (not partial row)
				else if (caretIndex >= verticalScrollPosition + lockedRowCount - headerShift &&
					caretIndex < verticalScrollPosition + rowCount - headerShift - partialRow - 1)
				{
				}
				else if (lockedRowCount >= rowCount - partialRow - 1)
				{
					newVerticalScrollPosition = Math.min(verticalScrollPosition + 1, maxVerticalScrollPosition);
				}
				else
				{
					newVerticalScrollPosition = Math.min(caretIndex - lockedRowCount + headerShift, maxVerticalScrollPosition);
				}
				bSelectItem = true;
				break;
			}

			case Keyboard.HOME:
			{
				if (caretIndex > 0)
				{
					caretIndex = 0;
					newVerticalScrollPosition = 0;
					bSelectItem = true;
				}
				break;
			}

			case Keyboard.END:
			{
				if (caretIndex < collection.length - 1)
				{
					caretIndex = collection.length - 1;
					newVerticalScrollPosition = maxVerticalScrollPosition;
					bSelectItem = true;
				}
				break;
			}
		}

		if (bUpdateVerticalScrollPosition)
		{
            if (caretIndex < lockedRowCount - headerShift)
			{
            	newVerticalScrollPosition = 0;
            }
			else if (caretIndex < verticalScrollPosition + lockedRowCount - headerShift)
			{
                newVerticalScrollPosition = caretIndex - lockedRowCount + headerShift;
            }
			else if (caretIndex >= verticalScrollPosition + rowCount - partialRow - headerShift)
			{
                newVerticalScrollPosition = Math.min(maxVerticalScrollPosition,
										caretIndex - rowCount + partialRow + headerShift + 1);
			}
		}

		if (!isNaN(newVerticalScrollPosition))
		{
			if (verticalScrollPosition != newVerticalScrollPosition)
			{
				var se:ScrollEvent = new ScrollEvent(ScrollEvent.SCROLL);
				se.detail = ScrollEventDetail.THUMB_POSITION;
				se.direction = ScrollEventDirection.VERTICAL;
				se.delta = newVerticalScrollPosition - verticalScrollPosition;
				se.position = newVerticalScrollPosition;
				verticalScrollPosition = newVerticalScrollPosition;
				dispatchEvent(se);
			}
			// bail if we page faulted
			if (!iteratorValid) 
			{
				keySelectionPending = true;
				return;
			}
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
        var rowCount:int = listItems.length;
        var partialRow:int = (rowInfo[rowCount-1].y + rowInfo[rowCount-1].height >
                                  listContent.height) ? 1 : 0;

		var headerShift:int = headerVisible ? 1 : 0;

		if (lastKey == Keyboard.PAGE_DOWN)
		{
			if (lockedRowCount >= rowCount - partialRow - 1)
				caretIndex = Math.min(verticalScrollPosition + lockedRowCount - headerShift,
								  collection.length - 1);
			// set caret to last full row of new screen
			else
				caretIndex = Math.min(verticalScrollPosition + rowCount - headerShift - partialRow - 1,
								  collection.length - 1);
		}

        var listItem:IListItemRenderer;
        var bSelChanged:Boolean = false;

		if (bSelectItem && caretIndex - verticalScrollPosition >= 0)
		{
            if (caretIndex - verticalScrollPosition + headerShift > listItems.length - 1)
            	caretIndex = listItems.length - 1 + verticalScrollPosition - headerShift;
            
            listItem = listItems[caretIndex - verticalScrollPosition + headerShift][0];
            if (listItem)
            {
				uid = itemToUID(listItem.data);
				listItem = visibleData[uid];
				if (!bCtrlKey)
				{
					selectItem(listItem, bShiftKey, bCtrlKey);
					bSelChanged = true;
				}
				if (bCtrlKey)
				{
					drawItem(listItem, selectedData[uid] != null, uid == highlightUID, true);
				}
            }
		}

        if (bSelChanged)
        {
        	var evt:ListEvent = new ListEvent(ListEvent.CHANGE);
        	evt.itemRenderer = listItem;
        	var pt:Point = itemRendererToIndices(listItem);
        	if (pt)
        	{
        		evt.rowIndex = pt.y;
        		evt.columnIndex = pt.x;
        	}
            dispatchEvent(evt);
        }
    }

    /**
	 *  @private
	 */
	override public function itemRendererToIndex(data:IListItemRenderer):int
	{
		var i:int = super.itemRendererToIndex(data);

		return i == int.MIN_VALUE ? i : headerVisible ? i - 1 : i;
	}

    /**
	 *  @private
	 */
	override protected function addToFreeItemRenderers(item:IListItemRenderer):void
	{
		// trace("putting " + item + " on free list");
		DisplayObject(item).visible = false;
		
		delete rowMap[item.name];
		
		if (columnMap[item.name])
		{
			var c:DataGridColumn = columnMap[item.name];
			if (freeItemRenderersTable[c] == undefined)
				freeItemRenderersTable[c] = [];
			freeItemRenderersTable[c].push(item);
			delete columnMap[item.name];
		}
	}

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

	/**
     *  Creates a new DataGridListData instance and populates the fields based on
     *  the input data provider item. 
     *  
     *  @param data The data provider item used to populate the ListData.
     *  @param uid The UID for the item.
     *  @param rowNum The index of the item in the data provider.
     *  @param columnNum The columnIndex associated with this item. 
     *  @param column The column associated with this item.
     *  
     *  @return A newly constructed ListData object.
     */
	protected function makeListData(data:Object, uid:String, 
		rowNum:int, columnNum:int, column:DataGridColumn):BaseListData
	{
		if (data is DataGridColumn)
		{
			return new DataGridListData((column.headerText != null) ? column.headerText : column.dataField, 
				column.dataField, columnNum, uid, this, rowNum);
		}
		else
		{ 
			return new DataGridListData(column.itemToLabel(data), column.dataField, 
				columnNum, uid, this, rowNum);
		}
	}

    /**
	 *  @private
	 *  This grid just returns the column size,
	 *  but could handle column spanning.
	 */
	mx_internal function getWidthOfItem(item:IListItemRenderer,
								   col:DataGridColumn):Number
	{
		return col.width;
	}

    /**
     *  Calculates the row height of columns in a row.
	 *  If <code>skipVisible</code> is <code>true></code> 
	 *  the DataGridBase already knows the height of
	 *  the renderers for the column that do fit in the display area
	 *  so this method only needs to calculate for the item renderers
	 *  that would exist if other columns in that row were in the
	 *  display area.  This is needed so that if the user scrolls
	 *  horizontally, the height of the row does not adjust as different
	 *  columns appear and disappear.
	 *
	 *  @param data The data provider item for the row.
	 *
	 *  @param hh The current height of the row.
	 *
	 *  @param skipVisible If <code>true</code>, no need to measure item
	 *  renderers in visible columns
	 *
	 *  @return The row height, in pixels.
     */
	protected function calculateRowHeight(data:Object, hh:Number, skipVisible:Boolean = false):Number
	{
		return NaN;
	}

    /**
     *  Get the appropriate renderer for a column, using the default renderer if none specified
     */
    mx_internal function columnItemRenderer(c:DataGridColumn, forHeader:Boolean):IListItemRenderer
	{
		var renderer:IListItemRenderer;
		if (forHeader)
		{
			if (c.headerRenderer)
				renderer = c.headerRenderer.newInstance();
		}
		else
		{
			if (c.itemRenderer)
				renderer = c.itemRenderer.newInstance();
		}
		if (!renderer)
			renderer = itemRenderer.newInstance();

		renderer.owner = this;
		return renderer;
	}

    /**
     *  Get the headerWordWrap for a column, using the default wordWrap if none specified
     */
    mx_internal function columnHeaderWordWrap(c:DataGridColumn):Boolean
	{
		if (c.headerWordWrap == true)
			return true;
		if (c.headerWordWrap == false)
			return false;
		
		return wordWrap;
	}

    /**
     *  Get the wordWrap for a column, using the default wordWrap if none specified
     */
    mx_internal function columnWordWrap(c:DataGridColumn):Boolean
	{
		if (c.wordWrap == true)
			return true;
		if (c.wordWrap == false)
			return false;
		
		return wordWrap;
	}

	/**
	 *  @private
	 */
	override protected function drawHighlightIndicator(indicator:Sprite, x:Number, y:Number, width:Number, height:Number, color:uint, itemRenderer:IListItemRenderer):void
	{
		super.drawHighlightIndicator(indicator, x, y, unscaledWidth - viewMetrics.left - viewMetrics.right, height, color, itemRenderer);
	}

	/**
	 *  @private
	 */
	override protected function drawCaretIndicator(indicator:Sprite, x:Number, y:Number, width:Number, height:Number, color:uint, itemRenderer:IListItemRenderer):void
	{
		super.drawCaretIndicator(indicator, x, y, unscaledWidth - viewMetrics.left - viewMetrics.right, height, color, itemRenderer);
	}

	/**
	 *  @private
	 */
	override protected function drawSelectionIndicator(indicator:Sprite, x:Number, y:Number, width:Number, height:Number, color:uint, itemRenderer:IListItemRenderer):void
	{
		super.drawSelectionIndicator(indicator, x, y, unscaledWidth - viewMetrics.left - viewMetrics.right, height, color, itemRenderer);
	}

	/**
	 *  @private
	 */
	override mx_internal function mouseEventToItemRendererOrEditor(event:MouseEvent):IListItemRenderer
	{
		var target:DisplayObject = DisplayObject(event.target);

        if (target == listContent)
		{
			var pt:Point = new Point(event.stageX, event.stageY);
			pt = listContent.globalToLocal(pt);
			var yy:Number = 0;
			var n:int = listItems.length;
			for (var i:int = 0; i < n; i++)
			{
				if (listItems[i].length)
				{
					if (pt.y < yy + rowInfo[i].height)
					{
						var xx:Number = 0;
						var m:int = listItems[i].length;
						for (var j:int = 0; j < m; j++)
						{
							if (pt.x < xx + visibleColumns[j].width)
								return listItems[i][j];
							xx += visibleColumns[j].width;
						}
					}
				}
				yy += rowInfo[i].height;
			}
		}
		else if (target == highlightIndicator)
			return lastHighlightItemRenderer;

        while (target && target != this)
        {
            if (target is IListItemRenderer && target.parent == listContent)
            {
                if (target.visible)
                    return IListItemRenderer(target);
                break;
            }

            target = target.parent;
        }
		return null;
	}

	/**
	 *  @private
	 */
	mx_internal function get gridColumnMap():Object
	{
		return columnMap;
	}
}

}
