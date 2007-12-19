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
import flash.events.Event;
import flash.events.KeyboardEvent;
    
import mx.automation.Automation;
import mx.automation.IAutomationObject;
import mx.automation.tabularData.ListTabularData;
import mx.controls.List;
import mx.controls.listClasses.IListItemRenderer;
import mx.core.mx_internal;
import mx.events.ListEvent;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  List control.
 * 
 *  @see mx.controls.List 
 *
 */
public class ListAutomationImpl extends ListBaseAutomationImpl 
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
        Automation.registerDelegateClass(List, ListAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj List object to be automated.     
     */
    public function ListAutomationImpl(obj:List)
    {
        super(obj);
        
        obj.addEventListener(ListEvent.ITEM_EDIT_BEGIN, itemEditBeginHandler, false, 0, true);
    }

    /**
     *  @private
     *  storage for the owner component
     */
    protected function get list():List
    {
        return uiComponent as List;
    }
    
    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

    /**
     * @private
     */
    override public function replayAutomatableEvent(event:Event):Boolean
    {
        switch (event.type)
        {
            case ListEvent.ITEM_EDIT_BEGIN:
            {
                var input:ListEvent = event as ListEvent;
                var ev:ListEvent = new ListEvent(ListEvent.ITEM_EDIT_BEGINNING);
                ev.itemRenderer = input.itemRenderer;
                ev.rowIndex = input.rowIndex;
                ev.columnIndex = 0;
                return list.dispatchEvent(ev);
            }

            default:
            {
                return super.replayAutomatableEvent(event);
            }

        }
    }
        
    /**
     * @private
     */
    override public function getAutomationChildAt(index:int):IAutomationObject
    {
        var listItems:Array = list.rendererArray;
        var numCols:int = listItems[0].length;
        var row:uint = uint(numCols == 0 ? 0 : index / numCols);
        var col:uint = uint(numCols == 0 ? index : index % numCols);

        var item:IListItemRenderer = listItems[row][col];
        if (list.itemEditorInstance && item == list.editedItemRenderer)
            return list.itemEditorInstance as IAutomationObject;
        else
            return item as IAutomationObject;
    }
 
 
 	/**
     * @private
     */
    override public function getItemAutomationIndex(delegate:IAutomationObject):String
    {
    	var item:IListItemRenderer = delegate as IListItemRenderer;
        if (item == list.itemEditorInstance && list.editedItemPosition)
            item = list.editedItemRenderer;
        
        return super.getItemAutomationIndex(item as IAutomationObject);
    }
   
    /**
     *  A matrix of the automationValues of each item in the grid. The return value
     *  is an array of rows, each of which is an array of item renderers (row-major).
     */
    override public function get automationTabularData():Object
    {
        return new ListTabularData(list);
    }

	/**
	 *  @private
	 *  Prevents recording of selection event if the list is editable.
	 */
    override protected function recordListItemSelectEvent(item:IListItemRenderer,
	                                                     trigger:Event, 
	                                                     cacheable:Boolean=true):void
    {
    	// if the list is editable skip selection recording
    	if(list.editable)
    		return;
    
    	super.recordListItemSelectEvent(item, trigger, cacheable);
    }
    
    //--------------------------------------------------------------------------
    //
    //  Event handlers
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    private function itemEditBeginHandler(event:ListEvent):void 
    {
        event.columnIndex = 0;
        recordAutomatableEvent(event, true);
    }

    /**
     *  @private
     */
    override protected function keyDownHandler(event:KeyboardEvent):void
    {
        if (list.itemEditorInstance)
            return;

        super.keyDownHandler(event);
    }

    
}
}
