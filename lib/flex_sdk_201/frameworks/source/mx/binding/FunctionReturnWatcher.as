////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.binding
{

import flash.events.Event;
import flash.events.IEventDispatcher;
import mx.core.EventPriority;
import mx.core.mx_internal;

use namespace mx_internal;

[ExcludeClass]

/**
 *  @private
 */
public class FunctionReturnWatcher extends Watcher
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 *  Constructor.
	 */
	public function FunctionReturnWatcher(functionName:String, document:Object,
										  parameterFunction:Function,
										  events:Object)
    {
		super();

        this.functionName = functionName;
        this.document = document;
        this.parameterFunction = parameterFunction;
        this.events = events;
    }

	//--------------------------------------------------------------------------
	//
	//  Variables
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
     *  The name of the property, used to actually get the property
	 *  and for comparison in propertyChanged events.
     */
    private var functionName:String;
    
	/**
 	 *  @private
     *  The document is what we need to use toe execute the parameter function.
     */
    private var document:Object;
    
	/**
 	 *  @private
     *  The function that will give us the parameters for calling the function.
     */
    private var parameterFunction:Function;
    
    /**
 	 *  @private
     *  The events that indicate the property has changed.
     */
    private var events:Object;
    
	/**
	 *  @private
     *  The parent object of this function.
     */
    private var parentObj:Object;
    
	/**
	 *  @private
     *  The watcher holding onto the parent object.
     */
    public var parentWatcher:Watcher;

	//----------------------------------
	//  functionGetter
	//----------------------------------

    /**
     *  Storage for the functionGetter property.
     */
    private var _functionGetter:Function;

    /**
     *  The function for accessing the parent's property.
     */
    public function set functionGetter(value:Function):void
    {
        _functionGetter = value;
    }
    
	//--------------------------------------------------------------------------
	//
	//  Overridden methods
	//
	//--------------------------------------------------------------------------

    /**
 	 *  @private
     */
    override public function updateParent(parent:Object):void
    {
        if (!(parent is Watcher))
            setupParentObj(parent);
        
		else if (parent == parentWatcher)
            setupParentObj(parentWatcher.value);
        
		updateFunctionReturn();
    }

    /**
 	 *  @private
     */
    override protected function shallowClone():Watcher
    {
        var clone:FunctionReturnWatcher = new FunctionReturnWatcher(functionName, document,
                                                                    parameterFunction, events);

        if (_functionGetter != null)
        {
            clone.functionGetter = _functionGetter;
        }

        return clone;
    }

	//--------------------------------------------------------------------------
	//
	//  Methods
	//
	//--------------------------------------------------------------------------

    /**
 	 *  @private
     *  Get the new return value of the function.
     */
    public function updateFunctionReturn():void
    {
        wrapUpdate(function():void
		{
            if (_functionGetter != null)
            {
                value = _functionGetter(functionName).apply(parentObj,
                                                            parameterFunction.apply(document));
            }
            else
            {
                value = parentObj[functionName].apply(parentObj,
                                                      parameterFunction.apply(document));
            }
			
			updateChildren();
		});
    }

    /**
 	 *  @private
     */
    private function setupParentObj(newParent:Object):void
    {
		var eventDispatcher:IEventDispatcher;
        var p:String;

        if (parentObj != null &&
            parentObj is IEventDispatcher &&
            events != null)
        {
            eventDispatcher = parentObj as IEventDispatcher;
            
			for (p in events)
            {
                eventDispatcher.removeEventListener(p, eventHandler);
            }
        }
        
		parentObj = newParent;
        
        if (parentObj != null &&
            parentObj is IEventDispatcher &&
            events != null)
        {
            eventDispatcher = parentObj as IEventDispatcher;

            for (p in events)
            {
                if (p != "__NoChangeEvent__")
				{
                    eventDispatcher.addEventListener(
						p, eventHandler, false, EventPriority.BINDING, true);
				}
            }
        }
    }

	//--------------------------------------------------------------------------
	//
	//  Event handlers
	//
	//--------------------------------------------------------------------------

    /**
 	 *  @private
     */
    public function eventHandler(event:Event):void
    {
        updateFunctionReturn();

        notifyListeners(events[event.type]);
    }
}

}
