////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.controls.dataGridClasses
{

import flash.display.DisplayObject;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.utils.Dictionary;

import mx.collections.CursorBookmark;
import mx.collections.IViewCursor;
import mx.collections.ItemResponder;
import mx.collections.errors.ItemPendingError;
import mx.controls.listClasses.BaseListData;
import mx.controls.listClasses.IDropInListItemRenderer;
import mx.controls.listClasses.IListItemRenderer;
import mx.controls.listClasses.ListBase;
import mx.controls.listClasses.ListBaseContentHolder;
import mx.controls.listClasses.ListBaseSeekPending;
import mx.controls.listClasses.ListRowInfo;
import mx.core.IFactory;
import mx.core.IFlexModuleFactory;
import mx.core.IFontContextComponent;
import mx.core.IInvalidating;
import mx.core.IUIComponent;
import mx.core.mx_internal;
import mx.core.SpriteAsset;
import mx.core.UIComponentGlobals;
import mx.core.UITextField;
import mx.events.TweenEvent;

use namespace mx_internal;

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
public class DataGridBase extends ListBase implements IFontContextComponent
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
        
        defaultRowCount = 7;    // default number of rows is 7
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
     *  The set of visible locked columns.
     */
    mx_internal var visibleLockedColumns:Array;

    /**
     *  The header sub-component
     */
    protected var header:DataGridHeaderBase;

    /**
     *  The class to use as the DGHeader
     */
    mx_internal var headerClass:Class = DataGridHeader;

    protected var headerMask:Shape;

    /**
     *  The header sub-component for locked columns
     */
    protected var lockedColumnHeader:DataGridHeaderBase;

    private var lockedColumnHeaderMask:Shape;

    /**
     *  The sub-component that contains locked rows
     */
    protected var lockedRowContent:DataGridLockedRowContentHolder;

    private var lockedRowMask:Shape;

    /**
     *  The sub-component that contains locked rows for locked columns
     */
    protected var lockedColumnAndRowContent:DataGridLockedRowContentHolder;

    private var lockedColumnAndRowMask:Shape;

    /**
     *  The sub-component that contains locked columns
     */
    protected var lockedColumnContent:ListBaseContentHolder;

    private var lockedColumnMask:Shape;

    /**
     *  @private
     *  automation
     */
    mx_internal function get dataGridHeader():DataGridHeaderBase
    {
        return header;
    }

    /**
     *  @private
     *  automation
     */
    mx_internal function get dataGridLockedColumnHeader():DataGridHeaderBase
    {
        return lockedColumnHeader;
    }

    /**
     *  @private
     *  automation
     */
    mx_internal function get dataGridLockedColumnAndRows():ListBaseContentHolder
    {
        return lockedColumnAndRowContent;
    }

    /**
     *  @private
     *  automation
     */
    mx_internal function get dataGridLockedRows():ListBaseContentHolder
    {
        return lockedRowContent;
    }

    /**
     *  @private
     *  automation
     */
    mx_internal function get dataGridLockedColumns():ListBaseContentHolder
    {
        return lockedColumnContent;
    }

    /**
     *  Flag specifying that the set of visible columns and/or their sizes needs to
     *  be recomputed.
     */
    mx_internal var columnsInvalid:Boolean = true;

    /**
     *  @private
     *  must be overridden by subclasses
     */
    mx_internal function columnRendererChanged(c:DataGridColumn):void
    {
    }

    /**
     *  @private
     *  must be overridden by subclasses
     */
    mx_internal function resizeColumn(col:int, w:Number):void
    {
    }

    // these three keep track of the key selection that caused
    // the page fault
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
    //  columns
    //----------------------------------

    /**
     *  @private
     *  must be overridden by subclasses
     */
    public function get columns():Array
    {
        return null;
    }

    /**
     *  @private
     */
    public function set columns(value:Array):void
    {
    }

    //----------------------------------
    //  fontContext
    //----------------------------------
    
    /**
     *  @inheritDoc 
     */
    public function get fontContext():IFlexModuleFactory
    {
        return moduleFactory;
    }

    /**
     *  @private
     */
    public function set fontContext(moduleFactory:IFlexModuleFactory):void
    {
        this.moduleFactory = moduleFactory;
    }
    
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
    //  lockedColumnCount
    //----------------------------------

    private var lockedColumnCountChanged:Boolean = false;

    /**
     *  @private
     *  Storage for the lockedColumnCount property.
     */
    mx_internal var _lockedColumnCount:int = 0;

    [Inspectable(defaultValue="0")]

    /**
     *  The index of the first column in the control that scrolls.
     *  Columns with indexes that are lower than this value remain fixed
     *  in view.  Not supported by all list classes.
     * 
     *  @default 0
     */
    public function get lockedColumnCount():int
    {
        return _lockedColumnCount;
    }

    /**
     *  @private
     */
    public function set lockedColumnCount(value:int):void
    {
        _lockedColumnCount = value;
        lockedColumnCountChanged = true;
        itemsSizeChanged = true;

        columnsInvalid = true;

        invalidateDisplayList();
    }

    //----------------------------------
    //  lockedRowCount
    //----------------------------------

    private var lockedRowCountChanged:Boolean = false;

    /**
     *  @private
     *  Storage for the lockedRowCount property.
     */
    mx_internal var _lockedRowCount:int = 0;

    [Inspectable(defaultValue="0")]

    /**
     *  The index of the first row in the control that scrolls.
     *  Rows above this one remain fixed in view.
     * 
     *  @default 0
     */
    public function get lockedRowCount():int
    {
        return _lockedRowCount;
    }

    /**
     *  @private
     */
    public function set lockedRowCount(value:int):void
    {
        _lockedRowCount = value;
        lockedRowCountChanged = true;
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
        if (value == _showHeaders)
            return;

        _showHeaders = value;
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
     *  Sizes and positions the column headers, columns, and items based on the
     *  size of the DataGrid.
     */
    override protected function updateDisplayList(unscaledWidth:Number,
                                                  unscaledHeight:Number):void
    {
        if (headerVisible && header)
        {
            header.visibleColumns = visibleColumns;
            header.invalidateSize();
            header.validateNow();
        }
        
        if (lockedColumnCountChanged)
        {
            lockedColumnCountChanged = false;

            if (lockedColumnCount > 0)
            {
                if (!lockedColumnContent)
                {
                    lockedColumnHeader = new headerClass();
                    lockedColumnHeader.styleName = this;
                    addChild(lockedColumnHeader);
                    lockedColumnAndRowContent = new DataGridLockedRowContentHolder(this);
                    lockedColumnAndRowContent.styleName = this;
                    addChild(lockedColumnAndRowContent)
                    lockedColumnContent = new ListBaseContentHolder(this);
                    lockedColumnContent.styleName = this;
                    addChild(lockedColumnContent);
                }

                lockedColumnHeader.visible = true;
                lockedColumnAndRowContent.visible = (lockedRowCount > 0);
                lockedColumnContent.visible = true;
            }
            else
            {
                if (lockedColumnContent)
                {
                    lockedColumnHeader.visible = false;
                    lockedColumnAndRowContent.visible = false;
                    lockedColumnContent.visible = false;
                }
            }
        }

        if (lockedRowCountChanged && iterator)
        {
            lockedRowCountChanged = false;

            if (lockedRowCount > 0)
            {
                if (!lockedRowContent)
                {
                    lockedRowContent = new DataGridLockedRowContentHolder(this);
                    lockedRowContent.styleName = this;
                    addChild(lockedRowContent);
                }
                lockedRowContent.visible = true;
            }
            else
            {
                if (lockedRowContent)
                    lockedRowContent.visible = false;
            }

            if (verticalScrollPosition < lockedRowCount)
            {
                seekPositionSafely(lockedRowCount);
            }
        }

        // ensure lockedColumnAndRowContent above other content
        var ci:int;
        var lcri:int;
        if (lockedRowContent && lockedColumnAndRowContent)
        {
            lcri = getChildIndex(lockedColumnAndRowContent);
            ci = getChildIndex(lockedRowContent);
            if (lcri < ci)
                setChildIndex(lockedRowContent, lcri);
        }
        if (lockedColumnContent && lockedColumnAndRowContent)
        {
            lcri = getChildIndex(lockedColumnAndRowContent);
            ci = getChildIndex(lockedColumnContent);
            if (lcri < ci)
                setChildIndex(lockedColumnContent, lcri);
        }
        if (headerVisible && lockedColumnHeader)
        {
            lockedColumnHeader.visibleColumns = visibleLockedColumns;
            lockedColumnHeader.invalidateSize();
            lockedColumnHeader.validateNow();
        }

        super.updateDisplayList(unscaledWidth, unscaledHeight);
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
        return makeRows(listContent, left, top, right, bottom, firstCol, firstRow, byCount, rowsNeeded);
    }

    /**
     *  @private
     */
    protected function makeRows(contentHolder:ListBaseContentHolder, 
                                                left:Number, top:Number,
                                                right:Number, bottom:Number,
                                                firstCol:int, firstRow:int,
                                                byCount:Boolean = false, rowsNeeded:uint = 0, 
                                                alwaysCleanup:Boolean = false):Point
    {
        var xx:Number;
        var yy:Number;
        var hh:Number;

        var bSelected:Boolean = false;
        var bHighlight:Boolean = false;
        var bCaret:Boolean = false;

        var i:int;
        var j:int;
        var n:int;

        var colNum:int = 0;
        var rowNum:int = 0;
        var rowsMade:int = 0;

        var item:IListItemRenderer;
        var listItems:Array = contentHolder.listItems;
        var iterator:IViewCursor = contentHolder.iterator;
        var visibleData:Object = contentHolder.visibleData;
        var rowInfo:Object = contentHolder.rowInfo;

        // bail if we have no columns
        if ((!visibleColumns || visibleColumns.length == 0) && lockedColumnCount == 0)
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
            contentHolder.visibleData = {};
            return new Point(0,0);
        }

        invalidateSizeFlag = true;

        var data:Object;
        var uid:String;
        var more:Boolean = true;
        var valid:Boolean = true;

            yy = top;
            rowNum = firstRow;
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
                    catch(e:ItemPendingError)
                    {
                        lastSeekPending = new ListBaseSeekPending(CursorBookmark.CURRENT, 0)
                        e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler, 
                                                        lastSeekPending));
                        more = false;
                        iteratorValid = false;
                    }
                }

                hh = 0;
                uid = null;

                prepareRowArray(contentHolder, rowNum);

                if (valid)
                {
                    uid = itemToUID(data);
                    hh = makeRow(contentHolder, rowNum, left, right, yy, data, uid);
                }
                else
                {
                    hh = rowNum > 1 ? rowInfo[rowNum - 1].height : rowHeight;
                }
                if (valid && variableRowHeight)
                {
                    hh = Math.ceil(calculateRowHeight(data, hh, true));
                }
                if (valid)
                    adjustRow(contentHolder, rowNum, yy, hh);
                else
                    clearRow(contentHolder, rowNum);

                bSelected = selectedData[uid] != null;
                bHighlight = highlightUID == uid;
                bCaret = caretUID == uid;
                setRowInfo(contentHolder, rowNum, yy, hh, uid);
                // trace("rowNum = " + rowNum);
                if (valid)
                    drawVisibleItem(uid, bSelected, bHighlight, bCaret);
                if (hh == 0) // hh can be zero if we had zero width
                    hh = rowHeight;
                yy += hh;
                rowNum++;
                rowsMade++;
            }
            // byCount means we're making rows and wont get all the way to the bottom
            // so we skip this cleanup pass
            if (!byCount || alwaysCleanup)
            {
                // trace("MakeRowsAndColumns rowNum = " + rowNum);
                // delete extra rows
                while (rowNum < listItems.length)
                {
                    removeExtraRow(contentHolder);
                }
            }
            // trace("MakeRowsAndColumns listItems.length = " + listItems.length);

        invalidateSizeFlag = false;

        return new Point(colNum, rowsMade);
    }

    /** 
     *  Make sure there's a slot in the row arrays for the given row number
     *  @param contentHolder The set of rows (locked rows, regular rows)
     *  @param rowNum The row number
     */
    protected function prepareRowArray(contentHolder:ListBaseContentHolder, rowNum:int):void
    {
        var listItems:Array = contentHolder.listItems;
        var columnContent:ListBaseContentHolder;

        if (lockedColumnCount > 0)
        {
            if (contentHolder == lockedRowContent)
                columnContent = lockedColumnAndRowContent;
            else
                columnContent = lockedColumnContent;
        }
        else
            columnContent = null;


        if (!listItems[rowNum])
            listItems[rowNum] = [];
        if (columnContent)
            if (!columnContent.listItems[rowNum])
                columnContent.listItems[rowNum] = [];
    }

    /** 
     *  Make the renderers for the given rowNum, dataObject and uid
     *  @param contentHolder The set of rows (locked rows, regular rows)
     *  @param rowNum The row number
     *  @param left The offset from the left side for the first column
     *  @param right The offset from the right side for the last column
     *  @param yy The y position of the row
     *  @param data The data for the row
     *  @param uid The uid for the data
     *  @return Height of the row
     */
    protected function makeRow(contentHolder:ListBaseContentHolder, rowNum:int, left:Number, right:Number, yy:Number, data:Object, uid:String):Number
    {
        var listItems:Array = contentHolder.listItems;
        var columnContent:ListBaseContentHolder;
        var item:IListItemRenderer;
        var extraItem:IListItemRenderer;
        var c:DataGridColumn;
        var itemSize:Point;

        if (lockedColumnCount > 0)
        {
            if (contentHolder == lockedRowContent)
                columnContent = lockedColumnAndRowContent;
            else
                columnContent = lockedColumnContent;
        }
        else
            columnContent = null;

        var colNum:int = 0;
        var xx:Number = left;
        var hh:Number = 0;

        while (colNum < visibleLockedColumns.length)
        {
            c = visibleLockedColumns[colNum];
            item = setupColumnItemRenderer(c, columnContent, rowNum, colNum, data, uid);
            itemSize = layoutColumnItemRenderer(c, item, xx, yy);
            xx += itemSize.x;
            colNum++;
            hh = Math.ceil(Math.max(hh, variableRowHeight ? itemSize.y + cachedPaddingTop + cachedPaddingBottom : rowHeight));
        }
        if (visibleLockedColumns.length)
        {
            while (columnContent.listItems[rowNum].length > colNum)
            {
                // remove extra columns
                extraItem = columnContent.listItems[rowNum].pop();
                addToFreeItemRenderers(extraItem);
            }
        }
        colNum = 0;
        xx = left;
        while (xx < right && colNum < visibleColumns.length)
        {
            c = visibleColumns[colNum];
            item = setupColumnItemRenderer(c, contentHolder, rowNum, colNum, data, uid);
            itemSize = layoutColumnItemRenderer(c, item, xx, yy);
            xx += itemSize.x;
            colNum++;
            hh = Math.ceil(Math.max(hh, variableRowHeight ? itemSize.y + cachedPaddingTop + cachedPaddingBottom : rowHeight));
        }
        while (listItems[rowNum].length > colNum)
        {
            // remove extra columns
            extraItem = listItems[rowNum].pop();
            addToFreeItemRenderers(extraItem);
        }

        return hh;
    }

    /** 
     *  Remove renderers from a row that should be empty for the given rowNum
     *  @param contentHolder The set of rows (locked rows, regular rows)
     *  @param rowNum The row number
     */
    protected function clearRow(contentHolder:ListBaseContentHolder, rowNum:int):void
    {
        var listItems:Array = contentHolder.listItems;
        var columnContent:ListBaseContentHolder;
        var extraItem:IListItemRenderer;

        if (lockedColumnCount > 0)
        {
            if (contentHolder == lockedRowContent)
                columnContent = lockedColumnAndRowContent;
            else
                columnContent = lockedColumnContent;
        }
        else
            columnContent = null;

        while (listItems[rowNum].length)
        {
            // remove extra columns
            extraItem = listItems[rowNum].pop();
            addToFreeItemRenderers(extraItem);
        }
        if (columnContent)
        {
            while (columnContent.listItems[rowNum].length)
            {
                // remove extra columns
                extraItem = columnContent.listItems[rowNum].pop();
                addToFreeItemRenderers(extraItem);
            }
        }
        
    }

    /** 
     *  Adjust size and positions of the renderers for the given rowNum, row position and height
     *  @param contentHolder The set of rows (locked rows, regular rows)
     *  @param rowNum The row number
     *  @param yy The y position of the row
     *  @param hh The height of the row
     */
    protected function adjustRow(contentHolder:ListBaseContentHolder, rowNum:int, yy:Number, hh:Number):void
    {
        var listItems:Array = contentHolder.listItems;
        var columnContent:ListBaseContentHolder;
        var i:int;
        var j:int;
        var n:int;
        var colNum:int;
        var item:IListItemRenderer;

        if (lockedColumnCount > 0)
        {
            if (contentHolder == lockedRowContent)
                columnContent = lockedColumnAndRowContent;
            else
                columnContent = lockedColumnContent;
        }
        else
            columnContent = null;

        if (listItems[rowNum])
        {
            if (columnContent)
            {
                n = columnContent.listItems[rowNum].length;
                for (j = 0; j < n; j++)
                    columnContent.listItems[rowNum][j].setActualSize(
                        columnContent.listItems[rowNum][j].width,
                            hh - cachedPaddingTop - cachedPaddingBottom);
            }
            n = listItems[rowNum].length;
            for (j = 0; j < n; j++)
                listItems[rowNum][j].setActualSize(
                    listItems[rowNum][j].width,
                        hh - cachedPaddingTop - cachedPaddingBottom);
        }
        if (cachedVerticalAlign != "top")
        {
            if (cachedVerticalAlign == "bottom")
            {
                colNum = listItems[rowNum].length;
                for (j = 0; j < colNum; j++)
                {
                    item = listItems[rowNum][j];
                    item.move(item.x, yy + hh - cachedPaddingBottom - item.getExplicitOrMeasuredHeight());
                }
                if (columnContent)
                {
                    n = columnContent.listItems[rowNum].length
                    for (j = 0; j < n; j++)
                    {
                        item = columnContent.listItems[rowNum][j];
                        item.move(item.x, yy + hh - cachedPaddingBottom - item.getExplicitOrMeasuredHeight());
                    }
                }
            }
            else
            {
                colNum = listItems[rowNum].length;
                for (j = 0; j < colNum; j++)
                {
                    item = listItems[rowNum][j];
                    item.move(item.x, yy + cachedPaddingTop + (hh - cachedPaddingBottom - cachedPaddingTop - item.getExplicitOrMeasuredHeight()) / 2);
                }
                if (columnContent)
                {
                    n = columnContent.listItems[rowNum].length
                    for (j = 0; j < n; j++)
                    {
                        item = columnContent.listItems[rowNum][j];
                        item.move(item.x, yy + cachedPaddingTop + (hh - cachedPaddingBottom - cachedPaddingTop - item.getExplicitOrMeasuredHeight()) / 2);
                    }
                }
            }

        }
    }

    /** 
     *  Set the rowInfo for the given rowNum, row position and height
     *  @param contentHolder The set of rows (locked rows, regular rows)
     *  @param rowNum The row number
     *  @param yy The y position of the row
     *  @param hh The height of the row
     *  @param uid The UID for the data
     */
    protected function setRowInfo(contentHolder:ListBaseContentHolder, rowNum:int, yy:Number, hh:Number, uid:String):void
    {
        var listItems:Array = contentHolder.listItems;
        var rowInfo:Object = contentHolder.rowInfo;
        var columnContent:ListBaseContentHolder;
        if (lockedColumnCount > 0)
        {
            if (contentHolder == lockedRowContent)
                columnContent = lockedColumnAndRowContent;
            else
                columnContent = lockedColumnContent;
        }
        else
            columnContent = null;

        rowInfo[rowNum] = new ListRowInfo(yy, hh, uid);
        if (columnContent)
            columnContent.rowInfo[rowNum] = rowInfo[rowNum];
    }

    /** 
     *  Remove extra row from the end of the contentHolder
     *  @param contentHolder The set of rows (locked rows, regular rows)
     */
    protected function removeExtraRow(contentHolder:ListBaseContentHolder):void
    {
        var item:IListItemRenderer;
        var listItems:Array = contentHolder.listItems;
        var rowInfo:Object = contentHolder.rowInfo;
        var columnContent:ListBaseContentHolder;
        if (lockedColumnCount > 0)
        {
            if (contentHolder == lockedRowContent)
                columnContent = lockedColumnAndRowContent;
            else
                columnContent = lockedColumnContent;
        }
        else
            columnContent = null;

        var rr:Array = listItems.pop();
        rowInfo.pop();
        while (rr.length)
        {
            item = rr.pop();
            addToFreeItemRenderers(item);
        }
        if (columnContent)
        {
            columnContent.rowInfo.pop();
            rr = columnContent.listItems.pop();
            while (rr.length)
            {
                item = rr.pop();
                addToFreeItemRenderers(item);
            }
        }
    }

    /**
     *  Setup an item renderer for a column and put it in the listItems array
     *  at the requested position
     *  @param c The DataGridColumn for the renderer
     *  @param contentHolder The set of rows (locked rows, regular rows)
     *  @param rowNum The row number
     *  @param colNum The column number
     *  @param data The data for the row
     *  @param uid The uid for the data
     *  @return The renderer for this column and row
     */
    protected function setupColumnItemRenderer(c:DataGridColumn, contentHolder:ListBaseContentHolder,
                    rowNum:int, colNum:int, data:Object, uid:String):IListItemRenderer
    {
        var listItems:Array = contentHolder.listItems;
        var item:IListItemRenderer;
        var rowData:DataGridListData;

        item = listItems[rowNum][colNum];
        if (!item || itemToUID(item.data) != uid
            || columnMap[item.name] != c)
        {
            item = createColumnItemRenderer(c, false, data);
            if (item.parent != contentHolder)
                contentHolder.addChild(DisplayObject(item));

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
            contentHolder.visibleData[uid] = item;
        return item;
    }

    /**
     *  Size and temporarily position an itemRenderer for a column, returning its size as a Point
     *  Its final position may be adjusted later due to alignment settings
     *  @param c The DataGridColumn for the renderer
     *  @param item The renderer
     *  @param xx The x position
     *  @param yy The y position
     *  @return Size of the renderer as a Point
     */
    protected function layoutColumnItemRenderer(c:DataGridColumn, item:IListItemRenderer, xx:Number, yy:Number):Point
    {
        var ww:Number;
        var rh:Number;
        item.explicitWidth = ww = getWidthOfItem(item, c);
        UIComponentGlobals.layoutManager.validateClient(item, true);
        rh = item.getExplicitOrMeasuredHeight();
        item.setActualSize(ww, variableRowHeight
                ? item.getExplicitOrMeasuredHeight()
                : rowHeight - cachedPaddingTop - cachedPaddingBottom);
        item.move(xx, yy + cachedPaddingTop);
        return new Point(ww, rh);
    }

    /**
     *  Draw an item if it is visible.
     *  @param uid The uid used to find the renderer.
     *  @param selected <code>true</code> if the renderer should be drawn in
     *  its selected state.
     *  @param highlighted <code>true</code> if the renderer should be drawn in
     *  its highlighted state.
     *  @param caret <code>true</code> if the renderer should be drawn as if
     *  it is the selection caret.
     *  @param transition <code>true</code> if the selection state should fade in
     *  via an effect.
     */
    protected function drawVisibleItem(uid:String, 
                                       selected:Boolean = false,
                                       highlighted:Boolean = false,
                                       caret:Boolean = false,
                                       transition:Boolean = false):void
    {
        if (visibleData[uid])
            drawItem(visibleData[uid], selected, highlighted, caret, transition);
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
        
        var i:int;
        var r:IListItemRenderer;
        
        for (i = 0; i < visibleColumns.length; i++)
        {
            r = (item.parent as ListBaseContentHolder).listItems[rowIndex][i];
            updateRendererDisplayList(r);
        }
        
        if (lockedColumnCount)
        {
            var columnContents:ListBaseContentHolder;
            if (item.parent == listContent)
                columnContents = lockedColumnContent;
            else
                columnContents = lockedColumnAndRowContent;
            
            for (i = 0; i < visibleLockedColumns.length; i++)
            {
                r = columnContents.listItems[rowIndex][i];
                updateRendererDisplayList(r);
            }
        }
    }

    /**
     *  redraw the renderer synchronously
     *  @param r the renderer;
     */
    protected function updateRendererDisplayList(r:IListItemRenderer):void
    {
        if (r is IInvalidating)
        {
            var ui:IInvalidating = IInvalidating(r);
            ui.invalidateDisplayList();
            ui.validateNow();
        }
    }

    /**
     *  @private
     */
    override protected function addToFreeItemRenderers(item:IListItemRenderer):void
    {
        // trace("putting " + item + " on free list");
        DisplayObject(item).visible = false;
        
        delete rowMap[item.name];
        var factory:IFactory = factoryMap[item];

        // Only delete from visibleData if we are freeing the renderer for
        // the first visible column
        var UID:String = itemToUID(item.data);
        var visibleData:Object = ListBaseContentHolder(item.parent).visibleData;
        if (visibleData[UID] == item)
            delete visibleData[UID];

        if (columnMap[item.name])
        {
            var c:DataGridColumn = columnMap[item.name];
            if (factory == c.itemRenderer)
            {
                if (freeItemRenderersTable[c] == undefined)
                    freeItemRenderersTable[c] = [];
                freeItemRenderersTable[c].push(item);
            }
            if (!c.freeItemRenderersByFactory)
                c.freeItemRenderersByFactory = new Dictionary(true);
            if (c.freeItemRenderersByFactory[factory] == undefined)
                c.freeItemRenderersByFactory[factory] = new Dictionary(true);
            c.freeItemRenderersByFactory[factory][item] = 1;

            delete columnMap[item.name];
        }
    }

    /**
     *  @private
     */
    override protected function purgeItemRenderers():void
    {
        var listItems:Array;
        var row:Array;
        var item:IListItemRenderer;

        if (lockedRowContent)
        {
            listItems = lockedRowContent.listItems;
            while (listItems.length)
            {
                row = listItems.pop();
                while (row.length)
                {
                    item = IListItemRenderer(row.pop());
                    if (item)
                    {
                        lockedRowContent.removeChild(DisplayObject(item));
                        delete lockedRowContent.visibleData[itemToUID(item.data)];
                    }
                }
            }
        }
        if (lockedColumnContent)
        {
            listItems = lockedColumnContent.listItems;
            while (listItems.length)
            {
                row = listItems.pop();
                while (row.length)
                {
                    item = IListItemRenderer(row.pop());
                    if (item)
                    {
                        lockedColumnContent.removeChild(DisplayObject(item));
                        delete lockedColumnContent.visibleData[itemToUID(item.data)];
                    }
                }
            }
        }
        if (lockedColumnAndRowContent)
        {
            listItems = lockedColumnAndRowContent.listItems;
            while (listItems.length)
            {
                row = listItems.pop();
                while (row.length)
                {
                    item = IListItemRenderer(row.pop());
                    if (item)
                    {
                        lockedColumnAndRowContent.removeChild(DisplayObject(item));
                        delete lockedColumnAndRowContent.visibleData[itemToUID(item.data)];
                    }
                }
            }
        }

        super.purgeItemRenderers();
    }

    /**
     *  @private
     */
    override public function indicesToIndex(rowIndex:int, colIndex:int):int
    {
        return rowIndex;
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
     *  Returns the item renderer for a column cell or for a column header. 
     *  This method returns the default item renderer if no custom render is assigned
     *  to the column.
     *
     *  <p>This method is public so that is can be accessed by the DataGridHeader class, 
     *  and is primarily used in subclasses of the DataGrid control.</p>
     * 
     *  @param c The DataGridColumn instance of the item renderer.
     * 
     *  @param forHeader <code>true</code> to return the header item renderer, 
     *  and <code>false</code> to return the item render for the column cells.
     * 
     *  @param data If <code>forHeader</code> is <code>false</code>, 
     *  the <code>data</code> Object for the item renderer. 
     *  If <code>forHeader</code> is <code>true</code>, 
     *  the DataGridColumn instance.  
     * 
     *  @return The item renderer.
     */
    public function createColumnItemRenderer(c:DataGridColumn, forHeader:Boolean, data:Object):IListItemRenderer
    {
        var factory:IFactory;

        // get the factory for the data
        factory = c.getItemRendererFactory(forHeader, data);
        if (!factory)
        {
            if (!data)
                factory = nullItemRenderer;
            if (!factory)
                factory = itemRenderer;
        }

        var renderer:IListItemRenderer;

        // if it is the default column factory, see if
        // the freeItemRenderersTable has a free one
        if (factory == c.itemRenderer)
        {
            if (freeItemRenderersTable[c] && freeItemRenderersTable[c].length)
            {
                renderer = freeItemRenderersTable[c].pop();
                delete c.freeItemRenderersByFactory[factory][renderer];
            }
        }
        else if (c.freeItemRenderersByFactory)
        {
            // other re-usable renderers are in the FactoryMap
            var d:Dictionary = c.freeItemRenderersByFactory[factory];
            if (d)
            {
                for (var p:* in d)
                {
                    renderer = IListItemRenderer(p);
                    delete d[p];
                    break;
                }
            }
        }

        if (!renderer)
        {
            renderer = factory.newInstance();
            renderer.styleName = c;
            factoryMap[renderer] = factory;
        }

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
    override protected function clearIndicators():void
    {
        super.clearIndicators();
        if (lockedColumnCount)
        {
            while (lockedColumnContent.selectionLayer.numChildren > 0)
            {
                lockedColumnContent.selectionLayer.removeChildAt(0);
            }
        }
        if (header)
            header.clearSelectionLayer();
        if (lockedColumnHeader)
            lockedColumnHeader.clearSelectionLayer();

    }

    /**
     *  The DisplayObject that contains the graphics that indicates
     *  which renderer is highlighted for lockedColumns.
     */
    protected var columnHighlightIndicator:Sprite;

    /**
     *  The DisplayObject that contains the graphics that indicates
     *  which renderer is the caret for lockedColumns.
     */
    protected var columnCaretIndicator:Sprite;

    /**
     *  @private
     */
    override protected function drawHighlightIndicator(indicator:Sprite, x:Number, y:Number, width:Number, height:Number, color:uint, itemRenderer:IListItemRenderer):void
    {
        super.drawHighlightIndicator(indicator, x, y, unscaledWidth - viewMetrics.left - viewMetrics.right, height, color, itemRenderer);
        if (lockedColumnCount)
        {
            var columnContents:ListBaseContentHolder;
            if (itemRenderer.parent == listContent)
                columnContents = lockedColumnContent;
            else
                columnContents = lockedColumnAndRowContent;
            var selectionLayer:Sprite = columnContents.selectionLayer;

            if (!columnHighlightIndicator)
            {
                columnHighlightIndicator = new SpriteAsset();
                columnContents.selectionLayer.addChild(DisplayObject(columnHighlightIndicator));
            }
            else
            {
                if (columnHighlightIndicator.parent != selectionLayer)
                    selectionLayer.addChild(columnHighlightIndicator);
                else
                    selectionLayer.setChildIndex(DisplayObject(columnHighlightIndicator),
                                             selectionLayer.numChildren - 1);
            }
            super.drawHighlightIndicator(columnHighlightIndicator, x, y, columnContents.width, height, color, itemRenderer);
        }
    }

    /**
     *  @private
     */
    override protected function clearHighlightIndicator(indicator:Sprite, itemRenderer:IListItemRenderer):void
    {
        super.clearHighlightIndicator(indicator, itemRenderer);
        if (lockedColumnCount)
        {
            if (columnHighlightIndicator)
                Sprite(columnHighlightIndicator).graphics.clear();
        }
    }

    /**
     *  @private
     */
    override protected function drawCaretIndicator(indicator:Sprite, x:Number, y:Number, width:Number, height:Number, color:uint, itemRenderer:IListItemRenderer):void
    {
        super.drawCaretIndicator(indicator, x, y, unscaledWidth - viewMetrics.left - viewMetrics.right, height, color, itemRenderer);
        if (lockedColumnCount)
        {
            var columnContents:ListBaseContentHolder;
            if (itemRenderer.parent == listContent)
                columnContents = lockedColumnContent;
            else
                columnContents = lockedColumnAndRowContent;
            var selectionLayer:Sprite = columnContents.selectionLayer;

            if (!columnCaretIndicator)
            {
                columnCaretIndicator = new SpriteAsset();
                columnContents.selectionLayer.addChild(DisplayObject(columnCaretIndicator));
            }
            else
            {
                if (columnCaretIndicator.parent != selectionLayer)
                    selectionLayer.addChild(columnCaretIndicator);
                else
                    selectionLayer.setChildIndex(DisplayObject(columnCaretIndicator),
                                             selectionLayer.numChildren - 1);
            }
            super.drawHighlightIndicator(columnCaretIndicator, x, y, columnContents.width, height, color, itemRenderer);
        }
    }

    /**
     *  @private
     */
    override protected function clearCaretIndicator(indicator:Sprite, itemRenderer:IListItemRenderer):void
    {
        super.clearCaretIndicator(indicator, itemRenderer);
        if (lockedColumnCount)
        {
            if (columnCaretIndicator)
                Sprite(columnCaretIndicator).graphics.clear();
        }
    }

    private var indicatorDictionary:Dictionary = new Dictionary(true);
    
    /**
     *  @private
     */
    override protected function drawSelectionIndicator(indicator:Sprite, x:Number, y:Number, width:Number, height:Number, color:uint, itemRenderer:IListItemRenderer):void
    {
        super.drawSelectionIndicator(indicator, x, y, unscaledWidth - viewMetrics.left - viewMetrics.right, height, color, itemRenderer);
        if (lockedColumnCount)
        {
            var columnContents:ListBaseContentHolder;
            if (itemRenderer.parent == listContent)
                columnContents = lockedColumnContent;
            else
                columnContents = lockedColumnAndRowContent;
            var selectionLayer:Sprite = columnContents.selectionLayer;

            var columnIndicator:Sprite = indicatorDictionary[indicator] as Sprite;
            if (!columnIndicator)
            {
                columnIndicator = new SpriteAsset();
                columnIndicator.mouseEnabled = false;
                selectionLayer.addChild(DisplayObject(columnIndicator));
                indicator.parent.addEventListener(Event.REMOVED, selectionRemovedListener);
                indicatorDictionary[indicator] = columnIndicator;
            }
            super.drawSelectionIndicator(columnIndicator, x, y, columnContents.width, height, color, itemRenderer);
        }
    }

    private function selectionRemovedListener(event:Event):void
    {
        if (!lockedColumnCount)
            return;

        var columnIndicator:Sprite = indicatorDictionary[event.target] as Sprite;
        if (columnIndicator)
        {
            columnIndicator.parent.removeChild(columnIndicator);
        }
    }

    /**
     *  @private
     */
    override mx_internal function mouseEventToItemRendererOrEditor(event:MouseEvent):IListItemRenderer
    {
        var isHighlight:Boolean = false;

        var target:DisplayObject = DisplayObject(event.target);
        var currentContent:ListBaseContentHolder;
        var visibleColumns:Array = this.visibleColumns;

        if (event.target == listContent)
            currentContent = listContent;
        else if (event.target == lockedColumnContent)
        {
            currentContent = lockedColumnContent;
            visibleColumns = visibleLockedColumns;
        }
        else if (event.target == lockedRowContent)
            currentContent = lockedRowContent;
        else if (event.target == lockedColumnAndRowContent)
        {
            currentContent = lockedColumnAndRowContent;
            visibleColumns = visibleLockedColumns;
        }
        else if (event.target == highlightIndicator)
        {
            currentContent = highlightIndicator.parent.parent as ListBaseContentHolder;
            visibleColumns = this.visibleColumns;
            if (currentContent == lockedColumnContent || currentContent == lockedColumnAndRowContent)
                visibleColumns = visibleLockedColumns;
            isHighlight = true;
        }
        else if (event.target == columnHighlightIndicator)
        {
            currentContent = columnHighlightIndicator.parent.parent as ListBaseContentHolder;
            visibleColumns = this.visibleColumns;
            if (currentContent == lockedColumnContent || currentContent == lockedColumnAndRowContent)
                visibleColumns = visibleLockedColumns;
            isHighlight = true;
        }
        

        if (isHighlight || target == currentContent)
        {
            var listItems:Array = currentContent.listItems;
            var rowInfo:Array = currentContent.rowInfo;

            var pt:Point = new Point(event.stageX, event.stageY);
            pt = currentContent.globalToLocal(pt);

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

        while (target && target != this)
        {
            if (target is IListItemRenderer && target.parent && 
                target.parent.parent == this && target.parent is ListBaseContentHolder)
            {
                if (target.visible)
                    return IListItemRenderer(target);
                break;
            }

            if (target is IUIComponent)
                target = IUIComponent(target).owner;
            else 
                target = target.parent;
        }
        return null;
    }

    /**
     *  called to determine the column under the mouse for dropping a column, if any.
     *  only checks horizontal position, assumes y value is within headers
     */
    mx_internal function getAllVisibleColumns():Array
    {
        var temp:Array = [];
        if (lockedColumnCount)
            temp = temp.concat(visibleLockedColumns);
        temp = temp.concat(visibleColumns);
        return temp;
    }

    /**
     *  @private
     */
    override protected function UIDToItemRenderer(uid:String):IListItemRenderer
    {
        var r:IListItemRenderer = visibleData[uid];
        if (!r)
        {
            if (lockedRowContent)
                r = lockedRowContent.visibleData[uid];
        }
        if (!r)
        {
            if (lockedColumnContent)
                r = lockedColumnContent.visibleData[uid];
        }
        if (!r)
        {
            if (lockedColumnAndRowContent)
                r = lockedColumnAndRowContent.visibleData[uid];
        }
        return r;
    }
    
    /**
     *  @private
     *  By default, there's a single large clip mask applied to the entire
     *  listContent area of the List.  When the List contains a mixture of
     *  device text and vector graphics (e.g.: there are custom item renderers),
     *  that clip mask imposes a rendering overhead.
     *
     *  When graphical (non-text) item renderers are used, we optimize by only
     *  applying a clip mask to the list items in the last row ... and then
     *  only when it's needed.
     *
     *  This optimization breaks down when there's a horizontal scrollbar.
     *  Rather than attempting to apply clip masks to every item along the left
     *  and right edges, we give up and use the default clip mask that covers
     *  the entire List.
     *
     *  For Lists and DataGrids containing custom item renderers, this
     *  optimization yields a 25% improvement in scrolling speed.
     */
    override mx_internal function addClipMask(layoutChanged:Boolean):void
    {
        var g:Graphics;

        // If something about the List has changed, check to see if we need
        // to clip items in the last row.
        if (layoutChanged)
        {
            if ((horizontalScrollBar && horizontalScrollBar.visible) || hasOnlyTextRenderers() || 
                    listContent.bottomOffset != 0 ||
                    listContent.topOffset != 0 ||
                    listContent.leftOffset != 0 ||
                    listContent.rightOffset != 0)
            {
                // As described above, we just use the default clip mask
                // when there's a horizontal scrollbar or the item renders
                // are all UITextFields.
                listContent.mask = maskShape;
                selectionLayer.mask = null;
                if (!headerMask)
                {
                    headerMask = new Shape();
                    addChild(headerMask);
                    g = headerMask.graphics;
                    g.beginFill(0xFFFFFF);
                    g.drawRect(0, 0, 10, 10);
                    g.endFill();
                    headerMask.visible = false;
                }
                header.mask = headerMask;
                header.selectionLayer.mask = null;
                headerMask.width = maskShape.width;
                headerMask.height = maskShape.height;
                headerMask.x = maskShape.x;
                headerMask.y = maskShape.y;

                if (lockedRowContent)
                {
                    if (!lockedRowMask)
                    {
                        lockedRowMask = new Shape();
                        addChild(lockedRowMask);
                        g = lockedRowMask.graphics;
                        g.beginFill(0xFFFFFF);
                        g.drawRect(0, 0, 10, 10);
                        g.endFill();
                        lockedRowMask.visible = false;
                    }
                    lockedRowContent.mask = lockedRowMask;
                    lockedRowContent.selectionLayer.mask = null;
                    lockedRowMask.width = maskShape.width;
                    lockedRowMask.height = maskShape.height;
                    lockedRowMask.x = maskShape.x;
                    lockedRowMask.y = maskShape.y;
                }
                if (lockedColumnContent)
                {
                    if (!lockedColumnMask)
                    {
                        lockedColumnMask = new Shape();
                        addChild(lockedColumnMask);
                        g = lockedColumnMask.graphics;
                        g.beginFill(0xFFFFFF);
                        g.drawRect(0, 0, 10, 10);
                        g.endFill();
                        lockedColumnMask.visible = false;
                    }
                    lockedColumnContent.mask = lockedColumnMask;
                    lockedColumnContent.selectionLayer.mask = null;
                    lockedColumnMask.width = maskShape.width;
                    lockedColumnMask.height = maskShape.height;
                    lockedColumnMask.x = maskShape.x;
                    lockedColumnMask.y = maskShape.y;
                }
                if (lockedColumnAndRowContent)
                {
                    if (!lockedColumnAndRowMask)
                    {
                        lockedColumnAndRowMask = new Shape();
                        addChild(lockedColumnAndRowMask);
                        g = lockedColumnAndRowMask.graphics;
                        g.beginFill(0xFFFFFF);
                        g.drawRect(0, 0, 10, 10);
                        g.endFill();
                        lockedColumnAndRowMask.visible = false;
                    }
                    lockedColumnAndRowContent.mask = lockedColumnAndRowMask;
                    lockedColumnAndRowContent.selectionLayer.mask = null;
                    lockedColumnAndRowMask.width = maskShape.width;
                    lockedColumnAndRowMask.height = maskShape.height;
                    lockedColumnAndRowMask.x = maskShape.x;
                    lockedColumnAndRowMask.y = maskShape.y;
                }
                if (lockedColumnHeader)
                {
                    if (!lockedColumnHeaderMask)
                    {
                        lockedColumnHeaderMask = new Shape();
                        addChild(lockedColumnHeaderMask);
                        g = lockedColumnHeaderMask.graphics;
                        g.beginFill(0xFFFFFF);
                        g.drawRect(0, 0, 10, 10);
                        g.endFill();
                        lockedColumnHeaderMask.visible = false;
                    }
                    lockedColumnHeader.mask = lockedColumnHeaderMask;
                    lockedColumnHeader.selectionLayer.mask = null;
                    lockedColumnHeaderMask.width = maskShape.width;
                    lockedColumnHeaderMask.height = maskShape.height;
                    lockedColumnHeaderMask.x = maskShape.x;
                    lockedColumnHeaderMask.y = maskShape.y;
                }
            }
            else
            {
                // When we're not applying the default clip mask to the whole
                // listContent, we still want to apply it to the selectionLayer
                // (so that the selection rectangle and the mouseOver rectangle
                // are properly clipped)
                listContent.mask = null;
                selectionLayer.mask = maskShape;
                if (!headerMask)
                {
                    headerMask = new Shape();
                    addChild(headerMask);
                    g = headerMask.graphics;
                    g.beginFill(0xFFFFFF);
                    g.drawRect(0, 0, 10, 10);
                    g.endFill();
                    headerMask.visible = false;
                }
                header.mask = null;
                header.selectionLayer.mask = headerMask;
                headerMask.width = maskShape.width;
                headerMask.height = maskShape.height;
                headerMask.x = maskShape.x;
                headerMask.y = maskShape.y;

                if (lockedRowContent)
                {
                    if (!lockedRowMask)
                    {
                        lockedRowMask = new Shape();
                        addChild(lockedRowMask);
                        g = lockedRowMask.graphics;
                        g.beginFill(0xFFFFFF);
                        g.drawRect(0, 0, 10, 10);
                        g.endFill();
                        lockedRowMask.visible = false;
                    }
                    lockedRowContent.mask = null;
                    lockedRowContent.selectionLayer.mask = lockedRowMask;
                    lockedRowMask.width = maskShape.width;
                    lockedRowMask.height = maskShape.height;
                    lockedRowMask.x = maskShape.x;
                    lockedRowMask.y = maskShape.y;
                }
                if (lockedColumnContent)
                {
                    if (!lockedColumnMask)
                    {
                        lockedColumnMask = new Shape();
                        addChild(lockedColumnMask);
                        g = lockedColumnMask.graphics;
                        g.beginFill(0xFFFFFF);
                        g.drawRect(0, 0, 10, 10);
                        g.endFill();
                        lockedColumnMask.visible = false;
                    }
                    lockedColumnContent.mask = null;
                    lockedColumnContent.selectionLayer.mask = lockedColumnMask;
                    lockedColumnMask.width = maskShape.width;
                    lockedColumnMask.height = maskShape.height;
                    lockedColumnMask.x = maskShape.x;
                    lockedColumnMask.y = maskShape.y;
                }
                if (lockedColumnAndRowContent)
                {
                    if (!lockedColumnAndRowMask)
                    {
                        lockedColumnAndRowMask = new Shape();
                        addChild(lockedColumnAndRowMask);
                        g = lockedColumnAndRowMask.graphics;
                        g.beginFill(0xFFFFFF);
                        g.drawRect(0, 0, 10, 10);
                        g.endFill();
                        lockedColumnAndRowMask.visible = false;
                    }
                    lockedColumnAndRowContent.mask = null;
                    lockedColumnAndRowContent.selectionLayer.mask = lockedColumnAndRowMask;
                    lockedColumnAndRowMask.width = maskShape.width;
                    lockedColumnAndRowMask.height = maskShape.height;
                    lockedColumnAndRowMask.x = maskShape.x;
                    lockedColumnAndRowMask.y = maskShape.y;
                }
                if (lockedColumnHeader)
                {
                    if (!lockedColumnHeaderMask)
                    {
                        lockedColumnHeaderMask = new Shape();
                        addChild(lockedColumnHeaderMask);
                        g = lockedColumnHeaderMask.graphics;
                        g.beginFill(0xFFFFFF);
                        g.drawRect(0, 0, 10, 10);
                        g.endFill();
                        lockedColumnHeaderMask.visible = false;
                    }
                    lockedColumnHeader.mask = null;
                    lockedColumnHeader.selectionLayer.mask = lockedColumnHeaderMask;
                    lockedColumnHeaderMask.width = maskShape.width;
                    lockedColumnHeaderMask.height = maskShape.height;
                    lockedColumnHeaderMask.x = maskShape.x;
                    lockedColumnHeaderMask.y = maskShape.y;
                }
            }
        }

        // If we've decided to clip the entire listContent, then stop here.
        // There's no need to clip individual items
        if (listContent.mask)
            return;

        // If the last row fits inside listContent, then stop here.  There's
        // no need to do any clipping.
        var lastRowIndex:int = listItems.length - 1;
        var lastRowInfo:ListRowInfo = rowInfo[lastRowIndex];
        var lastRowItems:Array = listItems[lastRowIndex];
        if (lastRowInfo.y + lastRowInfo.height <= listContent.height)
            return;

        // For each list item in the last row, either apply a clip mask or
        // set the row's height to not exceed the height of listContent
        var numColumns:int = lastRowItems.length;
        var rowY:Number = lastRowInfo.y;
        var rowWidth:Number = listContent.width;
        var rowHeight:Number = listContent.height - lastRowInfo.y;
        for (var i:int = 0; i < numColumns; i++)
        {
            var item:DisplayObject = lastRowItems[i];
            var yOffset:Number = item.y - rowY;
            if (item is UITextField)
                item.height = Math.max(rowHeight - yOffset, 0);
            else
                item.mask = createItemMask(0, rowY + yOffset, rowWidth, Math.max(rowHeight - yOffset, 0));
        }
    }

    /**
     *  @private
     */
    mx_internal function get gridColumnMap():Object
    {
        return columnMap;
    }

    /**
     *  @private
     */
    override public function itemRendererToIndex(itemRenderer:IListItemRenderer):int
    {
        if (itemRenderer.name in rowMap)
        {
            var index:int = rowMap[itemRenderer.name].rowIndex;
            
            if (itemRenderer.parent is DataGridLockedRowContentHolder)
                return index;

            // not clear why the commented out logic isn't correct...
            // maybe rowIndex isn't being set correctly?                   
            // return index + verticalScrollPosition + offscreenExtraRowsTop;
            return index + lockedRowCount + verticalScrollPosition;
        }
        return int.MIN_VALUE;
    }

    /**
     *  @private
     */
    override mx_internal function selectionTween_updateHandler(event:TweenEvent):void
    {
        super.selectionTween_updateHandler(event);
        if (lockedColumnCount)
        {
            var s:Sprite = Sprite(event.target.listener);
            s = indicatorDictionary[s] as Sprite;
            s.alpha = Number(event.value);
        }
    }

    /**
     *  @private
     */
    override protected function destroyRow(i:int, numCols:int):void
    {
        super.destroyRow(i, numCols);
        if (lockedColumnCount)
        {
            var listItems:Array = lockedColumnContent.listItems;
            numCols = listItems[i].length;
            var rowInfo:Array = lockedColumnContent.rowInfo;
            var visibleData:Object = lockedColumnContent.visibleData;

            var r:IListItemRenderer;
            var uid:String = rowInfo[i].uid;

            removeIndicators(uid);
            for (var j:int = 0; j < numCols; j++)
            {
                r = listItems[i][j];
                if (r.data)
                    delete visibleData[uid];
                addToFreeItemRenderers(r);
                // we don't seem to be doing this consistently throughout the code?
                // listContent.removeChild(DisplayObject(r));
            }
        }
    }

    /**
     *  @private
     */
    override protected function moveRowVertically(i:int, numCols:int, moveBlockDistance:Number):void
    {
        super.moveRowVertically(i, numCols, moveBlockDistance);
        if (lockedColumnCount)
        {
            var listItems:Array = lockedColumnContent.listItems;
            numCols = listItems[i].length;
            var rowInfo:Array = lockedColumnContent.rowInfo;
            var r:IListItemRenderer;

            for (var j:int = 0; j < numCols; j++)
            {
                r = listItems[i][j];
                r.move(r.x, r.y + moveBlockDistance);
            }
        }
    }

    /**
     *  @private
     */
    override protected function shiftRow(oldIndex:int, newIndex:int, numCols:int, shiftItems:Boolean):void
    {
        super.shiftRow(oldIndex, newIndex, numCols, shiftItems);
        if (lockedColumnCount)
        {
            var listItems:Array = lockedColumnContent.listItems;
            numCols = listItems[oldIndex].length;

            var r:IListItemRenderer;
            for (var j:int = 0; j < numCols; j++)
            {
                r = listItems[oldIndex][j];
                if (shiftItems)
                {
                    listItems[newIndex][j] = r;
                    rowMap[r.name].rowIndex = newIndex;

                }
                // this is sort of a hack to accomodate the fact that
                // scrolling down does a splice which throws off these values.
                // probably better to call shiftRow with different parameters?
                else
                    rowMap[r.name].rowIndex = oldIndex;
            }
            if (shiftItems)
                lockedColumnContent.rowInfo[newIndex] = lockedColumnContent.rowInfo[oldIndex];
        }
    }

    /**
     *  @private
     */
    override protected function moveIndicatorsVertically(uid:String, moveBlockDistance:Number):void
    {
        super.moveIndicatorsVertically(uid, moveBlockDistance);
        if (lockedColumnCount)
        {
            if (uid)
            {
                if (selectionIndicators[uid])
                    Sprite(indicatorDictionary[selectionIndicators[uid]]).y += moveBlockDistance;
                if (highlightUID == uid)
                    columnHighlightIndicator.y += moveBlockDistance;
                if (caretUID == uid)
                    columnCaretIndicator.y += moveBlockDistance;
            }
        }
    }

    /**
     *  @private
     */
    override protected function truncateRowArrays(numRows:int):void
    {
        super.truncateRowArrays(numRows);
        if (lockedColumnCount)
        {
            lockedColumnContent.listItems.splice(numRows);
            lockedColumnContent.rowInfo.splice(numRows);
        }
    }

    /**
     *  @private
     */
    override protected function addToRowArrays():void
    {
        super.addToRowArrays();

        if (lockedColumnCount)
        {
            lockedColumnContent.listItems.splice(0, 0, null);
            lockedColumnContent.rowInfo.splice(0, 0, null);
        }
    }

    /**
     *  @private
     */
    override protected function restoreRowArrays(modDeltaPos:int):void
    {
        super.restoreRowArrays(modDeltaPos);
        if (lockedColumnCount)
        {
            lockedColumnContent.listItems.splice(0, modDeltaPos);
            lockedColumnContent.rowInfo.splice(0, modDeltaPos);
        }
    }

    /**
     *  @private
     */
    override protected function removeFromRowArrays(i:int):void
    {
        super.removeFromRowArrays(i);
        if (lockedColumnCount)
        {
            lockedColumnContent.listItems.splice(i, 1);
            lockedColumnContent.rowInfo.splice(i, 1);
        }
    }

    /**
     *  @private
     */
    override protected function clearVisibleData():void
    {
        if (lockedColumnContent)
            lockedColumnContent.visibleData = {};
        if (lockedRowContent)
            lockedRowContent.visibleData = {};
        if (lockedColumnAndRowContent)
            lockedColumnAndRowContent.visibleData = {};

        super.clearVisibleData();
    }

    /**
     *  @private
     */
    override protected function set allowItemSizeChangeNotification(value:Boolean):void
    {
        if (lockedColumnContent)
            lockedColumnContent.allowItemSizeChangeNotification = value;
        if (lockedRowContent)
            lockedRowContent.allowItemSizeChangeNotification = value;
        if (lockedColumnAndRowContent)
            lockedColumnAndRowContent.allowItemSizeChangeNotification = value;

        super.allowItemSizeChangeNotification = value;
    }

    /**
     *  @private
     */
    override protected function indexToRow(index:int):int
    {
        if (index < lockedRowCount)
            return -1;
        return index - lockedRowCount;
    }

    /**
     *  Returns a Point containing the columnIndex and rowIndex of an
     *  item renderer.  Since item renderers are only created for items
     *  within the set of viewable rows
     *  you cannot use this method to get the indices for items
     *  that are not visible.  Also note that item renderers
     *  are recycled so the indices you get for an item may change
     *  if that item renderer is reused to display a different item.
     *  Usually, this method is called during mouse and keyboard handling
     *  when the set of data displayed by the item renderers hasn't yet
     *  changed.
     *
     *  @param item An item renderer
     *
     *  @return A Point.  The <code>x</code> property is the columnIndex
     *  and the <code>y</code> property is the rowIndex.
     */
    override protected function itemRendererToIndices(item:IListItemRenderer):Point
    {
        if (!item || !(item.name in rowMap))
            return null;
            
        var content:ListBaseContentHolder = item.parent as ListBaseContentHolder;

        var found:Boolean = false;
        var index:int = rowMap[item.name].rowIndex;
        var len:int = content.listItems[index].length;
        for (var i:int = 0; i < len; i++)
        {
            if (content.listItems[index][i] == item)
            {
                found = true;
                break;
            }
        }
        if (!found)
            return null;

        if (lockedRowContent == content)
            return new Point(i + horizontalScrollPosition + lockedColumnCount,
                                         index + offscreenExtraRowsTop);

        if (lockedColumnAndRowContent == content)
            return new Point(i, index + offscreenExtraRowsTop);

        if (lockedColumnContent == content)
            return new Point(i,
                             index + verticalScrollPosition + lockedRowCount + offscreenExtraRowsTop);

        return new Point(i + horizontalScrollPosition + lockedColumnCount,
                         index + verticalScrollPosition + lockedRowCount + offscreenExtraRowsTop);
    }

}

}
