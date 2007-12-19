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

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.GradientType;
import flash.display.Graphics;
import flash.display.Shape;
import flash.display.Sprite;
import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Matrix;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.ui.Keyboard;
import flash.utils.describeType;
import flash.utils.Dictionary;
import mx.collections.CursorBookmark;
import mx.collections.ICollectionView;
import mx.collections.ItemResponder;
import mx.collections.Sort;
import mx.collections.SortField;
import mx.collections.errors.ItemPendingError;
import mx.controls.dataGridClasses.DataGridBase;
import mx.controls.dataGridClasses.DataGridColumn;
import mx.controls.dataGridClasses.DataGridDragProxy;
import mx.controls.dataGridClasses.DataGridItemRenderer;
import mx.controls.dataGridClasses.DataGridListData;
import mx.controls.listClasses.IDropInListItemRenderer;
import mx.controls.listClasses.IListItemRenderer;
import mx.controls.listClasses.ListBaseSeekPending;
import mx.controls.listClasses.ListRowInfo;
import mx.controls.scrollClasses.ScrollBar;
import mx.core.ClassFactory;
import mx.core.EdgeMetrics;
import mx.core.EventPriority;
import mx.core.FlexShape;
import mx.core.FlexSprite;
import mx.core.IChildList;
import mx.core.IFactory;
import mx.core.IFlexDisplayObject;
import mx.core.IIMESupport;
import mx.core.IInvalidating;
import mx.core.IPropertyChangeNotifier;
import mx.core.IRawChildrenContainer;
import mx.core.IUIComponent;
import mx.core.ScrollPolicy;
import mx.core.UIComponent;
import mx.core.UIComponentGlobals;
import mx.core.mx_internal;
import mx.events.CollectionEvent;
import mx.events.CollectionEventKind;
import mx.events.ListEvent;
import mx.events.DataGridEvent;
import mx.events.DataGridEventReason;
import mx.events.DragEvent;
import mx.events.FlexEvent;
import mx.events.IndexChangedEvent;
import mx.events.ScrollEvent;
import mx.events.ScrollEventDetail;
import mx.managers.CursorManager;
import mx.managers.CursorManagerPriority;
import mx.managers.IFocusManager;
import mx.managers.IFocusManagerComponent;
import mx.skins.RectangularBorder;
import mx.skins.halo.DataGridColumnDropIndicator;
import mx.styles.ISimpleStyleClient;
import mx.styles.StyleManager;
import mx.utils.ObjectUtil;

use namespace mx_internal;

//--------------------------------------
//  Events
//--------------------------------------

/**
 *  Dispatched when the user releases the mouse button while over an item 
 *  renderer, tabs to the DataGrid control or within the DataGrid control, 
 *  or in any other way attempts to edit an item.
 *
 *  @eventType mx.events.DataGridEvent.ITEM_EDIT_BEGINNING
 */
[Event(name="itemEditBeginning", type="mx.events.DataGridEvent")]

/**
 *  Dispatched when the <code>editedItemPosition</code> property has been set
 *  and the item can be edited.
 *
 *  @eventType mx.events.DataGridEvent.ITEM_EDIT_BEGIN
 */
[Event(name="itemEditBegin", type="mx.events.DataGridEvent")]

/**
 *  Dispatched when an item editing session ends for any reason.
 *
 *  @eventType mx.events.DataGridEvent.ITEM_EDIT_END
 */
[Event(name="itemEditEnd", type="mx.events.DataGridEvent")]

/**
 *  Dispatched when an item renderer gets focus, which can occur if the user
 *  clicks on an item in the DataGrid control or navigates to the item using
 *  a keyboard.  Only dispatched if the item is editable.
 *
 *  @eventType mx.events.DataGridEvent.ITEM_FOCUS_IN
 */
[Event(name="itemFocusIn", type="mx.events.DataGridEvent")]

/**
 *  Dispatched when an item renderer loses focus, which can occur if the user
 *  clicks another item in the DataGrid control or clicks outside the control,
 *  or uses the keyboard to navigate to another item in the DataGrid control
 *  or outside the control.
 *  Only dispatched if the item is editable.
 *
 *  @eventType mx.events.DataGridEvent.ITEM_FOCUS_OUT
 */
[Event(name="itemFocusOut", type="mx.events.DataGridEvent")]

/**
 *  Dispatched when a user changes the width of a column, indicating that the 
 *  amount of data displayed in that column may have changed.
 *  If <code>horizontalScrollPolicy</code> is <code>"none"</code>, other
 *  columns shrink or expand to compensate for the columns' resizing,
 *  and they also dispatch this event.
 *
 *  @eventType mx.events.DataGridEvent.COLUMN_STRETCH
 */
[Event(name="columnStretch", type="mx.events.DataGridEvent")]

/**
 *  Dispatched when the user releases the mouse button on a column header
 *  to request the control to sort
 *  the grid contents based on the contents of the column.
 *  Only dispatched if the column is sortable and the data provider supports 
 *  sorting. The DataGrid control has a default handler for this event that implements
 *  a single-column sort.  Multiple-column sort can be implemented by calling the 
 *  <code>preventDefault()</code> method to prevent the single column sort and setting 
 *  the <code>sort</code> property of the data provider.
 * <p>
 * <b>Note</b>: The sort arrows are defined by the default event handler for
 * the headerRelease event. If you call the <code>preventDefault()</code> method
 * in your event handler, the arrows are not drawn.
 * </p>
 *
 *  @eventType mx.events.DataGridEvent.HEADER_RELEASE
 */
[Event(name="headerRelease", type="mx.events.DataGridEvent")]

/**
 *  Dispatched when the user releases the mouse button on a column header after 
 *  having dragged the column to a new location resulting in shifting the column
 *  to a new index
 *
 *  @eventType mx.events.IndexChangedEvent.HEADER_SHIFT
 */
[Event(name="headerShift", type="mx.events.IndexChangedEvent")]

//--------------------------------------
//  Styles
//--------------------------------------

/**
 *  A flag that indicates whether to show vertical grid lines between
 *  the columns.
 *  If <code>true</code>, shows vertical grid lines.
 *  If <code>false</code>, hides vertical grid lines.
 *  @default true
 */
[Style(name="verticalGridLines", type="Boolean", inherit="no")]

/**
 *  A flag that indicates whether to show horizontal grid lines between
 *  the rows.
 *  If <code>true</code>, shows horizontal grid lines.
 *  If <code>false</code>, hides horizontal grid lines.
 *  @default false
 */
[Style(name="horizontalGridLines", type="Boolean", inherit="no")]

/**
 *  The color of the vertical grid lines.
 *  @default 0x666666
 */
[Style(name="verticalGridLineColor", type="uint", format="Color", inherit="yes")]

/**
 *  The color of the horizontal grid lines.
  */
[Style(name="horizontalGridLineColor", type="uint", format="Color", inherit="yes")]

/**
 *  An array of two colors used to draw the header background gradient.
 *  The first color is the top color.
 *  The second color is the bottom color.
 *  @default [0xFFFFFF, 0xE6E6E6]
 */
[Style(name="headerColors", type="Array", arrayType="uint", format="Color", inherit="yes")]

/**
 *  The color of the row background when the user rolls over the row.
 *  @default 0xE3FFD6
 */
[Style(name="rollOverColor", type="uint", format="Color", inherit="yes")]

/**
 *  The color of the background for the row when the user selects 
 *  an item renderer in the row.
 *  @default 0xCDFFC1
 */
[Style(name="selectionColor", type="uint", format="Color", inherit="yes")]

/**
 *  The name of a CSS style declaration for controlling other aspects of
 *  the appearance of the column headers.
 *  @default "dataGridStyles"
 */
[Style(name="headerStyleName", type="String", inherit="no")]

/**
 *  The class to use as the skin for a column that is being resized.
 *  @default mx.skins.halo.DataGridColumnResizeSkin
 */
[Style(name="columnResizeSkin", type="Class", inherit="no")]

/**
 *  The skin that defines the appearance of the separators between 
 *  column headers in a DataGrid.
 *  @default mx.skins.halo.DataGridHeaderSeparator
 */
[Style(name="headerSeparatorSkin", type="Class", inherit="no")]

/**
 *  The class to use as the skin for the arrow that indicates the column sort 
 *  direction.
 *  @default mx.skins.halo.DataGridSortArrow
 */
[Style(name="sortArrowSkin", type="Class", inherit="no")]

/**
 *  The class to use as the skin for the cursor that indicates that a column
 *  can be resized.
 *  @default mx.skins.halo.DataGridStretchCursor
 */
[Style(name="stretchCursor", type="Class", inherit="no")]

/**
 *  The class to use as the skin that indicates that 
 *  a column can be dropped in the current location.
 *
 *  @default mx.skins.halo.DataGridColumnDropIndicator
 */
[Style(name="columnDropIndicatorSkin", type="Class", inherit="no")]

/**
 *  The name of a CSS style declaration for controlling aspects of the
 *  appearance of column when the user is dragging it to another location.
 *
 *  @default "headerDragProxyStyle"
 */
[Style(name="headerDragProxyStyleName", type="String", inherit="no")]

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="columnCount", kind="property")]
[Exclude(name="iconField", kind="property")]
[Exclude(name="iconFunction", kind="property")]
[Exclude(name="labelField", kind="property")]
[Exclude(name="maxHorizontalScrollPosition", kind="property")]
[Exclude(name="maxVerticalScrollPosition", kind="property")]
[Exclude(name="showDataTips", kind="property")]

//--------------------------------------
//  Other metadata
//--------------------------------------

[AccessibilityClass(implementation="mx.accessibility.DataGridAccImpl")]

[DataBindingInfo("acceptedTypes", "{ dataProvider: &quot;String&quot; }")]

[DefaultBindingProperty(source="selectedItem", destination="dataProvider")]

[DefaultProperty("dataProvider")]

[DefaultTriggerEvent("change")]

[IconFile("DataGrid.png")]

[RequiresDataBinding(true)]

/**
 *  The <code>DataGrid</code> control is like a List except that it can show 
 *  more than one column of data
 *  making it suited for showing objects with multiple properties.
 *  <p>
 *  The DataGrid control provides the following features:
 *  <ul>
 *  <li>Columns of different widths or identical fixed widths</li>
 *  <li>Columns that the user can resize at runtime </li>
 *  <li>Columns that the user can reorder at runtime </li>
 *  <li>Optional customizable column headers</li>
 *  <li>Ability to use a custom item renderer for any column to display data 
 *  other than text</li>
 *  <li>Support for sorting the data by clicking on a column</li>
 *  </ul>
 *  </p>
 *  The DataGrid control is intended for viewing data, and not as a
 *  layout tool like an HTML table.
 *  The mx.containers package provides those layout tools.
 *  
 *  @mxml
 *  <p>
 *  The <code>&lt;mx:DataGrid&gt;</code> tag inherits all of the tag attributes
 *  of its superclass, except for <code>labelField</code>, <code>iconField</code>,
 *  and <code>iconFunction</code>, and adds the following tag attributes:
 *  </p>
 *  <pre>
 *  &lt;mx:DataGrid
 *    <b>Properties</b>
 *    columns="<i>From dataProvider</i>"
 *    draggableColumns="true|false"
 *    editable="false|true"
 *    editedItemPosition="<code>null</code>"
 *    horizontalScrollPosition="null"
 *    imeMode="null"
 *    itemEditorInstance="null"
 *    minColumnWidth="<code>NaN</code>"
 *    resizableColumns="true|false"
 *    sortableColumns="true|false"
 *    &nbsp;
 *    <b>Styles</b>
 *    backgroundDisabledColor="0xEFEEEF"
 *    columnDropIndicatorSkin="DataGridColumnDropIndicator"
 *    columnResizeSkin="DataGridColumnResizeSkin"
 *    headerColors="[#FFFFFF, #E6E6E6]"
 *    headerDragProxyStyleName="headerDragProxyStyle"
 *    headerSeparatorSkin="DataGridHeaderSeparator"
 *    headerStyleName="<i>No default</i>"
 *    horizontalGridLineColor="<i>No default</i>"
 *    horizontalGridLines="false|true"
 *    rollOverColor="#E3FFD6"
 *    selectionColor="#CDFFC1"
 *    sortArrowSkin="DataGridSortArrow"
 *    stretchCursor="DataGridStretchCursor"
 *    verticalGridLineColor="#666666"
 *    verticalGridLines="false|true"
 *    &nbsp;
 *    <b>Events</b>
 *    columnStretch="<i>No default</i>"
 *    headerRelease="<i>No default</i>"
 *    headerShift="<i>No default</i>"
 *    itemEditBegin="<i>No default</i>"
 *    itemEditBeginning="<i>No default</i>" 
 *    itemEditEnd="<i>No default</i>"
 *    itemFocusIn="<i>No default</i>"
 *    itemFocusOut="<i>No default</i>"
 *  /&gt;
 *   
 *  <b><i>The following DataGrid code sample specifies the column order:</i></b>
 *  &lt;mx:DataGrid&gt;
 *    &lt;mx:dataProvider&gt;
 *        &lt;mx:Object Artist="Pavement" Price="11.99"
 *          Album="Slanted and Enchanted"/&gt;
 *        &lt;mx:Object Artist="Pavement"
 *          Album="Brighten the Corners" Price="11.99"/&gt;
 *    &lt;/mx:dataProvider&gt;
 *    &lt;mx:columns&gt;
 *        &lt;mx:DataGridColumn dataField="Album"/&gt;
 *        &lt;mx:DataGridColumn dataField="Price"/&gt;
 *    &lt;/mx:columns&gt;
 *  &lt;/mx:DataGrid&gt;
 *  </pre>
 *  </p>
 *
 *  @see mx.controls.dataGridClasses.DataGridItemRenderer
 *  @see mx.controls.dataGridClasses.DataGridColumn
 *  @see mx.controls.dataGridClasses.DataGridDragProxy
 *  @see mx.events.DataGridEvent
 *
 *  @includeExample examples/SimpleDataGrid.mxml
 */
public class DataGrid extends DataGridBase implements IIMESupport
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class mixins
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     *  Placeholder for mixin by DataGridAccImpl.
     */
    mx_internal static var createAccessibilityImplementation:Function;

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     */
    public function DataGrid()
    {
        super();

        _columns = [];

        itemRenderer = new ClassFactory(DataGridItemRenderer);

        // pick a default row height
        setRowHeight(20);

        // Register default handlers for item editing and sorting events.

        addEventListener(DataGridEvent.ITEM_EDIT_BEGINNING,
                        itemEditorItemEditBeginningHandler,
                        false, EventPriority.DEFAULT_HANDLER);

        addEventListener(DataGridEvent.ITEM_EDIT_BEGIN,
                         itemEditorItemEditBeginHandler,
                         false, EventPriority.DEFAULT_HANDLER);

        addEventListener(DataGridEvent.ITEM_EDIT_END,
                         itemEditorItemEditEndHandler,
                         false, EventPriority.DEFAULT_HANDLER);

        addEventListener(DataGridEvent.HEADER_RELEASE,
                         headerReleaseHandler,
                         false, EventPriority.DEFAULT_HANDLER);
                         
        addEventListener(MouseEvent.MOUSE_UP, mouseUpHandler);                         
    }

    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    /**
     *  The small arrow graphic used to show sortable columns and direction.
     */
    mx_internal var sortArrow:IFlexDisplayObject;

    [Inspectable(environment="none")]

    /**
     *  A reference to the currently active instance of the item editor, 
     *  if it exists.
     *
     *  <p>To access the item editor instance and the new item value when an 
     *  item is being edited, you use the <code>itemEditorInstance</code> 
     *  property. The <code>itemEditorInstance</code> property
     *  is not valid until after the event listener for
     *  the <code>itemEditBegin</code> event executes. Therefore, you typically
     *  only access the <code>itemEditorInstance</code> property from within 
     *  the event listener for the <code>itemEditEnd</code> event.</p>
     *
     *  <p>The <code>DataGridColumn.itemEditor</code> property defines the
     *  class of the item editor
     *  and, therefore, the data type of the item editor instance.</p>
     *
     *  <p>You do not set this property in MXML.</p>
     */
    public var itemEditorInstance:IListItemRenderer;

    /**
     *  A reference to the item renderer
     *  in the DataGrid control whose item is currently being edited.
     *
     *  <p>From within an event listener for the <code>itemEditBegin</code>
     *  and <code>itemEditEnd</code> events,
     *  you can access the current value of the item being edited
     *  using the <code>editedItemRenderer.data</code> property.</p>
     */
    public function get editedItemRenderer():IListItemRenderer
    {
        if (!itemEditorInstance) return null;

        return listItems[actualRowIndex][actualColIndex];
    }

    /**
     *  @private
     *  true if we want to block editing on mouseUp
     */
    private var dontEdit:Boolean = false;

    /**
     *  @private
     *  true if we want to block editing on mouseUp
     */
    private var losingFocus:Boolean = false;

    /**
     *  @private
     *  true if we're in the endEdit call.  Used to handle
     *  some timing issues with collection updates
     */
    private var inEndEdit:Boolean = false;

    /**
     *  @private
     *  true if we've disabled updates in the collection
     */
    private var collectionUpdatesDisabled:Boolean = false;

    /**
     *  Specifies a graphic that shows the proposed column width as the user stretches it.
     */
    private var resizeGraphic:IFlexDisplayObject; //

    /**
     *  @private
     *  A tmp var to store the stretching col's X coord.
     */
    private var startX:Number;

    /**
     *  @private
     *  A tmp var to store the stretching col's min X coord for column's minWidth.
     */
    private var minX:Number;

    /**
     *  @private
     *  List of header separators for column resizing.
     */
    private var separators:Array;

    /**
     *  @private
     *  The column that is being resized.
     */
    private var resizingColumn:DataGridColumn;

    /**
     *  @private
     *  The index of the column being sorted.
     */
    private var sortIndex:int = -1;

    /**
     *  @private
     *  The column being sorted.
     */
    private var sortColumn:DataGridColumn;

    /**
     *  @private
     *  The direction of the sort
     */
    private var sortDirection:String;

    /**
     *  @private
     *  The index of the last column being sorted on.
     */
    private var lastSortIndex:int = -1;

    /**
     *  @private
     */
    private var lastItemDown:IListItemRenderer;

    /**
     *  @private
     *  The column that is being moved.
     */
    private var movingColumn:DataGridColumn;

    /**
     *  @private
     *  Index of column before which to drop
     */
    private var dropColumnIndex:int = -1;

    /**
     *  @private
     */
    mx_internal var columnDropIndicator:IFlexDisplayObject;

    /**
     *  @private
     */
    private var displayWidth:Number;

    /**
     *  @private
     *  Additional affordance given to header separators.
     */
    private var separatorAffordance:Number = 3;

    /**
     *  @private
     *  Columns with visible="true"
     */
    private var displayableColumns:Array;

    /**
     *  @private
     *  Whether we have auto-generated the set of columns
     *  Defaults to true so we'll run the auto-generation at init time if needed
     */
    private var generatedColumns:Boolean = true;

    /**
     *  @private
     *  A hash table of objects used to calculate sizes
     */
    private var measuringObjects:Dictionary;

    /**
     *  @private
     */
    private var resizeCursorID:int = CursorManager.NO_CURSOR;

    // last known position of item editor instance
    private var actualRowIndex:int;
    private var actualColIndex:int;

    /**
     *  @private
     *  Flag to indicate whether sorting is manual or programmatic.  If it's
     *  not manual, we try to draw the sort arrow on the right column header.
     */
    private var manualSort:Boolean;

    //--------------------------------------------------------------------------
    //
    //  Overridden properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  baselinePosition
    //----------------------------------

    /**
     *  @private
     */
    override public function get baselinePosition():Number
    {
        var top:Number = 0;

        if (border && border is RectangularBorder)
            top = RectangularBorder(border).borderMetrics.top;

        return top + measureText(" ").ascent;
    }

    /**
     *  @private
     *  Number of columns that can be displayed.
     *  Some may be offscreen depending on horizontalScrollPolicy
     *  and the width of the DataGrid.
     */
    override  public function get columnCount():int
    {
        if (_columns)
            return _columns.length;
        else
            return 0;
    }

    //----------------------------------
    //  enabled
    //----------------------------------

    [Inspectable(category="General", enumeration="true,false", defaultValue="true")]

    /**
     *  @private
     */
    override public function set enabled(value:Boolean):void
    {
        super.enabled = value;

        if (itemEditorInstance)
            endEdit(DataGridEventReason.OTHER);

        invalidateDisplayList();
    }

    //----------------------------------
    //  horizontalScrollPosition
    //----------------------------------

    /**
     *  The offset into the content from the left edge. 
     *  This can be a pixel offset in some subclasses or some other metric 
     *  like the number of columns in a DataGrid control. 
     *
     *  The DataGrid scrolls by columns so the value of the 
     *  <code>horizontalScrollPosition</code> property is always
     *  in the range of 0 to the index of the columns
     *  that will make the last column visible.  This is different from the
     *  List control that scrolls by pixels.  The DataGrid control always aligns the left edge
     *  of a column with the left edge of the DataGrid control.
     */
    override public function set horizontalScrollPosition(value:Number):void
    {
        // if not init or no data;
        if (!initialized || listItems.length == 0)
        {
            super.horizontalScrollPosition = value;
            return;
        }

        var oldValue:int = super.horizontalScrollPosition;
        super.horizontalScrollPosition = value;

        // columns have variable width so we need to recalc scroll parms
        scrollAreaChanged = true;

        columnsInvalid = true;
        calculateColumnSizes();

        // we are going to get a full repaint so don't repaint now
        if (itemsSizeChanged)
            return;

        if (oldValue != value)
        {
            removeClipMask();

            var bookmark:CursorBookmark;

            if (iterator)
                bookmark = iterator.bookmark;

            clearIndicators();
            visibleData = {};
            //if we scrolled more than the number of scrollable columns
            makeRowsAndColumns(0, 0, listContent.width, listContent.height, 0, 0);

            if (iterator && bookmark)
                iterator.seek(bookmark, 0);

            invalidateDisplayList();

            addClipMask(false);
        }
    }

    //----------------------------------
    //  horizontalScrollPolicy
    //----------------------------------

    /**
     *  @private
     *  Accomodates ScrollPolicy.AUTO.
     *  Makes sure column widths stay in synch.
     *
     *  @param policy on, off, or auto
     */
    override public function set horizontalScrollPolicy(value:String):void
    {
        super.horizontalScrollPolicy = value;
        itemsSizeChanged = true;
        invalidateDisplayList();
    }

    //----------------------------------
    //  imeMode
    //----------------------------------

    /**
     *  @private
     */
    private var _imeMode:String = null;

    [Inspectable(environment="none")]

    /**
     *  Specifies the IME (input method editor) mode.
     *  The IME enables users to enter text in Chinese, Japanese, and Korean.
     *  Flex sets the specified IME mode when the control gets the focus,
     *  and sets it back to the previous value when the control loses the focus.
     *
     * <p>The flash.system.IMEConversionMode class defines constants for the
     *  valid values for this property.
     *  You can also specify <code>null</code> to specify no IME.</p>
     *
     *  @see flash.system.IMEConversionMode
     *
     *  @default null
     */
    public function get imeMode():String
    {
        return _imeMode;
    }

    /**
     *  @private
     */
    public function set imeMode(value:String):void
    {
        _imeMode = value;
    }

    //----------------------------------
    //  minColumnWidth
    //----------------------------------

    /**
     *  @private
     */
    private var _minColumnWidth:Number;

    /**
     *  @private
     */
    private var minColumnWidthInvalid:Boolean = false;

    [Inspectable(defaultValue="NaN")]

    /**
     *  The minimum width of the columns, in pixels.  If not NaN,
     *  the DataGrid control applies this value as the minimum width for
     *  all columns.  Otherwise, individual columns can have
     *  their own minimum widths.
     *  
     *  @default NaN
     */
    public function get minColumnWidth():Number
    {
        return _minColumnWidth;
    }

    /**
     *  @private
     */
    public function set minColumnWidth(value:Number):void
    {
        _minColumnWidth = value;
        minColumnWidthInvalid = true;
        itemsSizeChanged = true;
        columnsInvalid = true;
        invalidateDisplayList();
    }

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
     */
    private var _columns:Array; // the array of our DataGridColumns

    [Bindable("columnsChanged")]
    [Inspectable(arrayType="mx.controls.dataGridClasses.DataGridColumn")]

    /**
     *  An array of DataGridColumn objects, one for each column that
     *  can be displayed.  If not explicitly set, the DataGrid control 
     *  attempts to examine the first data provider item to determine the
     *  set of properties and display those properties in alphabetic
     *  order.
     *
     *  <p>If you want to change the set of columns, you must get this array,
     *  make modifications to the columns and order of columns in the array,
     *  and then assign the new array to the columns property.  This is because
     *  the DataGrid control returned a new copy of the array of columns and therefore
     *  did not notice the changes.</p>
     */
    public function get columns():Array
    {
        return _columns.slice(0);
    }

    /**
     *  @private
     */
    public function set columns(value:Array):void
    {
        var n:int;
        var i:int;

        freeItemRenderersTable = new Dictionary(false);
        columnMap = {};

        n = _columns.length;
        for (i = 0; i < n; i++)
        {
            columnRendererChanged(_columns[i]);
        }
        
        _columns = value.slice(0);
        columnsInvalid = true;
        generatedColumns = false;

        n = value.length;
        for (i = 0; i < n; i++)
        {
            var column:DataGridColumn = _columns[i];
            column.owner = this;
            column.colNum = i;
        }

        updateSortIndexAndDirection();

        itemsSizeChanged = true;
        invalidateDisplayList();
        dispatchEvent(new Event("columnsChanged"));
    }

    //----------------------------------
    //  draggableColumns
    //----------------------------------

    /**
     *  @private
     *  Storage for the draggableColumns property.
     */
    private var _draggableColumns:Boolean = true;

    [Inspectable(defaultValue="true")]

    /**
     *  A flag that indicates whether the user is allowed to reorder columns.
     *  If <code>true</code>, the user can reorder the columns
     *  of the DataGrid control by dragging the header cells.
     *
     *  @default true
     */
    public function get draggableColumns():Boolean
    {
        return _draggableColumns;
    }
    
    /**
     *  @private
     */
    public function set draggableColumns(value:Boolean):void
    {
        _draggableColumns = value;
    }

    //----------------------------------
    //  editable
    //----------------------------------

    [Inspectable(category="General")]

    /**
     *  A flag that indicates whether or not the user can edit
     *  items in the data provider.
     *  If <code>true</code>, the item renderers in the control are editable.
     *  The user can click on an item renderer to open an editor.
     *
     *  <p>You can turn off editing for individual columns of the
     *  DataGrid control using the <code>DataGridColumn.editable</code> property,
     *  or by handling the <code>itemEditBeginning</code> and
     *  <code>itemEditBegin</code> events</p>
     *
     *  @default false
     */
    public var editable:Boolean = false;

    //----------------------------------
    //  editedItemPosition
    //----------------------------------

    /**
     *  @private
     */
    private var bEditedItemPositionChanged:Boolean = false;

    /**
     *  @private
     *  undefined means we've processed it
     *  null means don't put up an editor
     *  {} is the coordinates for the editor
     */
    private var _proposedEditedItemPosition:*;

    /**
     *  @private
     *  the last editedItemPosition.  We restore editing
     *  to this point if we get focus from the TAB key
     */
    private var lastEditedItemPosition:*;

    /**
     *  @private
     */
    private var _editedItemPosition:Object;

    [Bindable("itemFocusIn")]

    /**
     *  The column and row index of the item renderer for the
     *  data provider item being edited, if any.
     *
     *  <p>This Object has two fields, <code>columnIndex</code> and 
     *  <code>rowIndex</code>,
     *  the zero-based column and row indexes of the item.
     *  For example: {columnIndex:2, rowIndex:3}</p>
     *
     *  <p>Setting this property scrolls the item into view and
     *  dispatches the <code>itemEditBegin</code> event to
     *  open an item editor on the specified item renderer.</p>
     *
     *  @default null
     */
    public function get editedItemPosition():Object
    {
        if (_editedItemPosition)
            return {rowIndex: _editedItemPosition.rowIndex,
                columnIndex: _editedItemPosition.columnIndex};
        else
            return _editedItemPosition;
    }

    /**
     *  @private
     */
    public function set editedItemPosition(value:Object):void
    {
        var newValue:Object = {rowIndex: value.rowIndex,
            columnIndex: value.columnIndex};

        setEditedItemPosition(newValue);
    }

    //----------------------------------
    //  resizableColumns
    //----------------------------------

    [Inspectable(category="General")]

    /**
     *  A flag that indicates whether the user can change the size of the
     *  columns.
     *  If <code>true</code>, the user can stretch or shrink the columns of 
     *  the DataGrid control by dragging the grid lines between the header cells.
     *  If <code>true</code>, individual columns must also have their 
     *  <code>resizeable</code> properties set to <code>false</code> to 
     *  prevent the user from resizing a particular column.  
     *
     *  @default true
     */
    public var resizableColumns:Boolean = true;

    //----------------------------------
    //  sortableColumns
    //----------------------------------

    [Inspectable(category="General")]

    /**
     *  A flag that indicates whether the user can sort the data provider items
     *  by clicking on a column header cell.
     *  If <code>true</code>, the user can sort the data provider items by
     *  clicking on a column header cell. 
     *  The <code>DataGridColumn.dataField</code> property of the column
     *  or the <code>DataGridColumn.sortCompareFunction</code> property 
     *  of the column is used as the sort field.  
     *  If a column is clicked more than once
     *  the sort alternates between ascending and descending order.
     *  If <code>true</code>, individual columns can be made to not respond
     *  to a click on a header by setting the column's <code>sortable</code>
     *  property to <code>false</code>.
     *
     *  <p>When a user releases the mouse button over a header cell, the DataGrid
     *  control dispatches a <code>headerRelease</code> event if both
     *  this property and the column's sortable property are <code>true</code>.  
     *  If no handler calls the <code>preventDefault()</code> method on the event, the 
     *  DataGrid sorts using that column's <code>DataGridColumn.dataField</code> or  
     *  <code>DataGridColumn.sortCompareFunction</code> properties.</p>
     * 
     *  @default true
     *
     *  @see mx.controls.dataGridClasses.DataGridColumn#dataField
     *  @see mx.controls.dataGridClasses.DataGridColumn#sortCompareFunction
     */
    public var sortableColumns:Boolean = true;

    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

    [Inspectable(category="Data", defaultValue="undefined")]

    /**
     *  @private
     */
    override public function set dataProvider(value:Object):void
    {
        if (itemEditorInstance)
            endEdit(DataGridEventReason.OTHER);

        super.dataProvider = value;
    }

    /**
     *  @private
     */
    override protected function initializeAccessibility():void
    {
        if (DataGrid.createAccessibilityImplementation != null)
            DataGrid.createAccessibilityImplementation(this);
    }

    /**
     *  @private
     *  Measures the DataGrid based on its contents,
     *  summing the total of the visible column widths.
     */
    override protected function measure():void
    {
        super.measure();

        var o:EdgeMetrics = viewMetrics;

        var n:int = columns.length;
        if (n == 0)
        {
            measuredWidth = DEFAULT_MEASURED_WIDTH;
            measuredMinWidth = DEFAULT_MEASURED_MIN_WIDTH;
            return;
        }

        var columnWidths:Number = 0;
        var columnMinWidths:Number = 0;
        for (var i:int = 0; i < n; i++)
        {
            if (columns[i].visible)
            {
                columnWidths += columns[i].preferredWidth;
                if (isNaN(_minColumnWidth))
                    columnMinWidths += columns[i].minWidth;
            }
        }

        if (!isNaN(_minColumnWidth))
            columnMinWidths = n * _minColumnWidth;

        measuredWidth = columnWidths + o.left + o.right;
        measuredMinWidth = columnMinWidths + o.left + o.right;

        // factor out scrollbars if policy == AUTO.  See Container.viewMetrics
        if (verticalScrollPolicy == ScrollPolicy.AUTO &&
            verticalScrollBar)
        {
            measuredWidth -= verticalScrollBar.minWidth;
            measuredMinWidth -= verticalScrollBar.minWidth;
        }
        if (horizontalScrollPolicy == ScrollPolicy.AUTO &&
            horizontalScrollBar)
        {
            measuredHeight -= horizontalScrollBar.minHeight;
            measuredMinHeight -= horizontalScrollBar.minHeight;
        }

    }

    /**
     *  @private
     *  Sizes and positions the column headers, columns, and items based on the
     *  size of the DataGrid.
     */
    override protected function updateDisplayList(unscaledWidth:Number,
                                                  unscaledHeight:Number):void
    {
        // Note: We can't immediately call super.updateDisplayList()
        // because the visibleColumns array must be populated first.

        // trace(">>updateDisplayList");

        if (displayWidth != unscaledWidth - viewMetrics.right - viewMetrics.left)
        {
            displayWidth = unscaledWidth - viewMetrics.right - viewMetrics.left;
            columnsInvalid = true;
        }

        calculateColumnSizes();

        super.updateDisplayList(unscaledWidth, unscaledHeight);

        if (collection && collection.length)
        {
            setRowCount(listItems.length);

            if (listItems.length)
                setColumnCount(listItems[0].length);
            else
                setColumnCount(0);
        }

        if (_horizontalScrollPolicy == ScrollPolicy.OFF)
        {
            // If we have a vScroll only, we want the scrollbar to be below
            // the header.
            if (verticalScrollBar != null && headerVisible)
            {
                var hh:Number = rowInfo.length ? rowInfo[0].height : headerHeight;
                verticalScrollBar.move(verticalScrollBar.x, viewMetrics.top + hh);
                verticalScrollBar.setActualSize(
                    verticalScrollBar.width,
                    unscaledHeight - viewMetrics.top - viewMetrics.bottom - hh);
                verticalScrollBar.visible =
                    (verticalScrollBar.height >= verticalScrollBar.minHeight);
            }
        }

        if (bEditedItemPositionChanged)
        {
            bEditedItemPositionChanged = false;
            commitEditedItemPosition(_proposedEditedItemPosition);
            _proposedEditedItemPosition = undefined;
            invalidateDisplayList(); // remove this soon
        }

        var headerBG:UIComponent =
            UIComponent(listContent.getChildByName("headerBG"));
        
        if (!headerBG)
        {
            headerBG = new UIComponent();
            headerBG.name = "headerBG";
            listContent.addChildAt(headerBG, listContent.getChildIndex(selectionLayer));
        }

        if (headerVisible)
        {
            headerBG.visible = true;
            drawHeaderBackground(headerBG);
        }
        else
            headerBG.visible = false;

        drawRowBackgrounds();

		drawLinesAndColumnBackgrounds();

        placeSortArrow();

        if (headerVisible)
            drawSeparators();
        else
            clearSeparators();

        // trace("<<updateDisplayList");
    }

    /**
     *  @private
     */
    override protected function makeRowsAndColumns(left:Number, top:Number,
                                                right:Number, bottom:Number,
                                                firstCol:int, firstRow:int,
                                                byCount:Boolean = false, rowsNeeded:uint = 0):Point
    {
		if (left == right)
			return new Point(0,0);

        listContent.allowItemSizeChangeNotification = false;

        if (headerVisible && itemsSizeChanged)
            calculateHeaderHeight();

        var pt:Point = super.makeRowsAndColumns(left, top, right, bottom,
                                                firstCol, firstRow, byCount, rowsNeeded);
        if (itemEditorInstance)
        {
            listContent.setChildIndex(DisplayObject(itemEditorInstance),
                                      listContent.numChildren - 1);
            var col:DataGridColumn = visibleColumns[actualColIndex];
            var item:IListItemRenderer = listItems[actualRowIndex][actualColIndex];
            var rowData:ListRowInfo = rowInfo[actualRowIndex];
            if (item && !col.rendererIsEditor)
            {
                var dx:Number = col.editorXOffset;
                var dy:Number = col.editorYOffset;
                var dw:Number = col.editorWidthOffset;
                var dh:Number = col.editorHeightOffset;
                itemEditorInstance.move(item.x + dx, rowData.y + dy);
                itemEditorInstance.setActualSize(Math.min(col.width + dw, listContent.width - listContent.x - itemEditorInstance.x),
                                         Math.min(rowData.height + dh, listContent.height - listContent.y - itemEditorInstance.y));
                item.visible = false;

            }
        }

        var lines:Sprite = Sprite(listContent.getChildByName("lines"));
        if (lines)
            listContent.setChildIndex(lines, listContent.numChildren - 1);

        listContent.allowItemSizeChangeNotification = variableRowHeight;
        return pt;
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
            {
                if (iterator && columns.length > 0)
                {
                    if (!measuringObjects)
                        measuringObjects = new Dictionary(false);

                    //set DataGridBase.visibleColumns to the set of 
                    //all columns
                    visibleColumns = columns;

                    var paddingTop:Number = getStyle("paddingTop");
                    var paddingBottom:Number = getStyle("paddingBottom");

                    var data:Object = iterator.current;
                    var item:IListItemRenderer;
                    var c:DataGridColumn;
                    var ch:Number = 0;
                    var n:int = columns.length;
                    for (var j:int = 0; j < n; j++)
                    {
                        c = columns[j];

                        if (!c.visible)
                            continue;

                        item = getMeasuringRenderer(c, false);
                        setupRendererFromData(c, item, data);
                        ch = Math.max(ch, item.getExplicitOrMeasuredHeight() + paddingBottom + paddingTop);
                    }

                    // unless specified otherwise, rowheight defaults to 20
                    setRowHeight(Math.max(ch, 20));
                }
                else
                    setRowHeight(20);
            }
        }
    }

    /**
     *  @private
     *  Instead of measuring the items, we measure the visible columns instead.
     */
    override public function measureWidthOfItems(index:int = -1, count:int = 0):Number
    {
        var w:Number = 0;

        var n:int = columns ? columns.length : 0;
        for (var i:int = 0; i < n; i++)
        {
            if (columns[i].visible)
                w += columns[i].width;
        }

        return w;
    }

    /**
     *  Get the appropriate renderer factory for a column, 
     *  using the default renderer if none specified
     */
    mx_internal function columnItemRendererFactory(c:DataGridColumn, forHeader:Boolean):IFactory
    {
        if (forHeader)
        {
            if (c.headerRenderer)
                return c.headerRenderer;
        }
        else
        {
            if (c.itemRenderer)
                return c.itemRenderer;
        }
        return itemRenderer;
    }

    /**
     *  @private
     */
    mx_internal function getMeasuringRenderer(c:DataGridColumn, forHeader:Boolean):IListItemRenderer
    {
        var factory:IFactory = columnItemRendererFactory(c,forHeader);
        
        var item:IListItemRenderer = measuringObjects[factory];
        if (!item)
        {
            item = columnItemRenderer(c, forHeader);
            item.visible = false;
            item.styleName = c;
            listContent.addChild(DisplayObject(item));
            measuringObjects[factory] = item;
        }

        return item;
    }

    mx_internal function setupRendererFromData(c:DataGridColumn, item:IListItemRenderer, data:Object):void
    {
        var rowData:DataGridListData = DataGridListData(makeListData(data, itemToUID(data), 0, c.colNum, c));
        if (item is IDropInListItemRenderer)
            IDropInListItemRenderer(item).listData = data ? rowData : null;
        item.data = data;
        item.explicitWidth = getWidthOfItem(item, c);
        UIComponentGlobals.layoutManager.validateClient(item, true);
    }

    /**
     *  @private
     */
    override public function measureHeightOfItems(index:int = -1, count:int = 0):Number
    {
        return measureHeightOfItemsUptoMaxHeight(index, count);
    }

    /**
     *  @private
     */
    mx_internal function measureHeightOfItemsUptoMaxHeight(index:int = -1, count:int = 0, maxHeight:Number = -1):Number
    {
        if (!columns.length)
            return rowHeight * count;

        var h:Number = 0;

        var item:IListItemRenderer;
        var c:DataGridColumn;
        var ch:Number = 0;
        var n:int;
        var j:int;

        var paddingTop:Number = getStyle("paddingTop");
        var paddingBottom:Number = getStyle("paddingBottom");

        if (!measuringObjects)
            measuringObjects = new Dictionary(false);

        var lockedCount:int = lockedRowCount;

        if (headerVisible && count > 0 && lockedCount > 0 && index == -1)
        {
            h = calculateHeaderHeight();

            if (maxHeight != -1 && h > maxHeight)
            {
                setRowCount(0);
                return 0;
            }

            // trace(this + " header preferredHeight = " + h);
            count --;
            lockedCount--;
        }

        var bookmark:CursorBookmark = (iterator) ? iterator.bookmark : null;

        var bMore:Boolean = iterator != null;
        if (index != -1 && iterator)
        {
            try
            {
                iterator.seek(CursorBookmark.FIRST, index);
            }
            catch (e:ItemPendingError)
            {
                bMore = false;
            }
        }

        if (lockedCount > 0)
        {
            try
            {
                collectionIterator.seek(CursorBookmark.FIRST,0);
            }
            catch (e:ItemPendingError)
            {
                bMore = false;
            }
        }

        for (var i:int = 0; i < count; i++)
        {
            var data:Object;
            if (bMore)
            {
                data = (lockedCount > 0) ? collectionIterator.current : iterator.current;
                ch = 0;
                n = columns.length;
                for (j = 0; j < n; j++)
                {
                    c = columns[j];

                    if (!c.visible)
                        continue;

                    item = getMeasuringRenderer(c, false);
                    setupRendererFromData(c, item, data);
                    ch = Math.max(ch, variableRowHeight ? item.getExplicitOrMeasuredHeight() + paddingBottom + paddingTop : rowHeight);
                }
            }

            if (maxHeight != -1 && (h + ch > maxHeight || !bMore))
            {
                try
                {
                    iterator.seek(bookmark, 0);
                }
                catch (e:ItemPendingError)
                {
                    // we don't recover here since we'd only get here if the first seek failed.
                }
                count = (headerVisible) ? i + 1 : i;
                setRowCount(count);
                return h;
            }

            h += ch;
            if (iterator)
            {
                try
                {
                    bMore = iterator.moveNext();
                    if (lockedCount > 0)
                    {
                        collectionIterator.moveNext();
                        lockedCount--;
                    }
                }
                catch (e:ItemPendingError)
                {
                    // if we run out of data, assume all remaining rows are the size of the previous row
                    bMore = false;
                }
            }
        }

        if (iterator)
        {
            try
            {
                iterator.seek(bookmark, 0);
            }
            catch (e:ItemPendingError)
            {
                // we don't recover here since we'd only get here if the first seek failed.
            }
        }

        // trace("calcheight = " + h);
        return h;
    }

    /**
     *  @private
     */
    private function calculateHeaderHeight():Number
    {
        if (!columns.length)
            return rowHeight;

        var item:IListItemRenderer;
        var c:DataGridColumn;
        var rowData:DataGridListData;
        var ch:Number = 0;
        var n:int;
        var j:int;

        var paddingTop:Number = getStyle("paddingTop");
        var paddingBottom:Number = getStyle("paddingBottom");

        if (!measuringObjects)
            measuringObjects = new Dictionary(false);

        if (headerVisible)
        {
            ch = 0;
            n = columns.length;

            if (_headerWordWrapPresent)
            {
                _headerHeight = _originalHeaderHeight;
                _explicitHeaderHeight = _originalExplicitHeaderHeight;
            }

            for (j = 0; j < n; j++)
            {
                c = columns[j];

                if (!c.visible)
                    continue;

                item = getMeasuringRenderer(c, true);
                rowData = DataGridListData(makeListData(c, uid, 0, c.colNum, c));
                rowMap[item.name] = rowData;
                if (item is IDropInListItemRenderer)
                    IDropInListItemRenderer(item).listData = rowData;
                item.data = c;
                item.explicitWidth = c.width;
                UIComponentGlobals.layoutManager.validateClient(item, true);
                ch = Math.max(ch, _explicitHeaderHeight ? headerHeight : item.getExplicitOrMeasuredHeight() + paddingBottom + paddingTop);

                if (columnHeaderWordWrap(c))
                    _headerWordWrapPresent = true;
            }

            if (_headerWordWrapPresent)
            {
                // take backups
                _originalHeaderHeight = _headerHeight;
                _originalExplicitHeaderHeight = _explicitHeaderHeight;

                headerHeight = ch;
            }
        }
        return ch;
    }

    private var _headerWordWrapPresent:Boolean = false;
    private var _originalExplicitHeaderHeight:Boolean = false;
    private var _originalHeaderHeight:Number = 0;

    /**
     *  @private
     */
    override protected function calculateRowHeight(data:Object, hh:Number, skipVisible:Boolean = false):Number
    {
        var item:IListItemRenderer;
        var c:DataGridColumn;

        var n:int = columns.length;
        var j:int;
        var k:int = 0;

        if (skipVisible && visibleColumns.length == _columns.length)
            return hh;

        var paddingTop:Number = getStyle("paddingTop");
        var paddingBottom:Number = getStyle("paddingBottom");

        if (!measuringObjects)
            measuringObjects = new Dictionary(false);

        for (j = 0; j < n; j++)
        {
            // skip any columns that are visible
            if (skipVisible && k < visibleColumns.length && visibleColumns[k].colNum == columns[j].colNum)
            {
                k++;
                continue;
            }
            c = columns[j];

            if (!c.visible)
                continue;

            item = getMeasuringRenderer(c, false);
            setupRendererFromData(c, item, data);
            hh = Math.max(hh, item.getExplicitOrMeasuredHeight() + paddingBottom + paddingTop);
        }
        return hh;
    }

    /**
     *  @private
     */
    override protected function scrollHandler(event:Event):void
    {
        if (event.target == verticalScrollBar ||
            event.target == horizontalScrollBar)
        {
            // TextField.scroll bubbles so you might see it here
            if (event is ScrollEvent)
            {
                if (!liveScrolling &&
                    ScrollEvent(event).detail == ScrollEventDetail.THUMB_TRACK)
                {
                    return;
                }

                if (itemEditorInstance)
                    endEdit(DataGridEventReason.OTHER);

                var scrollBar:ScrollBar = ScrollBar(event.target);
                var pos:Number = scrollBar.scrollPosition;

                if (scrollBar == verticalScrollBar)
                    verticalScrollPosition = pos;
                else if (scrollBar == horizontalScrollBar)
                    horizontalScrollPosition = pos;

                super.scrollHandler(event);
            }
        }
    }

    /**
     *  @private
     */
    override protected function configureScrollBars():void
    {
        var oldHorizontalScrollBar:Object = horizontalScrollBar;
        var oldVerticalScrollBar:Object = verticalScrollBar;

        var rowCount:int = listItems.length;
        if (rowCount == 0)
        {
            // Get rid of any existing scrollbars.
            if (oldHorizontalScrollBar || oldVerticalScrollBar)
                setScrollBarProperties(0, 0, 0, 0);

            return;
        }

        // partial last rows don't count
        if (rowCount > (headerVisible ? 2 : 1) && rowInfo[rowCount - 1].y + rowInfo[rowCount - 1].height > listContent.height)
            rowCount--;

        // offset, when added to rowCount, is the index of the dataProvider
        // item for that row.  IOW, row 10 in listItems is showing dataProvider
        // item 10 + verticalScrollPosition - lockedRowCount;
        var offset:int = verticalScrollPosition - lockedRowCount;
        // don't count filler rows at the bottom either.
        var fillerRows:int = 0;
        while (rowCount && listItems[rowCount - 1].length == 0)
        {
            // as long as we're past the end of the collection, add up
            // fillerRows
            if (collection && rowCount + offset >= collection.length)
            {
                rowCount--;
                ++fillerRows;
            }
            else
                break;
        }

        // we have to scroll up.  We can't have filler rows unless the scrollPosition is 0
        if (verticalScrollPosition > 0 && fillerRows > 0)
        {
            if (adjustVerticalScrollPositionDownward(Math.max(rowCount, 1)))
                return;
        }

        var colCount:int = listItems.length ? listItems[0].length : visibleColumns.length;

        // if the last column is visible and partially offscreen (but it isn't the only
        // column) then adjust the column count so we can scroll to see it
        if (colCount > 1 && visibleColumns[colCount - 1] == displayableColumns[displayableColumns.length - 1]
            && listItems[0][colCount - 1].x + visibleColumns[colCount - 1].width > listContent.width)
            colCount--;

        var headerShift:int = headerVisible ? 1 : 0;

        // trace("configureSB", verticalScrollPosition);

        setScrollBarProperties(displayableColumns.length - lockedColumnCount, Math.max(colCount - lockedColumnCount, 1),
                            collection ? collection.length - lockedRowCount + headerShift: 0,
                            Math.max(rowCount - lockedRowCount, 1));

    }

    /**
     *  @private
     *  Makes verticalScrollPosition smaller until it is 0 or there
     *  are no empty rows.  This is needed if we're scrolled to the
     *  bottom and something is deleted or the rows resize so more
     *  rows can be shown.
     */
    private function adjustVerticalScrollPositionDownward(rowCount:int):Boolean
    {
        var bookmark:CursorBookmark = iterator.bookmark;

        // add up how much space we're currently taking with valid items
        var h:Number = 0;

        var item:IListItemRenderer;
        var c:DataGridColumn;
        var ch:Number = 0;
        var n:int;
        var j:int;

        var paddingTop:Number = getStyle("paddingTop");
        var paddingBottom:Number = getStyle("paddingBottom");

        h = rowInfo[rowCount - 1].y + rowInfo[rowCount - 1].height;
        h = listContent.height - h;

        // back up one
        var numRows:int = 0;
        try
        {
            if (iterator.afterLast)
                iterator.seek(CursorBookmark.LAST, 0)
            else
                var bMore:Boolean = iterator.movePrevious();
        }
        catch (e:ItemPendingError)
        {
            bMore = false;
        }
        if (!bMore)
        {
            // reset to 0;
            super.verticalScrollPosition = 0;
            try
            {
                iterator.seek(CursorBookmark.FIRST, 0);
                // sometimes, if the iterator is invalid we'll get lucky and succeed
                // here, then we have to make the iterator valid again
                if (!iteratorValid)
                {
                    iteratorValid = true;
                    lastSeekPending = null;
                }
            }
            catch (e:ItemPendingError)
            {
                lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, 0);
                e.addResponder(new ItemResponder(seekPendingResultHandler, seekPendingFailureHandler,
                                                lastSeekPending));
                iteratorValid = false;
                invalidateList();
                return true;
            }
            updateList();
            return true;
        }
    
        // now work backwards to see how many more rows we need to create
        while (h > 0 && bMore)
        {
            var data:Object;
            if (bMore)
            {
                data = iterator.current;
                ch = 0;
                n = columns.length;
                for (j = 0; j < n; j++)
                {
                    c = columns[j];

                    if (!c.visible)
                        continue;

                    if (variableRowHeight)
                    {
                        item = getMeasuringRenderer(c, false);
                        setupRendererFromData(c, item, data);
                    }
                    ch = Math.max(ch, variableRowHeight ? item.getExplicitOrMeasuredHeight() + paddingBottom + paddingTop : rowHeight);
                }
            }
            h -= ch;
            try
            {
                bMore = iterator.movePrevious();
                numRows++;
            }
            catch (e:ItemPendingError)
            {
                // if we run out of data, assume all remaining rows are the size of the previous row
                bMore = false;
            }
        }
        // if we overrun, go back one.
        if (h < 0)
        {
            numRows--;
        }

        iterator.seek(bookmark, 0);
        verticalScrollPosition = Math.max(0, verticalScrollPosition - numRows);

        // make sure we get through configureScrollBars w/o coming in here.
        if (numRows > 0 && !variableRowHeight)
            configureScrollBars();

        return (numRows > 0);
    }

    /**
     *  @private
     */
    override protected function scrollVertically(pos:int, deltaPos:int, scrollUp:Boolean):void
    {
        // temporarily shift the cursor index to first movable row.
        var headerShift:int = headerVisible ? 1 : 0;
        iterator.seek(CursorBookmark.CURRENT, lockedRowCount - headerShift);

        super.scrollVertically(pos, deltaPos, scrollUp);

        // move the cursor back to actual first row.
        iterator.seek(CursorBookmark.CURRENT, - lockedRowCount + headerShift);

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

            var n:int = listItems.length;
            for (var i:int = headerVisible ? 1 : 0; i < n; i++)
            {
                if (rowInfo[i].y <= pt.y && pt.y <= rowInfo[i].y + rowInfo[i].height)
                {
                    item = listItems[i][0];
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
    override protected function calculateDropIndicatorY(rowCount:Number, rowNum:int):Number
    {
        if (headerVisible && rowNum < listItems.length - 1)
            rowNum++;
        
        return super.calculateDropIndicatorY(rowCount, rowNum);
    }

    /**
     *  @private
     */
    override protected function drawRowBackgrounds():void
    {
        var rowBGs:Sprite = Sprite(listContent.getChildByName("rowBGs"));
        if (!rowBGs)
        {
            rowBGs = new FlexSprite();
            rowBGs.mouseEnabled = false;
            rowBGs.name = "rowBGs";
            listContent.addChildAt(rowBGs, 0);
        }

        var colors:Array;

        colors = getStyle("alternatingItemColors");

        if (!colors || colors.length == 0)
            return;

        StyleManager.getColorNames(colors);

        var curRow:int = 0;
        if (headerVisible)
            curRow++;

        var i:int = 0;
        var actualRow:int = verticalScrollPosition;
        var n:int = listItems.length;

        while (curRow < n)
        {
            drawRowBackground(rowBGs, i++, rowInfo[curRow].y, rowInfo[curRow].height, colors[actualRow % colors.length], actualRow);
            curRow++;
            actualRow++;
        }

        while (rowBGs.numChildren > i)
        {
            rowBGs.removeChildAt(rowBGs.numChildren - 1);
        }
    }

    /**
     *  @private
     */
    override protected function mouseEventToItemRenderer(event:MouseEvent):IListItemRenderer
    {
        var r:IListItemRenderer;

        if (event.target == highlightIndicator || event.target == listContent)
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
                        var m:int = listItems[i].length;
                        if (m == 1)
                        {
                            r = listItems[i][0];
                            break;
                        }
                        
                        var ww:Number = 0;
                        for (var j:int = 0; j < m; j++)
                        {
                            ww += visibleColumns[j].width;
                            if (pt.x < ww)
                            {
                                r = listItems[i][j];
                                break;
                            }

                        }
                        if (r)
                            break;
                    }
                }
                yy += rowInfo[i].height;
            }
        }

        if (!r)
            r = super.mouseEventToItemRenderer(event);

        return r == itemEditorInstance ? null : r;
    }

    /**
     *  @private
     */
    override protected function get dragImage():IUIComponent
    {
        var image:DataGridDragProxy = new DataGridDragProxy();
        image.owner = this;
        return image;
    }

    //--------------------------------------------------------------------------
    //
    //  Methods
    //
    //--------------------------------------------------------------------------


    /**
     *  @private
     *  Move a column to a new position in the columns array, shifting all
     *  other columns left or right and updating the sortIndex and
     *  lastSortIndex variables accordingly.
     */
    mx_internal function shiftColumns(oldIndex:int, newIndex:int,
                                      trigger:Event = null):void
    {
        if (newIndex >= 0 && oldIndex != newIndex)
        {
            var incr:int = oldIndex < newIndex ? 1 : -1;
            for (var i:int = oldIndex; i != newIndex; i += incr)
            {
                var j:int = i + incr;
                var c:DataGridColumn = _columns[i];
                _columns[i] = _columns[j];
                _columns[j] = c;
                _columns[i].colNum = i;
                _columns[j].colNum = j;
            }

            if (sortIndex == oldIndex)
                sortIndex += newIndex - oldIndex;
            else if ((oldIndex < sortIndex && sortIndex <= newIndex)
                    || (newIndex <= sortIndex && sortIndex < oldIndex))
                sortIndex -= incr;

            if (lastSortIndex == oldIndex)
                lastSortIndex += newIndex - oldIndex;
            else if ((oldIndex < lastSortIndex
                        && lastSortIndex <= newIndex)
                    || (newIndex <= lastSortIndex
                        && lastSortIndex < oldIndex))
                lastSortIndex -= incr;

            columnsInvalid = true;
            itemsSizeChanged = true;
            invalidateDisplayList();
            var icEvent:IndexChangedEvent =
                new IndexChangedEvent(IndexChangedEvent.HEADER_SHIFT);
            icEvent.oldIndex = oldIndex;
            icEvent.newIndex = newIndex;
            icEvent.triggerEvent = trigger;
            dispatchEvent(icEvent);
        }
    }

    /**
     *  @private
     *  Searches the iterator to determine columns.
     */
    private function generateCols():void
    {
        if (collection.length > 0)
        {
            var col:DataGridColumn;
            var newCols:Array = [];
            var cols:Array;
            if (dataProvider)
            {
                try
                {
                    iterator.seek(CursorBookmark.FIRST);
                }
                catch (e:ItemPendingError)
                {
                    lastSeekPending = new ListBaseSeekPending(CursorBookmark.FIRST, 0);
                    e.addResponder(new ItemResponder(generateColumnsPendingResultHandler, seekPendingFailureHandler,
                                                    lastSeekPending));
                    iteratorValid = false;
                    return;
                }
                var info:Object =
                    ObjectUtil.getClassInfo(iterator.current,
                                            ["uid", "mx_internal_uid"]);

                if(info)
                    cols = info.properties;
            }

            if (!cols)
            {
                // introspect the first item and use its fields
                var itmObj:Object = iterator.current;
                for (var p:String in itmObj)
                {
                    if (p != "uid")
                    {
                        col = new DataGridColumn();
                        col.dataField = p;
                        newCols.push(col);
                    }
                }
            }
            else
            {
                // this is an old recordset - use its columns
                var n:int = cols.length;
                var colName:Object;
                for (var i:int = 0; i < n; i++)
                {
                    colName = cols[i];
                    if (colName is QName)
                        colName = QName(colName).localName;
                    col = new DataGridColumn();
                    col.dataField = String(colName);
                    newCols.push(col);
                }
            }
            columns = newCols;
            generatedColumns = true;
        }
    }

    /**
     *  @private
     */
    private function generateColumnsPendingResultHandler(data:Object, info:ListBaseSeekPending):void
    {
        // generate cols if we haven't successfully generated them
        if (columns.length == 0)
            generateCols();
        seekPendingResultHandler(data, info);
    }

    /**
     *  @private
     */
    private function calculateColumnSizes():void
    {
        var delta:Number;
        var n:int;
        var i:int;
        var totalWidth:Number = 0;
        var col:DataGridColumn;

        if (columns.length == 0)
        {
            visibleColumns = [];
            return;
        }

        // no columns are visible so figure out which ones
        // to make visible
        if (columnsInvalid)
        {
            columnsInvalid = false;
            visibleColumns = [];

            if (minColumnWidthInvalid)
            {
                n = columns.length;
                for (i = 0; i < n; i++)
                {
                    columns[i].minWidth = minColumnWidth;
                }
                minColumnWidthInvalid = false;
            }

            displayableColumns = null;
            n = _columns.length;
            for (i = 0; i < n; i++)
            {
                if (displayableColumns && _columns[i].visible)
                {
                    displayableColumns.push(_columns[i]);
                }
                else if (!displayableColumns && !_columns[i].visible)
                {
                    displayableColumns = new Array(i);
                    for (var k:int = 0; k < i; k++)
                        displayableColumns[k] = _columns[k];
                }
            }

            // If there are no hidden columns, displayableColumns points to
            // _columns (we don't need a duplicate copy of _columns).
            if (!displayableColumns)
                displayableColumns = _columns;

            // if no hscroll, then pack columns in available space
            if (horizontalScrollPolicy == ScrollPolicy.OFF)
            {
                n = displayableColumns.length;
                for (i = 0; i < n; i++)
                {
                    visibleColumns.push(displayableColumns[i]);
                }
            }
            else
            {
                n = displayableColumns.length;
                for (i = 0; i < n; i++)
                {
                    if (i >= lockedColumnCount &&
                        i < lockedColumnCount + horizontalScrollPosition)
                    {
                        continue;
                    }

                    col = displayableColumns[i];

                    if (totalWidth < displayWidth)
                    {
                        visibleColumns.push(col);
                        totalWidth += col.width;
                    }
                    else
					{
						if (visibleColumns.length == 0)
							visibleColumns.push(displayableColumns[0]);
                        break;
					}
                }
            }
        }

        var lastColumn:DataGridColumn;
        var newSize:Number;

        // if no hscroll, then pack columns in available space
        if (horizontalScrollPolicy == ScrollPolicy.OFF)
        {
            var numResizable:int = 0;
            var fixedWidth:Number = 0;

            // trace("resizing columns");

            // count how many resizable columns and how wide they are
            n = visibleColumns.length;
            for (i = 0; i < n; i++)
            {
                // trace("column " + i + " width = " + visibleColumns[i].width);
                if (visibleColumns[i].resizable)
                {
                    // trace("    resizable");
                    if (!isNaN(visibleColumns[i].explicitWidth))
                    {
                        // trace("    explicit width " + visibleColumns[i].width);
                        fixedWidth += visibleColumns[i].width;
                    }
                    else
                    {
                        // trace("    implicitly resizable");
                        numResizable++;
                        fixedWidth += visibleColumns[i].minWidth;
                        // trace("    minWidth " + visibleColumns[i].minWidth);
                    }
                }
                else
                {
                    // trace("    not resizable");
                    fixedWidth += visibleColumns[i].width;
                }

                totalWidth += visibleColumns[i].width;
            }
            // trace("totalWidth = " + totalWidth);
            // trace("displayWidth = " + displayWidth);

            var ratio:Number;
            var newTotal:Number = displayWidth;
            var minWidth:Number;
            if (displayWidth > fixedWidth && numResizable)
            {
                // we have flexible columns and room to honor minwidths and non-resizable
                // trace("have enough room");

                // divide and distribute the excess among the resizable
                n = visibleColumns.length;
                for (i = 0; i < n; i++)
                {
                    if (visibleColumns[i].resizable && isNaN(visibleColumns[i].explicitWidth))
                    {
                        lastColumn = visibleColumns[i];
                        if (totalWidth > displayWidth)
                            ratio = (lastColumn.width - lastColumn.minWidth)/ (totalWidth - fixedWidth);
                        else
                            ratio = lastColumn.width / totalWidth;
                        newSize = lastColumn.width - (totalWidth - displayWidth) * ratio;
                        minWidth = visibleColumns[i].minWidth;
                        visibleColumns[i].setWidth(newSize > minWidth ? newSize : minWidth);
                        // trace("column " + i + " set to " + visibleColumns[i].width);
                    }
                    newTotal -= visibleColumns[i].width;
                }
                if (newTotal && lastColumn)
                {
                    // trace("excess = " + newTotal);
                    lastColumn.setWidth(lastColumn.width + newTotal);
                }
            }
            else // can't honor minwidth and non-resizables so just scale everybody
            {
                // trace("too small or too big");
                n = visibleColumns.length;
                for (i = 0; i < n; i++)
                {
                    lastColumn = visibleColumns[i];
                    ratio = lastColumn.width / totalWidth;
                    //totalWidth -= visibleColumns[i].width;
                    newSize = displayWidth * ratio;
                    lastColumn.setWidth(newSize);
                    lastColumn.explicitWidth = NaN;
                    // trace("column " + i + " set to " + visibleColumns[i].width);
                    newTotal -= newSize;
                }
                if (newTotal && lastColumn)
                {
                    // trace("excess = " + newTotal);
                    lastColumn.setWidth(lastColumn.width + newTotal);
                }
            }
        }
        else // we have or can have an horizontalScrollBar
        {
            totalWidth = 0;
            // drop any that completely overflow
            n = visibleColumns.length;
            for (i = 0; i < n; i++)
            {
                if (totalWidth > displayWidth)
                {
                    visibleColumns.splice(i);
                    break;
                }
                totalWidth += visibleColumns[i].width;
            }

            i = visibleColumns[visibleColumns.length - 1].colNum + 1;
            // add more if we have room
            if (totalWidth < displayWidth && i < displayableColumns.length)
            {
                n = displayableColumns.length;
                for (; i < n && totalWidth < displayWidth; i++)
                {
                    col = displayableColumns[i];

                    visibleColumns.push(col);
                    totalWidth += col.width;
                }
            }

            lastColumn = visibleColumns[visibleColumns.length - 1];
            newSize = lastColumn.width + displayWidth - totalWidth;
            if (lastColumn == displayableColumns[displayableColumns.length - 1]
                && lastColumn.resizable && newSize >= lastColumn.minWidth
                    && newSize > lastColumn.width || (newSize < lastColumn.width
                        && newSize >= displayWidth/visibleColumns.length))
            {
                lastColumn.setWidth(newSize);
            }

            maxHorizontalScrollPosition =
                displayableColumns.length - visibleColumns.length + 1;
        }
    }

    /**
     *  @private
     *  If there is no horizontal scroll bar, changes the display width of other columns when
     *  one column's width is changed.
     *  @param col column whose width is changed
     *  @param w width of column
     */
    mx_internal function resizeColumn(col:int, w:Number):void
    {
        // there's a window of time before we calccolumnsizes
        // that someone can set width in AS
        if (!visibleColumns || visibleColumns.length == 0)
        {
            columns[col].setWidth(w);
            return;
        }

        if (w < visibleColumns[col].minWidth)
            w = visibleColumns[col].minWidth;

        // hScrollBar is present
        if (_horizontalScrollPolicy == ScrollPolicy.ON ||
            _horizontalScrollPolicy == ScrollPolicy.AUTO)
        {
            // adjust the column's width
            visibleColumns[col].setWidth(w);
            visibleColumns[col].explicitWidth = w;
            columnsInvalid = true;
        }
        else
        {

            // we want all cols's new widths to the right of this to be in proportion
            // to what they were before the stretch.

            // get the original space to the right not taken up by the column
            var totalSpace:Number = 0;
            var n:int = visibleColumns.length;
            var lastColumn:DataGridColumn;
            var i:int;
            var newWidth:Number;
            //non-resizable columns don't count though
            for (i = col + 1; i < n; i++)
            {
                if (visibleColumns[i].resizable)
                    totalSpace += visibleColumns[i].width;
            }

            var newTotalSpace:Number = visibleColumns[col].width - w + totalSpace;
            if (totalSpace)
            {
                visibleColumns[col].setWidth(w);
                visibleColumns[col].explicitWidth = w;
            }

            var totX:Number = 0;
            // resize the columns to the right proportionally to what they were
            for (i = col + 1; i < n; i++)
            {
                if (visibleColumns[i].resizable)
                {
                    newWidth = Math.floor(visibleColumns[i].width
                                                * newTotalSpace / totalSpace);
                    if (newWidth < visibleColumns[i].minWidth)
                        newWidth = visibleColumns[i].minWidth;
                    visibleColumns[i].setWidth(newWidth);
                    totX += visibleColumns[i].width;
                    lastColumn = visibleColumns[i];
                }
            }

            if (totX > newTotalSpace)
            {
                // if excess then should be taken out only from changing column
                // cause others would have already gone to their minimum
                newWidth = visibleColumns[col].width - totX + newTotalSpace;
                if (newWidth < visibleColumns[col].minWidth)
                    newWidth = visibleColumns[col].minWidth;
                visibleColumns[col].setWidth(newWidth);
            }
            else if (lastColumn)
            {
                // if less then should be added in last column
                // dont need to check for minWidth as we are adding
                lastColumn.setWidth(lastColumn.width - totX + newTotalSpace);
            }
        }
        itemsSizeChanged = true

        invalidateDisplayList();
    }

    /**
     *  Draws the background of the headers into the given 
     *  UIComponent.  The graphics drawn may be scaled horizontally
     *  if the component's width changes or this method will be
     *  called again to redraw at a different width and/or height
     *
     *  @param headerBG A UIComponent that will contain the header
     *  background graphics.
     */
    protected function drawHeaderBackground(headerBG:UIComponent):void
    {
        var g:Graphics = headerBG.graphics;
        g.clear();

        var tot:Number = displayWidth;

        var colors:Array = getStyle("headerColors");
        StyleManager.getColorNames(colors);

        // If we have vScroll only, extend the header over the scrollbar
        if (verticalScrollBar != null &&
            _horizontalScrollPolicy == ScrollPolicy.OFF &&
            headerVisible)
        {
            var bm:EdgeMetrics = borderMetrics;
            var adjustedWidth:Number = unscaledWidth - (bm.left + bm.right);
            tot = adjustedWidth;
            // Need to extend mask too.
               maskShape.width = adjustedWidth;
        }

        var hh:Number = rowInfo.length ? rowInfo[0].height : headerHeight;

        var matrix:Matrix = new Matrix();
        matrix.createGradientBox(tot, hh + 1, Math.PI/2, 0, 0);

        colors = [ colors[0], colors[0], colors[1] ];
        var ratios:Array = [ 0, 60, 255 ];
        var alphas:Array = [ 1.0, 1.0, 1.0 ];

        g.beginGradientFill(GradientType.LINEAR, colors, alphas, ratios, matrix);
        g.lineStyle(0, 0x000000, 0);
        g.moveTo(0, 0);
        g.lineTo(tot, 0);
        g.lineTo(tot, hh - 0.5);
        g.lineStyle(0, getStyle("borderColor"), 100);
        g.lineTo(0, hh - 0.5);
        g.lineStyle(0, 0x000000, 0);
        g.endFill();
    }

    /**
     *  Draws a row background 
     *  at the position and height specified using the
     *  color specified.  This implementation creates a Shape as a
     *  child of the input Sprite and fills it with the appropriate color.
     *  This method also uses the <code>backgroundAlpha</code> style property 
     *  setting to determine the transparency of the background color.
     * 
     *  @param s A Sprite that will contain a display object
     *  that contains the graphics for that row.
     *
     *  @param rowIndex The row's index in the set of displayed rows.  The
     *  header does not count, the top most visible row has a row index of 0.
     *  This is used to keep track of the objects used for drawing
     *  backgrounds so a particular row can re-use the same display object
     *  even though the index of the item that row is rendering has changed.
     *
     *  @param y The suggested y position for the background
     * 
     *  @param height The suggested height for the indicator
     * 
     *  @param color The suggested color for the indicator
     * 
     *  @param dataIndex The index of the item for that row in the
     *  data provider.  This can be used to color the 10th item differently
     *  for example.
     */
    protected function drawRowBackground(s:Sprite, rowIndex:int,
                                            y:Number, height:Number, color:uint, dataIndex:int):void
    {
        var background:Shape;
        if (rowIndex < s.numChildren)
        {
            background = Shape(s.getChildAt(rowIndex));
        }
        else
        {
            background = new FlexShape();
            background.name = "background";
            s.addChild(background);
        }

        background.y = y;

        // Height is usually as tall is the items in the row, but not if
        // it would extend below the bottom of listContent
        var height:Number = Math.min(height,
                                     listContent.height -
                                     y);

        var g:Graphics = background.graphics;
        g.clear();
        g.beginFill(color, getStyle("backgroundAlpha"));
        g.drawRect(0, 0, displayWidth, height);
        g.endFill();
    }

    /**
     *  Draws a column background for a column with the suggested color.
     *  This implementation creates a Shape as a
     *  child of the input Sprite and fills it with the appropriate color.
     *
     *  @param s A Sprite that will contain a display object
     *  that contains the graphics for that column.
     *
     *  @param columnIndex The column's index in the set of displayed columns.  
     *  The left most visible column has a column index of 0.
     *  This is used to keep track of the objects used for drawing
     *  backgrounds so a particular column can re-use the same display object
     *  even though the index of the DataGridColumn for that column has changed.
     *
     *  @param color The suggested color for the indicator
     * 
     *  @param column The column of the DataGrid control that you are drawing the background for.
     */
    protected function drawColumnBackground(s:Sprite, columnIndex:int,
                                            color:uint, column:DataGridColumn):void
    {
        var background:Shape;
        background = Shape(s.getChildByName(columnIndex.toString()));
        if (!background)
        {
            background = new FlexShape();
            s.addChild(background);
            background.name = columnIndex.toString();
        }

        var g:Graphics = background.graphics;
        g.clear();
        g.beginFill(color);

        var lastRow:Object = rowInfo[listItems.length - 1];
        var xx:Number = listItems[0][columnIndex].x
        var yy:Number = rowInfo[0].y
        if (headerVisible)
            yy += rowInfo[0].height;

        // Height is usually as tall is the items in the row, but not if
        // it would extend below the bottom of listContent
        var height:Number = Math.min(lastRow.y + lastRow.height,
                                     listContent.height - yy);

        g.drawRect(xx, yy, listItems[0][columnIndex].width,
                   listContent.height - yy);
        g.endFill();
    }

    /**
     *  Draws a line between rows.  This implementation draws a line
     *  directly into the given Sprite.  The Sprite has been cleared
     *  before lines are drawn into it.
     *
     *  @param s A Sprite that will contain a display object
     *  that contains the graphics for that row.
     *
     *  @param rowIndex The row's index in the set of displayed rows.  The
     *  header does not count, the top most visible row has a row index of 0.
     *  This is used to keep track of the objects used for drawing
     *  backgrounds so a particular row can re-use the same display object
     *  even though the index of the item that row is rendering has changed.
     *
     *  @param color The suggested color for the indicator
     * 
     *  @param y The suggested y position for the background
     */
    protected function drawHorizontalLine(s:Sprite, rowIndex:int, color:uint, y:Number):void
    {
        var g:Graphics = s.graphics;

        if (lockedRowCount > (headerVisible ? 1 : 0) && rowIndex == lockedRowCount - 1)
            g.lineStyle(0, 0);
        else
            g.lineStyle(0, color);

        g.moveTo(0, y);
        g.lineTo(displayWidth, y);
    }

    /**
     *  Draw lines between columns.  This implementation draws a line
     *  directly into the given Sprite.  The Sprite has been cleared
     *  before lines are drawn into it.
     *
     *  @param s A Sprite that will contain a display object
     *  that contains the graphics for that row.
     *
     *  @param columnIndex The column's index in the set of displayed columns.  
     *  The left most visible column has a column index of 0.
     *
     *  @param color The suggested color for the indicator
     * 
     *  @param x The suggested x position for the background
     */
    protected function drawVerticalLine(s:Sprite, colIndex:int, color:uint, x:Number):void
    {
        //draw our vertical lines
        var g:Graphics = s.graphics;
        if (lockedColumnCount > 0 && colIndex == lockedColumnCount - 1)
            g.lineStyle(1, 0, 100);
        else
            g.lineStyle(1, color, 100);
        g.moveTo(x, 1);
        g.lineTo(x, listContent.height);
    }

    /**
     *  Draw lines between columns, and column backgrounds.
     *  This implementation calls the <code>drawHorizontalLine()</code>, 
     *  <code>drawVerticalLine()</code>,
     *  and <code>drawColumnBackground()</code> methods as needed.  
     *  It creates a
     *  Sprite that contains all of these graphics and adds it as a
     *  child of the listContent at the front of the z-order.
     */
    protected function drawLinesAndColumnBackgrounds():void
    {
        var lines:Sprite = Sprite(listContent.getChildByName("lines"));
        if (!lines)
        {
            lines = new UIComponent();
            lines.name = "lines";
            lines.cacheAsBitmap = true;
            lines.mouseEnabled = false;
            listContent.addChild(lines);
        }
        listContent.setChildIndex(lines, listContent.numChildren - 1);

        lines.graphics.clear();

        var tmpHeight:Number = unscaledHeight - 1; // FIXME: can remove?
        var lineCol:uint;

        var i:int;

        var len:uint = (listItems && listItems[0]) ? listItems[0].length : visibleColumns.length;
		// defend against degenerate case when width == 0
		if (len > visibleColumns.length)
			len = visibleColumns.length;

        // draw horizontalGridlines if needed.
        lineCol = getStyle("horizontalGridLineColor");
        if (getStyle("horizontalGridLines"))
        {
            for (i = headerVisible ? 1 : 0; i < listItems.length; i++)
            {
                drawHorizontalLine(lines, i, lineCol, rowInfo[i].y + rowInfo[i].height);
            }
        }
        else if (lockedRowCount > (headerVisible ? 1 : 0) && lockedRowCount < listItems.length)
        {
            drawHorizontalLine(lines, lockedRowCount - 1, lineCol, rowInfo[lockedRowCount - 1].y + rowInfo[lockedRowCount - 1].height);
        }

        var vLines:Boolean = getStyle("verticalGridLines");
        lineCol = getStyle("verticalGridLineColor");

        if (listItems && listItems[0] && listItems[0][len - 1])
        {
            var colBGs:Sprite = Sprite(listContent.getChildByName("colBGs"));
            // traverse the columns, set the sizes, draw the column backgrounds
            for (i = 0; i < len; i++)
            {
                if (vLines)
                    drawVerticalLine(lines, i, lineCol, listItems[0][i].x + visibleColumns[i].width);

                var col:DataGridColumn = visibleColumns[i];
                var bgCol:Object;
                if (enabled)
                    bgCol = col.getStyle("backgroundColor");
                else
                    bgCol = col.getStyle("backgroundDisabledColor");

                if (bgCol !== null && !isNaN(Number(bgCol)))
                {
                    if (!colBGs)
                    {
                        colBGs = new FlexSprite();
                        colBGs.mouseEnabled = false;
                        colBGs.name = "colBGs";
                        listContent.addChildAt(colBGs, listContent.getChildIndex(listContent.getChildByName("rowBGs")) + 1);
                    }
                    drawColumnBackground(colBGs, i, Number(bgCol), col);
                }
                else if (colBGs)
                {
                    var background:Shape = Shape(colBGs.getChildByName(i.toString()));
                    if (background)
                    {
                        var g:Graphics = background.graphics;
                        g.clear();
                        colBGs.removeChild(background);
                    }
                }
            }
        }

        if (!vLines && lockedColumnCount > 0 && lockedColumnCount < len)
            drawVerticalLine(lines, lockedColumnCount - 1, lineCol, listItems[0][lockedColumnCount - 1].x + visibleColumns[lockedColumnCount - 1].width);

    }

    /**
     *  Removes column header separators that the user normally uses
     *  to resize columns.
     */
    protected function clearSeparators():void
    {
        if (!separators)
            return;

        var lines:Sprite = Sprite(listContent.getChildByName("lines"));
        while (lines.numChildren)
        {
            lines.removeChildAt(lines.numChildren - 1);
            separators.pop();
        }
    }

    /**
     *  Creates and displays the column header separators that the user 
     *  normally uses to resize columns.  This implementation uses
     *  the same Sprite as the lines and column backgrounds and adds
     *  instances of the <code>headerSeparatorSkin</code> and attaches mouse
     *  listeners to them in order to know when the user wants
     *  to resize a column.
     */
    protected function drawSeparators():void
    {
        if (!separators)
            separators = [];

        var lines:Sprite = Sprite(listContent.getChildByName("lines"));

        var n:int = visibleColumns.length;
        for (var i:int = 0; i < n; i++)
        {
            var sep:UIComponent;
            var sepSkin:IFlexDisplayObject;
            
            if (i < lines.numChildren)
            {
                sep = UIComponent(lines.getChildAt(i));
                sepSkin = IFlexDisplayObject(sep.getChildAt(0));
            }
            else
            {
                var headerSeparatorClass:Class =
                    getStyle("headerSeparatorSkin");
                sepSkin = new headerSeparatorClass();
                if (sepSkin is ISimpleStyleClient)
                    ISimpleStyleClient(sepSkin).styleName = this;
                sep = new UIComponent();
                sep.addChild(DisplayObject(sepSkin));
                lines.addChild(sep);
                DisplayObject(sep).addEventListener(
                    MouseEvent.MOUSE_OVER, columnResizeMouseOverHandler);
                DisplayObject(sep).addEventListener(
                    MouseEvent.MOUSE_OUT, columnResizeMouseOutHandler);
                DisplayObject(sep).addEventListener(
                    MouseEvent.MOUSE_DOWN, columnResizeMouseDownHandler);
                separators.push(sep);
            }

            if (!listItems || !listItems[0] || !listItems[0][i])
            {
                sep.visible = false;
                continue;
            }

            sep.visible = true;
            sep.x = listItems[0][i].x +
                    visibleColumns[i].width - Math.round(sep.measuredWidth / 2 + 0.5);
            if (i > 0)
            {
                sep.x = Math.max(sep.x,
                                 separators[i - 1].x + Math.round(sep.measuredWidth / 2 + 0.5));
            }
            sep.y = 0;
            sepSkin.setActualSize(sepSkin.measuredWidth,
                                  rowInfo.length ?
                                  rowInfo[0].height :
                                  headerHeight);
            
            // Draw invisible background for separator affordance
            sep.graphics.clear();
            sep.graphics.beginFill(0xFFFFFF, 0);
            sep.graphics.drawRect(-separatorAffordance, 0, sepSkin.measuredWidth + separatorAffordance , headerHeight);
            sep.graphics.endFill();
        }
        while (lines.numChildren > visibleColumns.length)
        {
            lines.removeChildAt(lines.numChildren - 1);
            separators.pop();
        }
    }

    /**
     *  @private
     *  Update sortIndex and sortDirection based on sort info availabled in
     *  underlying data provider.
     */
    private function updateSortIndexAndDirection():void
    {
        // Don't show sort indicator if sortableColumns is false or if the
        // column sorted on has sortable="false"

        if (!sortableColumns)
        {
            lastSortIndex = sortIndex;
            sortIndex = -1;

            if (lastSortIndex != sortIndex)
                invalidateDisplayList();

            return;
        }

        if (!dataProvider)
            return;

        var view:ICollectionView = ICollectionView(dataProvider);
        var sort:Sort = view.sort;
        if (!sort)
        {
            sortIndex = lastSortIndex = -1;
            return;
        }

        var fields:Array = sort.fields;
        if (!fields)
            return;

        if (fields.length != 1)
        {
            lastSortIndex = sortIndex;
            sortIndex = -1;

            if (lastSortIndex != sortIndex)
                invalidateDisplayList();

            return;
        }

        // fields.length == 1, so the collection is sorted on a single field.
        var sortField:SortField = fields[0];
        var n:int = _columns.length;
        for (var i:int = 0; i < n; i++)
        {
            if (_columns[i].dataField == sortField.name)
            {
                sortIndex = _columns[i].sortable ? i : -1;
                sortDirection = sortField.descending ? "DESC" : "ASC";
                return;
            }
        }
    }

    /**
     *  Draws the sort arrow graphic on the column that is the current sort key.
     *  This implementation creates or reuses an instance of the skin specified
     *  by <code>sortArrowSkin</code> style property and places 
     *  it in the appropriate column header.  It
     *  also shrinks the size of the column header if the text in the header
     *  would be obscured by the sort arrow.
     */
    protected function placeSortArrow():void
    {

        var sortArrowHitArea:Sprite =
            Sprite(listContent.getChildByName("sortArrowHitArea"));

        if (sortIndex == -1 && lastSortIndex == -1)
        {
            if (sortArrow)
                sortArrow.visible = false;
            if (sortArrowHitArea)
                sortArrowHitArea.visible = false;
            return;
        }

        if (!headerVisible)
        {
            if (sortArrow)
                sortArrow.visible = false;
            if (sortArrowHitArea)
                sortArrowHitArea.visible = false;
            return;
        }

        if (!sortArrow)
        {
            var sortArrowClass:Class = getStyle("sortArrowSkin");
            sortArrow = new sortArrowClass();
            DisplayObject(sortArrow).name = "sortArrow";
            listContent.addChild(DisplayObject(sortArrow));
        }
        var xx:Number;
        var n:int;
        var i:int;
        if (listItems && listItems.length && listItems[0])
        {
            n = listItems[0].length;
            for (i = 0; i < n; i++)
            {
                if (visibleColumns[i].colNum == sortIndex)
                {
                    xx = listItems[0][i].x + visibleColumns[i].width;
                    listItems[0][i].setActualSize(visibleColumns[i].width - sortArrow.measuredWidth - 8, listItems[0][i].height);

                    if (!isNaN(listItems[0][i].explicitWidth))
                        listItems[0][i].explicitWidth = listItems[0][i].width;

                    // Create hit area to capture mouse clicks behind arrow.
                    if (!sortArrowHitArea)
                    {
                        sortArrowHitArea = new FlexSprite();
                        sortArrowHitArea.name = "sortArrowHitArea";
                        listContent.addChild(sortArrowHitArea);
                    }
                    else
                        sortArrowHitArea.visible = true;

                    sortArrowHitArea.x = listItems[0][i].x + listItems[0][i].width;
                    sortArrowHitArea.y = listItems[0][i].y;

                    var g:Graphics = sortArrowHitArea.graphics;
                    g.clear();
                    g.beginFill(0, 0);
                    g.drawRect(0, 0, sortArrow.measuredWidth + 8,
                            listItems[0][i].height);
                    g.endFill();

                    break;
                }
            }
        }
        if (isNaN(xx))
        {
            sortArrow.visible = false;
            return;
        }
        sortArrow.visible = true;
        if (lastSortIndex >= 0 && lastSortIndex != sortIndex)
            if (visibleColumns[0].colNum <= lastSortIndex && lastSortIndex <= visibleColumns[visibleColumns.length - 1].colNum)
            {
                n = listItems[0].length;
                for (var j:int = 0; j < n ; j++)
                {
                    if (visibleColumns[j].colNum == lastSortIndex)
                    {
                        listItems[0][j].setActualSize(visibleColumns[j].width, listItems[0][j].height);
                        break;
                    }
                }
            }

        var d:Boolean = (sortDirection == "ASC");
        sortArrow.width = sortArrow.measuredWidth;
        sortArrow.height = sortArrow.measuredHeight;
        DisplayObject(sortArrow).scaleY = (d) ? -1.0 : 1.0;
        sortArrow.x = xx - sortArrow.measuredWidth - 8;
        var hh:Number = rowInfo.length ? rowInfo[0].height : headerHeight
        sortArrow.y = (hh - sortArrow.measuredHeight) / 2 + ((d) ? sortArrow.measuredHeight: 0);

        if (sortArrow.x < listItems[0][i].x)
            sortArrow.visible = false;

        if (!sortArrow.visible && sortArrowHitArea)
            sortArrowHitArea.visible = false;
    }

    /**
     *  @private
     */
    private function sortByColumn(index:int):void
    {
        var c:DataGridColumn = columns[index];
        var desc:Boolean = c.sortDescending;

        // do the sort if we're allowed to
        if (c.sortable)
        {
            var s:Sort = collection.sort;
            var f:SortField;
            if (s)
            {
                s.compareFunction = null;
                // analyze the current sort to see what we've been given
                var sf:Array = s.fields;
                if (sf)
                {
                    for (var i:int = 0; i < sf.length; i++)
                    {

                        if (sf[i].name == c.dataField)
                        {
                            // we're part of the current sort
                            f = sf[i]
                            // flip the logic so desc is new desired order
                            desc = !f.descending;
                            break;
                        }
                    }
                }
            }
            else
                s = new Sort;

            if (!f)
                f = new SortField(c.dataField);


            c.sortDescending = desc;
            var dir:String = (desc) ? "DESC" : "ASC";
            sortDirection = dir;

            // set the grid's sortIndex
            lastSortIndex = sortIndex;
            sortIndex = index;
            sortColumn = c;

            placeSortArrow();

            // if you have a labelFunction you must supply a sortCompareFunction
            f.name = c.dataField;
            if (c.sortCompareFunction != null)
            {
                f.compareFunction = c.sortCompareFunction;
            }
            else
            {
                f.compareFunction = null;
            }
            f.descending = desc;
            s.fields = [f];
        }
        collection.sort = s;
        collection.refresh();

    }

    /**
     *  @private
     */
    private function setEditedItemPosition(coord:Object):void
    {
        bEditedItemPositionChanged = true;
        _proposedEditedItemPosition = coord;
        invalidateDisplayList();
    }

    /**
     *  @private
     *  focus an item renderer in the grid - harder than it looks
     */
    private function commitEditedItemPosition(coord:Object):void
    {
        if (!enabled || !editable)
            return;

        // just give focus back to the itemEditorInstance
        if (itemEditorInstance && coord &&
            itemEditorInstance is IFocusManagerComponent &&
            _editedItemPosition.rowIndex == coord.rowIndex &&
            _editedItemPosition.columnIndex == coord.columnIndex)
        {
            IFocusManagerComponent(itemEditorInstance).setFocus();
            return;
        }

        // dispose of any existing editor, saving away its data first
        if (itemEditorInstance)
        {
            var reason:String;
            if (!coord)
            {
                reason = DataGridEventReason.OTHER;
            }
            else
            {
                reason = (!editedItemPosition || coord.rowIndex == editedItemPosition.rowIndex) ?
                         DataGridEventReason.NEW_COLUMN :
                         DataGridEventReason.NEW_ROW;
            }
            if (!endEdit(reason) && reason != DataGridEventReason.OTHER)
                return;
        }

        // store the value
        _editedItemPosition = coord;

        // allow setting of undefined to dispose item editor instance
        if (!coord)
            return;

        if (dontEdit)
        {
            return;
        }

        var rowIndex:int = coord.rowIndex;
        var colIndex:int = coord.columnIndex;
        if (displayableColumns.length != _columns.length)
        {
            for (var i:int = 0; i < displayableColumns.length; i++)
            {
                if (displayableColumns[i].colNum >= colIndex)
                {
                    colIndex = i;
                    break;
                }
            }
            if (i == displayableColumns.length)
                colIndex = 0;
        }

        // trace("commitEditedItemPosition ", coord.rowIndex, selectedIndex);

        if (selectedIndex != coord.rowIndex)
            commitSelectedIndex(coord.rowIndex);

        var actualLockedRows:int = lockedRowCount - (headerVisible ? 1 : 0);
        var lastRowIndex:int = verticalScrollPosition + listItems.length - 1 - (headerVisible ? 1 : 0);
        var partialRow:int = (rowInfo[listItems.length - 1].y + rowInfo[listItems.length - 1].height > listContent.height) ? 1 : 0;

        // actual row/column is the offset into listItems
        if (rowIndex > actualLockedRows)
        {
            // not a locked editable row make sure it is on screen
            if (rowIndex < verticalScrollPosition + actualLockedRows)
                verticalScrollPosition = rowIndex - actualLockedRows;
            else
            {
                // variable row heights means that we can't know how far to scroll sometimes so we loop
                // until we get it right
                while (rowIndex > lastRowIndex ||
                    // we're the last row, and we're partially visible, but we're not
                    // the top scrollable row already
                    (rowIndex == lastRowIndex && rowIndex > verticalScrollPosition + actualLockedRows &&
                        partialRow))
                {
                    if (verticalScrollPosition == maxVerticalScrollPosition)
                        break;
                    verticalScrollPosition = Math.min(verticalScrollPosition + (rowIndex > lastRowIndex ? rowIndex - lastRowIndex : partialRow), maxVerticalScrollPosition);
                    lastRowIndex = verticalScrollPosition + listItems.length - 1 - (headerVisible ? 1 : 0);
                    partialRow = (rowInfo[listItems.length - 1].y + rowInfo[listItems.length - 1].height > listContent.height) ? 1 : 0;
                }
            }

            actualRowIndex = rowIndex - verticalScrollPosition + (headerVisible ? 1 : 0);

        }
        else
        {
            if (rowIndex == actualLockedRows)
                verticalScrollPosition = 0;

            actualRowIndex = rowIndex + (headerVisible ? 1 : 0);
        }

        var bm:EdgeMetrics = borderMetrics;

        var len:uint = (listItems && listItems[0]) ? listItems[0].length : visibleColumns.length;
        var lastColIndex:int = horizontalScrollPosition + len - 1;
        var partialCol:int = (listItems[0][len - 1].x + listItems[0][len - 1].width > listContent.width) ? 1 : 0;

        if(colIndex > lockedColumnCount)
        {
            if (colIndex < horizontalScrollPosition + lockedColumnCount)
                horizontalScrollPosition = colIndex - lockedColumnCount;
            else
            {
                while (colIndex > lastColIndex ||
                       (colIndex == lastColIndex && colIndex > horizontalScrollPosition + lockedColumnCount &&
                       partialCol))
                {
                    if (horizontalScrollPosition == maxHorizontalScrollPosition)
                        break;
                    horizontalScrollPosition = Math.min(horizontalScrollPosition + (colIndex > lastColIndex ? colIndex - lastColIndex : partialCol), maxHorizontalScrollPosition);
                    len = (listItems && listItems[0]) ? listItems[0].length : visibleColumns.length;
                    lastColIndex = horizontalScrollPosition + len - 1;
                    partialCol = (listItems[0][len - 1].x + listItems[0][len - 1].width > listContent.width) ? 1 : 0;
                }
            }
            actualColIndex = colIndex - horizontalScrollPosition;
        }
        else
        {
            if (colIndex == lockedColumnCount)
                horizontalScrollPosition = 0;

            actualColIndex = colIndex;
        }


        // get the actual references for the column, row, and item
        var item:IListItemRenderer = listItems[actualRowIndex][actualColIndex];
        if (!item)
        {
            // assume that editing was cancelled
            commitEditedItemPosition(null);
            return;
        }

        var event:DataGridEvent =
            new DataGridEvent(DataGridEvent.ITEM_EDIT_BEGIN, false, true);
            // ITEM_EDIT events are cancelable
        event.columnIndex = displayableColumns[colIndex].colNum;
        event.rowIndex = _editedItemPosition.rowIndex;
        event.itemRenderer = item;
        dispatchEvent(event);

        lastEditedItemPosition = _editedItemPosition;

        // user may be trying to change the focused item renderer
        if (bEditedItemPositionChanged)
        {
            bEditedItemPositionChanged = false;
            commitEditedItemPosition(_proposedEditedItemPosition);
            _proposedEditedItemPosition = undefined;

        }

        if (!itemEditorInstance)
        {
            // assume that editing was cancelled
            commitEditedItemPosition(null);
        }
    }

    /**
     *  Creates the item editor for the item renderer at the
     *  <code>editedItemPosition</code> using the editor
     *  specified by the <code>itemEditor</code> property.
     *
     *  <p>This method sets the editor instance as the 
     *  <code>itemEditorInstance</code> property.</p>
     *
     *  <p>You may only call this method from within the event listener
     *  for the <code>itemEditBegin</code> event. 
     *  To create an editor at other times, set the
     *  <code>editedItemPosition</code> property to generate 
     *  the <code>itemEditBegin</code> event.</p>
     *
     *  @param colIndex The column index in the data provider of the item to be edited.
     *
     *  @param rowIndex The row index in the data provider of the item to be edited.
     */
    public function createItemEditor(colIndex:int, rowIndex:int):void
    {
        if (displayableColumns.length != _columns.length)
        {
            for (var i:int = 0; i < displayableColumns.length; i++)
            {
                if (displayableColumns[i].colNum >= colIndex)
                {
                    colIndex = i;
                    break;
                }
            }
            if (i == displayableColumns.length)
                colIndex = 0;
        }

        var col:DataGridColumn = displayableColumns[colIndex];
        if (rowIndex > lockedRowCount - (headerVisible ? 1 : 0))
            rowIndex -= verticalScrollPosition;

        if (headerVisible)
            rowIndex++;

        if (colIndex > lockedColumnCount)
            colIndex -= horizontalScrollPosition;

        var item:IListItemRenderer = listItems[rowIndex][colIndex];
        var rowData:ListRowInfo = rowInfo[rowIndex];

        if (!col.rendererIsEditor)
        {
            var dx:Number = 0;
            var dy:Number = -2;
            var dw:Number = 0;
            var dh:Number = 4;
            // if this isn't implemented, use an input control as editor
            if (!itemEditorInstance)
            {
                var itemEditor:IFactory = col.itemEditor;
                dx = col.editorXOffset;
                dy = col.editorYOffset;
                dw = col.editorWidthOffset;
                dh = col.editorHeightOffset;
                itemEditorInstance = itemEditor.newInstance();
                itemEditorInstance.owner = this;
                itemEditorInstance.styleName = col;
                listContent.addChild(DisplayObject(itemEditorInstance));
            }
            listContent.setChildIndex(DisplayObject(itemEditorInstance), listContent.numChildren - 1);
            // give it the right size, look and placement
            itemEditorInstance.visible = true;
            itemEditorInstance.move(item.x + dx, rowData.y + dy);
            itemEditorInstance.setActualSize(Math.min(col.width + dw, listContent.width - listContent.x - itemEditorInstance.x),
                                     Math.min(rowData.height + dh, listContent.height - listContent.y - itemEditorInstance.y));
            DisplayObject(itemEditorInstance).addEventListener(FocusEvent.FOCUS_OUT, itemEditorFocusOutHandler);
            item.visible = false;

        }
        else
        {
            // if the item renderer is also the editor, we'll use it
            itemEditorInstance = item;
        }

        // listen for keyStrokes on the itemEditorInstance (which lets the grid supervise for ESC/ENTER)
        DisplayObject(itemEditorInstance).addEventListener(KeyboardEvent.KEY_DOWN, editorKeyDownHandler);
        // we disappear on any mouse down outside the editor
        stage.addEventListener(MouseEvent.MOUSE_DOWN, editorMouseDownHandler, true, 0, true);
		// we disappear if stage is resized
        stage.addEventListener(Event.RESIZE, editorStageResizeHandler, true, 0, true);
    }

    /**
     *  @private
     *  Determines the next item renderer to navigate to using the Tab key.
     *  If the item renderer to be focused falls out of range (the end or beginning
     *  of the grid) then move focus outside the grid.
     */
    private function findNextItemRenderer(shiftKey:Boolean):Boolean
    {
        if (!lastEditedItemPosition)
            return false;

        // some other thing like a collection change has changed the
        // position, so bail and wait for commit to reset the editor.
        if (_proposedEditedItemPosition !== undefined)
            return false;

        _editedItemPosition = lastEditedItemPosition;

        var index:int = _editedItemPosition.rowIndex;
        var colIndex:int = _editedItemPosition.columnIndex;

        var found:Boolean = false;
        var incr:int = shiftKey ? -1 : 1;
        var maxIndex:int = headerVisible ? collection.length : collection.length - 1;

        // cycle till we find something worth focusing, or the end of the grid
        while (!found)
        {
            // go to next column
            colIndex += incr;
            if (colIndex >= _columns.length || colIndex < 0)
            {
                // if we fall off the end of the columns, wrap around
                colIndex = (colIndex < 0) ? _columns.length - 1 : 0;
                // and increment/decrement the row index
                index += incr;
                if (index >= maxIndex || index < 0)
                {
                    // if we've fallen off the rows, we need to leave the grid. get rid of the editor
                    setEditedItemPosition(null);
                    // set focus back to the grid so default handler will move it to the next component
                    losingFocus = true;
                    setFocus();
                    return false;
                }
            }
            // if we find a visible and editable column, move to it
            if (_columns[colIndex].editable && _columns[colIndex].visible)
            {
                found = true;
                // kill the old edit session
                var reason:String;
                reason = index == _editedItemPosition.rowIndex ?
                         DataGridEventReason.NEW_COLUMN :
                         DataGridEventReason.NEW_ROW;
                if (!itemEditorInstance || endEdit(reason))
                {
                    // send event to create the new one
                    var dataGridEvent:DataGridEvent =
                        new DataGridEvent(DataGridEvent.ITEM_EDIT_BEGINNING, false, true);
                        // ITEM_EDIT events are cancelable
                    dataGridEvent.columnIndex = colIndex;
                    dataGridEvent.dataField = _columns[colIndex].dataField;
                    dataGridEvent.rowIndex = index;
                    dispatchEvent(dataGridEvent);
                }
            }
        }
        return found;
    }

    /**
     *  This method closes an item editor currently open on an item renderer. 
     *  You typically only call this method from within the event listener 
     *  for the <code>itemEditEnd</code> event, after
     *  you have already called the <code>preventDefault()</code> method to 
     *  prevent the default event listener from executing.
     */
    public function destroyItemEditor():void
    {
        // trace("destroyItemEditor");
        if (itemEditorInstance)
        {
            DisplayObject(itemEditorInstance).removeEventListener(KeyboardEvent.KEY_DOWN, editorKeyDownHandler);
            stage.removeEventListener(MouseEvent.MOUSE_DOWN, editorMouseDownHandler, true);
            stage.removeEventListener(Event.RESIZE, editorStageResizeHandler, true);

            var event:DataGridEvent =
                new DataGridEvent(DataGridEvent.ITEM_FOCUS_OUT);
            event.columnIndex = _editedItemPosition.columnIndex;
            event.rowIndex = _editedItemPosition.rowIndex;
            event.itemRenderer = itemEditorInstance;
            dispatchEvent(event);

            if (! _columns[_editedItemPosition.columnIndex].rendererIsEditor)
            {
                // FocusManager.removeHandler() does not find
                // itemEditors in focusableObjects[] array
                // and hence does not remove the focusRectangle
                if (itemEditorInstance && itemEditorInstance is UIComponent)
                    UIComponent(itemEditorInstance).drawFocus(false);

                // must call removeChild() so FocusManager.lastFocus becomes null
                listContent.removeChild(DisplayObject(itemEditorInstance));
                editedItemRenderer.visible = true;
            }
            itemEditorInstance = null;
            _editedItemPosition = null;
        }
    }

    /**
     *  @private
     *  When the user finished editing an item, this method is called.
     *  It dispatches the DataGridEvent.ITEM_EDIT_END event to start the process
     *  of copying the edited data from
     *  the itemEditorInstance to the data provider and hiding the itemEditorInstance.
     *  returns true if nobody called preventDefault.
     */
    private function endEdit(reason:String):Boolean
    {
        // this happens if the renderer is removed asynchronously ususally with FDS
        if (!editedItemRenderer)
            return true;

        inEndEdit = true;

        var dataGridEvent:DataGridEvent =
            new DataGridEvent(DataGridEvent.ITEM_EDIT_END, false, true);
            // ITEM_EDIT events are cancelable
        dataGridEvent.columnIndex = editedItemPosition.columnIndex;
        dataGridEvent.dataField = _columns[editedItemPosition.columnIndex].dataField;
        dataGridEvent.rowIndex = editedItemPosition.rowIndex;
        dataGridEvent.itemRenderer = editedItemRenderer;
        dataGridEvent.reason = reason;
        dispatchEvent(dataGridEvent);
        // set a flag to not open another edit session if the item editor is still up
        // this means somebody wants the old edit session to stay.
        dontEdit = itemEditorInstance != null;
        // trace("dontEdit", dontEdit);

        if (!dontEdit && reason == DataGridEventReason.CANCELLED)
        {
            losingFocus = true;
            setFocus();
        }

        inEndEdit = false;

        return !(dataGridEvent.isDefaultPrevented())
    }

    /**
     *  @private
     */
    mx_internal function columnRendererChanged(c:DataGridColumn):void
    {
        var item:IListItemRenderer;
        
        var factory:IFactory = columnItemRendererFactory(c,true);
        if (measuringObjects)
        {
            item = measuringObjects[factory];
            if (item)
            {
                listContent.removeChild(DisplayObject(item));
                measuringObjects[factory] = null;
            }
            factory = columnItemRendererFactory(c,false);
            item = measuringObjects[factory];
            if (item)
            {
                listContent.removeChild(DisplayObject(item));
                measuringObjects[factory] = null;
            }
        }
        var freeRenderers:Array = freeItemRenderersTable[c] as Array;
        if (freeRenderers)
            while (freeRenderers.length)
            {
                listContent.removeChild(freeRenderers.pop());
            }

        rendererChanged = true;
        invalidateDisplayList();
    }

    /**
     *  @private
     */
    private function columnDraggingMouseMoveHandler(event:MouseEvent):void
    {
        if (!event.buttonDown)
        {
            columnDraggingMouseUpHandler(event);
            return;
        }

        var item:IListItemRenderer;
        var c:DataGridColumn = movingColumn;
        var s:Sprite;
        var i:int = 0;
        var n:int = listItems[0].length;

        if (isNaN(startX))
        {
            // If startX is not a number, dragging has just started.
            // Initialise and return without actually moving anything.

            startX = event.stageX;

            // Set this to null so sort doesn't happen.
            lastItemDown = null;

            // Create and position proxy.
            var proxy:IListItemRenderer = columnItemRenderer(c, true);
            proxy.name = "headerDragProxy";

            var rowData:DataGridListData = DataGridListData(makeListData(c, null, 0, c.colNum, c));
            if (proxy is IDropInListItemRenderer)
                IDropInListItemRenderer(proxy).listData = rowData;

            listContent.addChild(DisplayObject(proxy));

            proxy.data = c;
            proxy.styleName = getStyle("headerDragProxyStyleName");
            UIComponentGlobals.layoutManager.validateClient(proxy, true);
            proxy.setActualSize(c.width, _explicitHeaderHeight ?
                headerHeight : proxy.getExplicitOrMeasuredHeight());

            for (i = 0; i < n; i++)
            {
                item = listItems[0][i];
                if (item.data == movingColumn)
                    break;
            }
            proxy.move(item.x, item.y);

            // Create, position and draw column overlay.
            s = new FlexSprite();
            s.name = "columnDragOverlay";
            s.alpha = 0.6;
            listContent.addChildAt(s, listContent.getChildIndex(selectionLayer));

            var vm:EdgeMetrics = viewMetrics;

            s.x = item.x;
            s.y = 0;

            if (c.width > 0)
            {
                var g:Graphics = s.graphics;
                g.beginFill(getStyle("disabledColor"));
                g.drawRect(0, 0, c.width,
                        unscaledHeight - vm.bottom - s.y);
                g.endFill();
            }

            s = Sprite(selectionLayer.getChildByName("headerSelection"));
            if (s)
                s.width = movingColumn.width;


            if (!listContent.mask)
            {
                // Clip the contents so the header drag proxy doesn't show
                // outside the list.
                var bm:EdgeMetrics = borderMetrics;
                listContent.scrollRect = new Rectangle(0, 0,
                        unscaledWidth - bm.left - bm.right,
                        unscaledHeight - bm.top - bm.bottom);
            }

            return;
        }

        var deltaX:Number = event.stageX - startX;

        // Move header selection.
        s = Sprite(selectionLayer.getChildByName("headerSelection"));
        if (s)
            s.x += deltaX;

        // Move header proxy.
        item = IListItemRenderer(listContent.getChildByName("headerDragProxy"));
        if (item)
            item.move(item.x + deltaX, item.y);

        startX += deltaX;

        var pt:Point = new Point(event.localX, event.localY);
        pt = DisplayObject(event.target).localToGlobal(pt);
        pt = listContent.globalToLocal(pt);

        for (i = 0; i < n; i++)
        {
            item = listItems[0][i];
            if (item.x < pt.x && pt.x < item.x + item.width)
            {
                // If the mouse pointer over the right half of the column, the
                // drop indicator should be shown before the next column.
                if (pt.x > item.x + Math.floor(item.width / 2))
                    i++;

                if (dropColumnIndex != i)
                {
                    dropColumnIndex = i;

                    if (!columnDropIndicator)
                    {
                        var dropIndicatorClass:Class
                            = getStyle("columnDropIndicatorSkin");
                        if (!dropIndicatorClass)
                            dropIndicatorClass = DataGridColumnDropIndicator;
                        columnDropIndicator = IFlexDisplayObject(
                            new dropIndicatorClass());

                        if (columnDropIndicator is ISimpleStyleClient)
                            ISimpleStyleClient(columnDropIndicator).styleName = this;

                        listContent.addChild(
                            DisplayObject(columnDropIndicator));
                    }

                    listContent.setChildIndex(
                        DisplayObject(columnDropIndicator),
                        listContent.numChildren - 1);
                    columnDropIndicator.visible = true;

                    if (dropColumnIndex < n)
                    {
                        item = listItems[0][dropColumnIndex];
                        columnDropIndicator.x = item.x - 2;
                    }
                    else
                    {
                        item = listItems[0][dropColumnIndex - 1];
                        columnDropIndicator.x = item.x
                            + visibleColumns[visibleColumns.length - 1].width - 2;
                    }

                    columnDropIndicator.y = 0;
                    columnDropIndicator.setActualSize(3, listContent.height);
                }
                break;
            }
        }
    }

    /**
     *  @private
     */
    private function columnDraggingMouseUpHandler(event:MouseEvent):void
    {
        if (!movingColumn)
            return;

        var origIndex:int = movingColumn.colNum;

        if (origIndex < dropColumnIndex)
            dropColumnIndex--;

        if (dropColumnIndex >= 0)
        {
            dropColumnIndex = Math.min(dropColumnIndex,
                    listItems[0].length - 1);

            // dropColumnIndex is actually the index into the listItems[0]
            // array.  Get the corresponding index into the _columns array.
            dropColumnIndex = listItems[0][dropColumnIndex].data.colNum;
        }

        // Shift columns.
        shiftColumns(origIndex, dropColumnIndex, event);

        systemManager.removeEventListener(MouseEvent.MOUSE_MOVE, columnDraggingMouseMoveHandler, true);
        systemManager.removeEventListener(MouseEvent.MOUSE_UP, columnDraggingMouseUpHandler, true);

        var proxy:IListItemRenderer =
            IListItemRenderer(listContent.getChildByName("headerDragProxy"));
        if (proxy)
            listContent.removeChild(DisplayObject(proxy));

        var s:Sprite = Sprite(selectionLayer.getChildByName("headerSelection"));
        if (s)
            selectionLayer.removeChild(s);

        if (columnDropIndicator)
            columnDropIndicator.visible = false;

        s = Sprite(listContent.getChildByName("columnDragOverlay"));
        if (s)
            listContent.removeChild(s);

        listContent.scrollRect = null;

        startX = NaN;
        movingColumn = null;
        dropColumnIndex = -1;
    }

    //--------------------------------------------------------------------------
    //
    //  Overridden event handlers
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     *  Catches any events from the model. Optimized for editing one item.
     *  Creates columns when there are none. Inherited from list.
     *  @param eventObj
     */
    override protected function collectionChangeHandler(event:Event):void
    {
        if(event is CollectionEvent)
        {
            var ceEvent:CollectionEvent = CollectionEvent(event)
            if(ceEvent.kind == CollectionEventKind.RESET)
            {
                if (generatedColumns)
                    generateCols();
                updateSortIndexAndDirection();
            }
            else if (ceEvent.kind == CollectionEventKind.REFRESH && !manualSort)
                updateSortIndexAndDirection();
            else
            {
                // if we get a remove while editing adjust the editPosition
                if (ceEvent.kind == CollectionEventKind.REMOVE)
                {
                    if (editedItemPosition)
                    {
                        if (collection.length == 0)
                        {
                            if (itemEditorInstance)
                                endEdit(DataGridEventReason.CANCELLED);
                            setEditedItemPosition(null); // nothing left to edit
                        }
                        else if (ceEvent.location <= editedItemPosition.rowIndex)
                        {
                            var curEditedItemPosition:Object = editedItemPosition;

                            // if the editor is up on the item going away, cancel the session
                            if (ceEvent.location == editedItemPosition.rowIndex && itemEditorInstance)
                                endEdit(DataGridEventReason.CANCELLED);

                            if (inEndEdit)
                                _editedItemPosition = { columnIndex : editedItemPosition.columnIndex, 
                                                    rowIndex : Math.max(0, editedItemPosition.rowIndex - ceEvent.items.length)};
                            else
                                setEditedItemPosition({ columnIndex : curEditedItemPosition.columnIndex, 
                                                    rowIndex : Math.max(0, curEditedItemPosition.rowIndex - ceEvent.items.length)});
                        }
                    }
                }
                else if (ceEvent.kind == CollectionEventKind.REPLACE)
                {
                    if (editedItemPosition)
                    {
                        // if the editor is up on the item going away, cancel the session
                        if (ceEvent.location == editedItemPosition.rowIndex && itemEditorInstance)
                            endEdit(DataGridEventReason.CANCELLED);
                    }
                }
            }
        }

        super.collectionChangeHandler(event);

        if (event is CollectionEvent)
        {
            // trace("ListBase collectionEvent");
            var ce:CollectionEvent = CollectionEvent(event);
            if (ce.kind == CollectionEventKind.ADD)
            {
                // added first item, generate columns for it if needed
                if (collection.length == 1)
                    if (generatedColumns)
                        generateCols();
            }
        }


//      if (event.eventName != "sort" && bRowsChanged)
//          invInitHeaders = true;
    }

    /**
     *  @private
     */
    override protected function mouseOverHandler(event:MouseEvent):void
    {
        if (movingColumn)
            return;

        var r:IListItemRenderer;
        if (enabled && sortableColumns && headerVisible && listItems.length 
            && !isPressed)
        {
            s = Sprite(listContent.getChildByName("sortArrowHitArea"));

            if (event.target != s)
                r = mouseEventToItemRenderer(event);

            var n:int = listItems[0].length;
            for (var i:int = 0; i < n; i++)
            {
                if (!r && s == event.target &&
                    visibleColumns[i].colNum == sortIndex)
                {
                    r = listItems[0][i];
                }

                if (r == listItems[0][i])
                {
                    if (visibleColumns[i].sortable)
                    {
                        var s:Sprite = Sprite(
                            selectionLayer.getChildByName("headerSelection"));
                        if (!s)
                        {
                            s = new FlexSprite();
                            s.name = "headerSelection";
                            selectionLayer.addChild(s);
                        }

                        var g:Graphics = s.graphics;
                        g.clear();
                        g.beginFill(getStyle("rollOverColor"));
                        g.drawRect(0, 0, visibleColumns[i].width, rowInfo[0].height - 0.5);
                        g.endFill();

                        s.x = r.x;
                        s.y = rowInfo[0].y;
                    }
                    return;
                }
            }
        }
        lastItemDown = null;

        super.mouseOverHandler(event);
    }

    /**
     *  @private
     */
    override protected function mouseOutHandler(event:MouseEvent):void
    {
        if (movingColumn)
            return;

        var r:IListItemRenderer;
        if (enabled && sortableColumns && headerVisible && listItems.length)
        {
            s = Sprite(listContent.getChildByName("sortArrowHitArea"));

            if (event.target != s)
                r = mouseEventToItemRenderer(event);

            var n:int = listItems[0].length;
            for (var i:int = 0; i < n; i++)
            {
                if (!r && s == event.target &&
                    visibleColumns[i].colNum == sortIndex)
                {
                    r = listItems[0][i];
                }

                if (r == listItems[0][i])
                {
                    if (visibleColumns[i].sortable)
                    {
                        var s:Sprite = Sprite(
                            selectionLayer.getChildByName("headerSelection"));
                        if (s)
                            selectionLayer.removeChild(s);
                    }
                    return;
                }
            }
        }
        lastItemDown = null;

        super.mouseOutHandler(event);
    }

    /**
     *  @private
     */
    override protected function mouseDownHandler(event:MouseEvent):void
    {
        // trace(">>mouseDownHandler");
        var r:IListItemRenderer;
        var s:Sprite;

        // find out if we hit the sort arrow
        s = Sprite(listContent.getChildByName("sortArrowHitArea"));

        if (event.target == s)
            r = listItems[0][sortIndex - visibleColumns[0].colNum]
        else
            r = mouseEventToItemRenderer(event);

        // if headers are visible and clickable for sorting
        if (enabled && (sortableColumns || draggableColumns)
                && headerVisible && listItems.length)
        {

            // find out if we clicked on a header
            var n:int = listItems[0].length;
            for (var i:int = 0; i < listItems[0].length; i++)
            {
                // if we did click on a header
                if (r == listItems[0][i])
                {
                    // dispose the editor
                    if (itemEditorInstance)
                        endEdit(DataGridEventReason.OTHER);

                    if (sortableColumns && visibleColumns[i].sortable)
                    {
                        lastItemDown = r;
                        s = Sprite(selectionLayer.getChildByName("headerSelection"));
                        if (!s)
                        {
                            s = new FlexSprite();
                            s.name = "headerSelection";
                            selectionLayer.addChild(s);
                        }

                        var g:Graphics = s.graphics;
                        g.clear();
                        g.beginFill(getStyle("selectionColor"));
                        g.drawRect(0, 0, visibleColumns[i].width, rowInfo[0].height - 0.5);
                        g.endFill();

                        s.x = r.x;
                        s.y = rowInfo[0].y;
                    }
                    isPressed = true;

                    // begin column dragging
                    if (draggableColumns)
                    {
                        startX = NaN;
                        systemManager.addEventListener(MouseEvent.MOUSE_MOVE, columnDraggingMouseMoveHandler, true);
                        systemManager.addEventListener(MouseEvent.MOUSE_UP, columnDraggingMouseUpHandler, true);
                        movingColumn = visibleColumns[i];
                    }

                    return;
                }
            }
        }
        lastItemDown = null;

        var isItemEditor:Boolean = itemRendererContains(itemEditorInstance, DisplayObject(event.target));

        // If it isn't an item renderer, or an item editor do default behavior
        if (!isItemEditor)
        {
            if (r && r.data)
            {
                lastItemDown = r;

                var pos:Point = itemRendererToIndices(r);

                if (headerVisible)
                    pos.y--;

                var bEndedEdit:Boolean = true;

                if (itemEditorInstance)
                {
                    bEndedEdit = endEdit(editedItemPosition.rowIndex == pos.y ?
                                         DataGridEventReason.NEW_COLUMN :
                                         DataGridEventReason.NEW_ROW);
                }

                // if we didn't end edit session, don't do default behavior (call super)
                if (!bEndedEdit)
                    return;
            }
            else
            {
                // trace("end edit?");
                if (itemEditorInstance)
                    endEdit(DataGridEventReason.OTHER);
            }

            super.mouseDownHandler(event);
        }
        // trace("<<mouseDownHandler");
    }

    /**
     *  @private
     */
    override protected function mouseUpHandler(event:MouseEvent):void
    {
        var dataGridEvent:DataGridEvent;
        var r:IListItemRenderer;
        var s:Sprite;
        var n:int;
        var i:int;

        // find out if we hit the sort arrow
        s = Sprite(listContent.getChildByName("sortArrowHitArea"));

        if (event.target == s)
            r = listItems[0][sortIndex - visibleColumns[0].colNum]
        else
            r = mouseEventToItemRenderer(event);

        if (enabled && (sortableColumns || draggableColumns)
                && collection && headerVisible && listItems.length)
        {
            n = listItems[0].length;
            for (i = 0; i < n; i++)
            {
                if (r == listItems[0][i])
                {
                    if (sortableColumns && visibleColumns[i].sortable && lastItemDown == r)
                    {
                        lastItemDown = null;
                        dataGridEvent = new DataGridEvent(DataGridEvent.HEADER_RELEASE, false, true);
                        // HEADER_RELEASE event is cancelable
                        dataGridEvent.columnIndex = visibleColumns[i].colNum;
                        dataGridEvent.dataField = visibleColumns[i].dataField;
                        dataGridEvent.itemRenderer = r;
                        dispatchEvent(dataGridEvent);
                    }
                    isPressed = false;
                    return;
                }
            }
        }

        if (movingColumn)
            return;

        super.mouseUpHandler(event);

        if (r && r.data && r != itemEditorInstance && lastItemDown == r)
        {
            var pos:Point = itemRendererToIndices(r);

            if (headerVisible)
                pos.y--;

            if (pos.y >= 0 && editable && displayableColumns[pos.x].editable && !dontEdit)
            {
                dataGridEvent = new DataGridEvent(DataGridEvent.ITEM_EDIT_BEGINNING, false, true);
                // ITEM_EDIT events are cancelable
                dataGridEvent.columnIndex = displayableColumns[pos.x].colNum;
                dataGridEvent.dataField = displayableColumns[pos.x].dataField;
                dataGridEvent.rowIndex = pos.y;
                dataGridEvent.itemRenderer = r;
                dispatchEvent(dataGridEvent);
            }
        }

        lastItemDown = null;
    }

    /**
     *  @private
     *  when the grid gets focus, focus an item renderer
     */
    override protected function focusInHandler(event:FocusEvent):void
    {
        // trace(">>DGFocusIn ", selectedIndex);

        if (event.target != this)
        {
            // trace("subcomponent got focus ignoring");
            // trace("<<DGFocusIn ");
            return;
        }

        if (losingFocus)
        {
            losingFocus = false;
            // trace("losing focus via tab");
            // trace("<<DGFocusIn ");
            return;
        }

        super.focusInHandler(event);

        if (editable && !isPressed) // don't do this if we're mouse focused
        {
            _editedItemPosition = lastEditedItemPosition;

            var foundOne:Boolean = editedItemPosition != null;

            // start somewhere
            if (!_editedItemPosition)
            {
                _editedItemPosition = { rowIndex: 0, columnIndex: 0 };

                for (;
                     _editedItemPosition.columnIndex != _columns.length;
                     _editedItemPosition.columnIndex++)
                {
                    // If the editedItemPosition is valid, focus it,
                    // otherwise find one.
                    if (_columns[_editedItemPosition.columnIndex].editable &&
                        _columns[_editedItemPosition.columnIndex].visible)
                    {
                        var row:Array = listItems[_editedItemPosition.rowIndex + (headerVisible ? 1 : 0)];
                        if (row && row[_editedItemPosition.columnIndex])
                        {
                            foundOne = true;
                            break;
                        }
                    }
                }
            }

            if (foundOne)
            {
                // trace("setting focus", _editedItemPosition.columnIndex, _editedItemPosition.rowIndex);
                setEditedItemPosition(_editedItemPosition);
            }

        }

        if (editable)
        {
            addEventListener(FocusEvent.KEY_FOCUS_CHANGE, keyFocusChangeHandler);
            addEventListener(MouseEvent.MOUSE_DOWN, mouseFocusChangeHandler);
        }
        // trace("<<DGFocusIn ");
    }

    /**
     *  @private
     *  when the grid loses focus, close the editor
     */
    override protected function focusOutHandler(event:FocusEvent):void
    {
        // trace(">>DGFocusOut " + itemEditorInstance + " " + event.relatedObject, event.target);
        if (event.target == this)
            super.focusOutHandler(event);

        // just leave if item editor is losing focus back to grid.  Usually happens
        // when someone clicks out of the editor onto a new item renderer.
        if (event.relatedObject == this && itemRendererContains(itemEditorInstance, DisplayObject(event.target)))
            return;

        // just leave if the cell renderer is losing focus to nothing while its editor exists. 
        // this happens when we make the cell renderer invisible as we put up the editor
        // if the renderer can have focus.
        if (event.relatedObject == null && itemRendererContains(editedItemRenderer, DisplayObject(event.target)))
            return;

        // just leave if item editor is losing focus to nothing.  Usually happens
        // when someone clicks out of the textfield
        if (event.relatedObject == null && itemRendererContains(itemEditorInstance, DisplayObject(event.target)))
            return;

        // however, if we're losing focus to anything other than the editor or the grid
        // hide the editor;
        if (itemEditorInstance && (!event.relatedObject || !itemRendererContains(itemEditorInstance, event.relatedObject)))
        {
            // trace("call endEdit from focus out");
            endEdit(DataGridEventReason.OTHER);
            removeEventListener(FocusEvent.KEY_FOCUS_CHANGE, keyFocusChangeHandler);
            removeEventListener(MouseEvent.MOUSE_DOWN, mouseFocusChangeHandler);
        }
        // trace("<<DGFocusOut " + itemEditorInstance + " " + event.relatedObject);
    }

    /**
     *  @private
     */
    private function deactivateHandler(event:Event):void
    {
        // if stage losing activation, set focus to DG so when we get it back
        // we popup an editor again
        if (itemEditorInstance)
        {
            endEdit(DataGridEventReason.OTHER);
            losingFocus = true;
            setFocus();
        }
    }

    /**
     *  @private
     */
    override protected function keyDownHandler(event:KeyboardEvent):void
    {
        if (itemEditorInstance || event.target != event.currentTarget)
            return;

        if (event.keyCode != Keyboard.SPACE)
            super.keyDownHandler(event);
        else if (caretIndex != -1)
        {
            var index:int = caretIndex + (headerVisible ? 1 : 0);
            if (index >= lockedRowCount)
                index -= verticalScrollPosition;

            var li:IListItemRenderer = listItems[index][0];
            if (selectItem(li, event.shiftKey, event.ctrlKey))
            {
                var evt:ListEvent = new ListEvent(ListEvent.CHANGE);
                evt.itemRenderer = li;
                var pt:Point = itemRendererToIndices(li);
                if(pt)
                {
                    evt.rowIndex = pt.y;
                    evt.columnIndex = pt.x;
                }
                dispatchEvent(evt);
            }
        }
    }

    /**
     *  @private
     *  used by ListBase.findString.  Shouldn't be used elsewhere
     *  because column's itemToLabel is preferred
     */
    override public function itemToLabel(data:Object):String
    {
        return displayableColumns[sortIndex == -1 ? 0 : sortIndex].itemToLabel(data);
    }

    //--------------------------------------------------------------------------
    //
    //  Event handlers
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    private function columnResizeMouseOverHandler(event:MouseEvent):void
    {
        if (!enabled || !resizableColumns)
            return;

        var target:DisplayObject = DisplayObject(event.target);
        var index:int = target.parent.getChildIndex(target);
        if (!visibleColumns[index].resizable)
            return;

        // hide the mouse, attach and show the cursor
        var stretchCursorClass:Class = getStyle("stretchCursor");
        resizeCursorID = CursorManager.setCursor(stretchCursorClass,
                                                 CursorManagerPriority.HIGH);
    }

    /**
     *  @private
     */
    private function columnResizeMouseOutHandler(event:MouseEvent):void
    {
        if (!enabled || !resizableColumns)
            return;

        var target:DisplayObject = DisplayObject(event.target);
        var index:int = target.parent.getChildIndex(target);
        if (!visibleColumns[index].resizable)
            return;

        CursorManager.removeCursor(resizeCursorID);
    }

    /**
     *  @private
     *  Indicates where the right side of a resized column appears.
     */
    private function columnResizeMouseDownHandler(event:MouseEvent):void
    {
        if (!enabled || !resizableColumns)
            return;

        var target:DisplayObject = DisplayObject(event.target);
        var index:int = target.parent.getChildIndex(target);
        if (!visibleColumns[index].resizable)
            return;

        if (itemEditorInstance)
            endEdit(DataGridEventReason.OTHER);

        startX = DisplayObject(event.target).x;

        var n:int = separators.length;
        for (var i:int = 0; i < n; i++)
        {
            if (separators[i] == event.target)
            {
                resizingColumn = visibleColumns[i];
                break;
            }
        }
        if (!resizingColumn)
            return;

        minX = listItems[0][i].x + resizingColumn.minWidth;

        isPressed = true;

        systemManager.addEventListener(MouseEvent.MOUSE_MOVE, columnResizingHandler, true);
        systemManager.addEventListener(MouseEvent.MOUSE_UP, columnResizeMouseUpHandler, true);

        var resizeSkinClass:Class = getStyle("columnResizeSkin");
        resizeGraphic = new resizeSkinClass();
        listContent.addChild(DisplayObject(resizeGraphic));
        resizeGraphic.move(DisplayObject(event.target).x, 0);
        resizeGraphic.setActualSize(resizeGraphic.measuredWidth,
                                    unscaledHeight);
    }

    /**
     *  @private
     */
    private function columnResizingHandler(event:MouseEvent):void
    {
        if (!MouseEvent(event).buttonDown)
            columnResizeMouseUpHandler(event);
        
        var vsw:int = verticalScrollBar ? verticalScrollBar.width : 0;

        var pt:Point = new Point(event.stageX, event.stageY);
        pt = globalToLocal(pt);
        resizeGraphic.move(Math.min(Math.max(minX, pt.x),
                           unscaledWidth - separators[0].width - vsw), 0);
    }

    /**
     *  @private
     *  Determines how much to resize the column.
     */
    private function columnResizeMouseUpHandler(event:MouseEvent):void
    {
        if (!enabled || !resizableColumns)
            return;

        isPressed = false;

        systemManager.removeEventListener(MouseEvent.MOUSE_MOVE, columnResizingHandler, true);
        systemManager.removeEventListener(MouseEvent.MOUSE_UP, columnResizeMouseUpHandler, true);

        listContent.removeChild(DisplayObject(resizeGraphic));

        CursorManager.removeCursor(resizeCursorID);

        var c:DataGridColumn = resizingColumn;
        resizingColumn = null;

        // need to find the visible column index here.
        var n:int = visibleColumns.length;
        var i:int;
        for (i = 0; i < n; i++)
        {
            if (c == visibleColumns[i])
                break;
        }
        if (i >= visibleColumns.length)
            return;

        var vsw:int = verticalScrollBar ? verticalScrollBar.width : 0;

        var pt:Point = new Point(event.stageX, event.stageY);
        pt = globalToLocal(pt);

        // resize the column
        var widthChange:Number = Math.min(Math.max(minX, pt.x),
            unscaledWidth - separators[0].width - vsw) - startX;
        resizeColumn(i, Math.floor(c.width + widthChange));

        // event
        var dataGridEvent:DataGridEvent =
            new DataGridEvent(DataGridEvent.COLUMN_STRETCH);
        dataGridEvent.columnIndex = c.colNum;
        dataGridEvent.dataField = c.dataField;
        dataGridEvent.localX = pt.x;
        dispatchEvent(dataGridEvent);
    }

    /**
     *  @private
     */
    private function editorMouseDownHandler(event:MouseEvent):void
    {
        if (!owns(DisplayObject(event.target)))
            endEdit(DataGridEventReason.OTHER);
    }

    /**
     *  @private
     */
    private function editorKeyDownHandler(event:KeyboardEvent):void
    {
        // ESC just kills the editor, no new data
        if (event.keyCode == Keyboard.ESCAPE)
        {
            endEdit(DataGridEventReason.CANCELLED);
        }
        else if (event.ctrlKey && event.charCode == 46)
        {   // Check for Ctrl-.
            endEdit(DataGridEventReason.CANCELLED);
        }
        else if (event.charCode == Keyboard.ENTER && event.keyCode != 229)
        {
            // multiline editors can take the enter key.
            if (columns[_editedItemPosition.columnIndex].editorUsesEnterKey)
                return;

            // Enter edits the item, moves down a row
            // The 229 keyCode is for IME compatability. When entering an IME expression,
            // the enter key is down, but the keyCode is 229 instead of the enter key code.
            // Thanks to Yukari for this little trick...
            if (endEdit(DataGridEventReason.NEW_ROW) && !dontEdit)
                findNextEnterItemRenderer(event);
        }
    }

    /**
     *  @private
     */
    private function editorStageResizeHandler(event:Event):void
    {
        if (!owns(DisplayObject(event.target)))
            endEdit(DataGridEventReason.OTHER);
    }

    /**
     *  @private
     *  find the next item renderer down from the currently edited item renderer, and focus it.
     */
    private function findNextEnterItemRenderer(event:KeyboardEvent):void
    {
        // some other thing like a collection change has changed the
        // position, so bail and wait for commit to reset the editor.
        if (_proposedEditedItemPosition !== undefined)
            return;

        _editedItemPosition = lastEditedItemPosition;

        var rowIndex:int = _editedItemPosition.rowIndex;
        var columnIndex:int = _editedItemPosition.columnIndex;
        // modify direction with SHIFT (up or down)
        var newIndex:int = _editedItemPosition.rowIndex +
                           (event.shiftKey ? -1 : 1);
        // only move if we're within range
        if (newIndex < collection.length && newIndex >= 0)
            rowIndex = newIndex;

        // send event to create the new one
        var dataGridEvent:DataGridEvent =
            new DataGridEvent(DataGridEvent.ITEM_EDIT_BEGINNING, false, true);
            // ITEM_EDIT events are cancelable
        dataGridEvent.columnIndex = columnIndex;
        dataGridEvent.dataField = _columns[columnIndex].dataField;
        dataGridEvent.rowIndex = rowIndex;
        dispatchEvent(dataGridEvent);
    }

    /**
     *  @private
     *  This gets called when the tab key is hit.
     */
    private function mouseFocusChangeHandler(event:MouseEvent):void
    {
        // trace("mouseFocus handled by " + this);

        if (itemEditorInstance &&
            !event.isDefaultPrevented() &&
            itemRendererContains(itemEditorInstance, DisplayObject(event.target)))
        {
            event.preventDefault();
        }
    }

    /**
     *  @private
     *  This gets called when the tab key is hit.
     */
    private function keyFocusChangeHandler(event:FocusEvent):void
    {
        // trace("tabHandled by " + this);

        if (event.keyCode == Keyboard.TAB &&
            ! event.isDefaultPrevented() &&
            findNextItemRenderer(event.shiftKey))
        {
            event.preventDefault();
        }
    }

    /**
     *  @private
     *  Hides the itemEditorInstance.
     */
    private function itemEditorFocusOutHandler(event:FocusEvent):void
    {
        // trace("itemEditorFocusOut " + event.relatedObject);
        if (event.relatedObject && contains(event.relatedObject))
            return;

        // ignore textfields losing focus on mousedowns
        if (!event.relatedObject)
            return;

        // trace("endEdit from itemEditorFocusOut");
        if (itemEditorInstance)
            endEdit(DataGridEventReason.OTHER);
    }

    /**
     *  @private
     */
    private function itemEditorItemEditBeginningHandler(event:DataGridEvent):void
    {
        // trace("itemEditorItemEditBeginningHandler");
        if (!event.isDefaultPrevented())
            setEditedItemPosition({columnIndex: event.columnIndex, rowIndex: event.rowIndex});
        else if (!itemEditorInstance)
        {
            _editedItemPosition = null;
            // return focus to the grid w/o selecting an item
            editable = false;
            setFocus();
            editable = true;
        }
    }

    /**
     *  @private
     *  focus an item renderer in the grid - harder than it looks
     */
    private function itemEditorItemEditBeginHandler(event:DataGridEvent):void
    {
        // weak reference for deactivation
        if (stage)
            stage.addEventListener(Event.DEACTIVATE, deactivateHandler, false, 0, true);

        // if not prevented and if data is not null (might be from dataservices)
        if (!event.isDefaultPrevented() && listItems[actualRowIndex][actualColIndex].data != null)
        {
            createItemEditor(event.columnIndex, event.rowIndex);

            if (editedItemRenderer is IDropInListItemRenderer && itemEditorInstance is IDropInListItemRenderer)
                IDropInListItemRenderer(itemEditorInstance).listData = IDropInListItemRenderer(editedItemRenderer).listData;
            // if rendererIsEditor, don't apply the data as the data may have already changed in some way.
            // This can happen if clicking on a checkbox rendererIsEditor as the checkbox will try to change
            // its value as we try to stuff in an old value here.
            if (!columns[event.columnIndex].rendererIsEditor)
                itemEditorInstance.data = editedItemRenderer.data;

            if (itemEditorInstance is IInvalidating)
                IInvalidating(itemEditorInstance).validateNow();

            if (itemEditorInstance is IIMESupport)
                IIMESupport(itemEditorInstance).imeMode =
                    (columns[event.columnIndex].imeMode == null) ? _imeMode : columns[event.columnIndex].imeMode;

            var fm:IFocusManager = focusManager;
            // trace("setting focus to item editor");
            if (itemEditorInstance is IFocusManagerComponent)
                fm.setFocus(IFocusManagerComponent(itemEditorInstance));
            fm.defaultButtonEnabled = false;

            var event:DataGridEvent =
                new DataGridEvent(DataGridEvent.ITEM_FOCUS_IN);
            event.columnIndex = _editedItemPosition.columnIndex;
            event.rowIndex = _editedItemPosition.rowIndex;
                event.itemRenderer = itemEditorInstance;
            dispatchEvent(event);
        }
    }

    /**
     *  @private
     */
    private function itemEditorItemEditEndHandler(event:DataGridEvent):void
    {
        if (!event.isDefaultPrevented())
        {
            var bChanged:Boolean = false;

            if (event.reason == DataGridEventReason.NEW_COLUMN)
            {
                if (!collectionUpdatesDisabled)
                {
                    collection.disableAutoUpdate();
                    collectionUpdatesDisabled = true;
                }
            }
            else
            {
                if (collectionUpdatesDisabled)
                {
                    collection.enableAutoUpdate();
                    collectionUpdatesDisabled = false;
                }
            }

            if (itemEditorInstance && event.reason != DataGridEventReason.CANCELLED)
            {
                var newData:Object = itemEditorInstance[_columns[event.columnIndex].editorDataField];
                var property:String = _columns[event.columnIndex].dataField;
                var data:Object = event.itemRenderer.data;
                var typeInfo:String = "";
                for each(var variable:XML in describeType(data).variable)
                {
                    if (property == variable.@name.toString())
                    {
                        typeInfo = variable.@type.toString();
                        break;
                    }
                }

                if (typeInfo == "String")
                {
                    if (!(newData is String))
                        newData = newData.toString();
                }
                else if (typeInfo == "uint")
                {
                    if (!(newData is uint))
                        newData = uint(newData);
                }
                else if (typeInfo == "int")
                {
                    if (!(newData is int))
                        newData = int(newData);
                }
                else if (typeInfo == "Number")
                {
                    if (!(newData is int))
                        newData = Number(newData);
                }
                if (data[property] != newData)
                {
                    bChanged = true;
                    data[property] = newData;
                }
                if (bChanged && !(data is IPropertyChangeNotifier))
                {
                    collection.itemUpdated(data, property);
                }
                if (event.itemRenderer is IDropInListItemRenderer)
                {
                    var listData:DataGridListData = DataGridListData(IDropInListItemRenderer(event.itemRenderer).listData);
                    listData.label = _columns[event.columnIndex].itemToLabel(data);
                    IDropInListItemRenderer(event.itemRenderer).listData = listData;
                }
                event.itemRenderer.data = data;
            }
        }
        else
        {
            if (event.reason != DataGridEventReason.OTHER)
            {
                if (itemEditorInstance && _editedItemPosition)
                {
                    // edit session is continued so restore focus and selection
                    if (selectedIndex != _editedItemPosition.rowIndex)
                        selectedIndex = _editedItemPosition.rowIndex;
                    var fm:IFocusManager = focusManager;
                    // trace("setting focus to itemEditorInstance", selectedIndex);
                    if (itemEditorInstance is IFocusManagerComponent)
                        fm.setFocus(IFocusManagerComponent(itemEditorInstance));
                }
            }
        }

        if (event.reason == DataGridEventReason.OTHER || !event.isDefaultPrevented())
        {
            destroyItemEditor();
        }
    }

    /**
     *  @private
     */
    private function headerReleaseHandler(event:DataGridEvent):void
    {
        if (!event.isDefaultPrevented())
        {
            manualSort = true;
            sortByColumn(event.columnIndex);
            manualSort = false;
        }
    }

    /**
     *  @private
     */
    override protected function mouseWheelHandler(event:MouseEvent):void
    {
        if (itemEditorInstance)
            endEdit(DataGridEventReason.OTHER);

        super.mouseWheelHandler(event);
    }

    /**
     *  @private
     */
    mx_internal function getSeparators():Array
    {
        return separators;
    }

}

}
