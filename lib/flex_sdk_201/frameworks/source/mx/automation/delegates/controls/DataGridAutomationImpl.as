////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation.delegates.controls 
{
import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.Event;
import flash.events.FocusEvent;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.ui.Keyboard;

import mx.automation.Automation;
import mx.automation.IAutomationManager;
import mx.automation.IAutomationObject;
import mx.automation.IAutomationObjectHelper;
import mx.automation.tabularData.DataGridTabularData;
import mx.automation.events.AutomationDragEvent;
import mx.automation.events.ListItemSelectEvent;
import mx.controls.DataGrid;
import mx.controls.listClasses.IListItemRenderer;
import mx.core.IFlexDisplayObject
import mx.core.mx_internal;
import mx.events.IndexChangedEvent;
import mx.events.DataGridEvent;
import mx.events.DragEvent;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  DataGrid control.
 * 
 *  @see mx.controls.DataGrid 
 *
 */
public class DataGridAutomationImpl extends ListBaseAutomationImpl 
{
    include "../../../core/Version.as";
	
	//--------------------------------------------------------------------------
	//
	//  Class methods
	//
	//--------------------------------------------------------------------------

	/**
     *  Registers the delegate class for a component class with automation manager.
	 */
	public static function init(root:DisplayObject):void
	{
		Automation.registerDelegateClass(DataGrid, DataGridAutomationImpl);
	}	

    /**
     *  Constructor.
     * @param obj DataGrid object to be automated.     
     */
  	public function DataGridAutomationImpl(obj:DataGrid)
  	{
  		super(obj);
  		
  		
  		obj.addEventListener(IndexChangedEvent.HEADER_SHIFT, headerShiftHandler, false, 0, true);
  		obj.addEventListener(DataGridEvent.HEADER_RELEASE, headerReleaseHandler, false, 0, true);
  		obj.addEventListener(DataGridEvent.COLUMN_STRETCH, columnStretchHandler, false, 0, true);
  		
  		obj.addEventListener(DataGridEvent.ITEM_EDIT_BEGIN, itemEditHandler, false, 0, true);
  	}

    /**
     *  @private
	 *  storage for the owner component
     */
 	protected function get grid():DataGrid
 	{
 		return uiComponent as DataGrid;
 	}

	/**
     * @private
     */
    override public function getAutomationChildAt(index:int):IAutomationObject
    {
    	var listItems:Array = grid.rendererArray;
        var numCols:int = listItems[0].length;
        var row:uint = uint(numCols == 0 ? 0 : index / numCols);
        var col:uint = uint(numCols == 0 ? index : index % numCols);
        var item:IListItemRenderer = listItems[row][col];
        
        if (grid.itemEditorInstance &&
            grid.editedItemPosition &&
            item == grid.editedItemRenderer)
        {
            return grid.itemEditorInstance as IAutomationObject;
        }

        return  item as IAutomationObject;
    }

	/**
     * @private
     */
    override public function getItemAutomationIndex(delegate:IAutomationObject):String
    {
    	var item:IListItemRenderer = delegate as IListItemRenderer;
        if (item == grid.itemEditorInstance && grid.editedItemPosition)
            item = grid.editedItemRenderer;
        var row:int = grid.itemRendererToIndex(item);
        return (row < 0
                ? getItemAutomationName(delegate)
                : grid.gridColumnMap[item.name].dataField + ":" + row);
    }

	/**
	 *  @private
	 */
    override public function getItemAutomationValue(item:IAutomationObject):String
    {
        return getItemAutomationNameOrValueHelper(item, false);
    }

	/**
	 *  @private
	 */
    override public function getItemAutomationName(item:IAutomationObject):String
    {
        return getItemAutomationNameOrValueHelper(item, true);
    }

	/**
	 *  @private
	 */
    private function getItemAutomationNameOrValueHelper(delegate:IAutomationObject,
                                                        useName:Boolean):String
    {
		var result:Array = [];
		var item:IListItemRenderer = delegate as IListItemRenderer;

        if (item == grid.itemEditorInstance)
            item = grid.editedItemRenderer;

        var row:int = grid.itemRendererToIndex(item);
        if (row == int.MIN_VALUE)
            return null;
            
		row = row < grid.lockedRowCount ?
				   row :
				   row - grid.verticalScrollPosition;            

        var isHeader:Boolean = grid.headerVisible && row == -1;
        if (row >= 0)
        {
            if (grid.headerVisible)
                ++row;
        }
        else if (isHeader)
            row = 0;
        
        var listItems:Array = grid.rendererArray;
        for (var col:int = 0; col < listItems[row].length; col++)
        {
            var i:IListItemRenderer = listItems[row][col];
            if(i == grid.editedItemRenderer)
            	i = grid.itemEditorInstance;
            var itemDelegate:IAutomationObject = i as IAutomationObject;
            var s:String = (useName
                            ? itemDelegate.automationName
                            : itemDelegate.automationValue.join(" | "));
            result.push(i == item ? "*" + s + "*" : s);
        }
        return (isHeader
                ? "[" + result.join("] | [") + "]"
                : result.join(" | "));
    }

	/**
	 *  @private
	 *  Prevents recording of selection event if the grid is editable.
	 */
    override protected function recordListItemSelectEvent(item:IListItemRenderer,
	                                                     trigger:Event, 
	                                                     cacheable:Boolean=true):void
    {
    	// if the list is editable skip selection recording
    	if(grid.editable)
    		return;
    
    	super.recordListItemSelectEvent(item, trigger, cacheable);
    }

    /**
     *  @private
     */
    override public function replayAutomatableEvent(interaction:Event):Boolean
    {
        var help:IAutomationObjectHelper = Automation.automationObjectHelper;
        var mouseEvent:MouseEvent;
        switch (interaction.type)
        {
	        case "headerShift":
			{
                var icEvent:IndexChangedEvent = IndexChangedEvent(interaction);
                grid.shiftColumns(icEvent.oldIndex, icEvent.newIndex);
	            return true;
			}

	        case DataGridEvent.HEADER_RELEASE:
			{
				var listItems:Array = grid.rendererArray;
	            var c:IListItemRenderer = listItems[0][DataGridEvent(interaction).columnIndex];
                return help.replayClick(c);
			}

	        case DataGridEvent.COLUMN_STRETCH:
			{
	            var s:IFlexDisplayObject = grid.getSeparators()[DataGridEvent(interaction).columnIndex];
	            s.dispatchEvent(new MouseEvent(MouseEvent.MOUSE_DOWN));
				// localX needs to be passed in the constructor
				// to get stageX value computed.
	            mouseEvent = new MouseEvent(MouseEvent.MOUSE_UP, 
	            			true, // bubble 
	            			false, // cancellable 
	            			DataGridEvent(interaction).localX, 
	            			20, // dummy value 
	            			uiComponent as InteractiveObject );
                return help.replayMouseEvent(uiComponent, mouseEvent);
			}

			case DataGridEvent.ITEM_EDIT_BEGIN:
			{
				var de:DataGridEvent = new DataGridEvent(DataGridEvent.ITEM_EDIT_BEGINNING);
				var input:DataGridEvent = interaction as DataGridEvent;
				de.itemRenderer = input.itemRenderer;
                de.rowIndex = input.rowIndex;
                de.columnIndex = input.columnIndex;
				uiComponent.dispatchEvent(de);
			}

            case ListItemSelectEvent.DESELECT:
            case ListItemSelectEvent.MULTI_SELECT:
            case ListItemSelectEvent.SELECT:
	        default:
			{
	            return super.replayAutomatableEvent(interaction);
			}
        }
    }

	/**
     *  A matrix of the automationValues of each item in the grid. The return value
     *  is an array of rows, each of which is an array of item renderers (row-major).
     */
    override public function get automationTabularData():Object
    {
    	return  new DataGridTabularData(grid);
	}
	
	/**
	 *  @private
	 */
	override protected function keyDownHandler(event:KeyboardEvent):void
	{
		if (grid.itemEditorInstance || event.target != event.currentTarget)
			return;

        super.keyDownHandler(event);
	}
	
	/**
	 *  @private
	 */
	private function columnStretchHandler(event:DataGridEvent):void 
	{
       recordAutomatableEvent(event);
	}

	/**
	 *  @private
	 */
	private function headerReleaseHandler(event:DataGridEvent):void 
	{
       recordAutomatableEvent(event);
	}
	
	/**
	 *  @private
	 */
	private function headerShiftHandler(event:IndexChangedEvent):void 
	{
        if (event.triggerEvent)
            recordAutomatableEvent(event);
	}
	
	/**
	 *  @private
	 */
	private function itemEditHandler(event:DataGridEvent):void
	{
		recordAutomatableEvent(event, true);	
	}
	
    /**
     *  @private
     */
	override protected function dragDropHandler(event:DragEvent):void
	{
		if(dragScrollEvent)
		{
			recordAutomatableEvent(dragScrollEvent);
			dragScrollEvent=null;
		}

        var am:IAutomationManager = Automation.automationManager;
    	var index:int = grid.calculateDropIndex(event);
		var drag:AutomationDragEvent = new AutomationDragEvent(event.type);
		drag.action = event.action;

    	if (grid.dataProvider && index != grid.dataProvider.length)
    	{
			//increment the index if headers are being shown
    		if(grid.headerVisible)
    			++index;
    		
	        if (index >= grid.lockedRowCount)
	            index -= grid.verticalScrollPosition;
	
	        var rc:Number = grid.rendererArray.length;
	        if (index >= rc)
	            index = rc - 1;
	        
	        if (index < 0)
	            index = 0;

	    	if(grid.rendererArray && grid.rendererArray[0] && grid.rendererArray[0].length)
		        index = index * grid.rendererArray[0].length;
	
	    	drag.draggedItem = getAutomationChildAt(index);
	    }

       	preventDragDropRecording = false;
		am.recordAutomatableEvent(uiAutomationObject, drag);
		preventDragDropRecording = true;
	}
	
    /**
     *  @private
     */
	override protected function mouseDownHandler(event:MouseEvent):void
	{
    	var listItems:Array = grid.rendererArray;
        var r:IListItemRenderer = grid.getItemRendererForMouseEvent(event);
		var headerClick:Boolean = false;
        // if headers are visible and clickable for sorting
        if (grid.enabled && (grid.sortableColumns || grid.draggableColumns)
                && grid.headerVisible && listItems.length)
        {

            // find out if we clicked on a header
            var n:int = listItems[0].length;
            for (var i:int = 0; i < listItems[0].length; i++)
            {
                // if we did click on a header
                if (r == listItems[0][i])
                {
            		headerClick = true;    	
                }
            }
        }
    
        if(!headerClick)
			super.mouseDownHandler(event);
	}
		
	
}
}
