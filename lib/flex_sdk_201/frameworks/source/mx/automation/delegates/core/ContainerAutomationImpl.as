////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation.delegates.core
{

import flash.display.DisplayObject;
import flash.events.Event;
import flash.events.MouseEvent;

import mx.utils.StringUtil;
import mx.automation.Automation;
import mx.automation.AutomationIDPart;
import mx.automation.tabularData.ContainerTabularData;
import mx.automation.IAutomationObject;
import mx.automation.IAutomationObjectHelper;
import mx.core.Container;
import mx.controls.scrollClasses.ScrollBar;
import mx.events.ChildExistenceChangedEvent;
import mx.events.ScrollEvent;
import mx.events.ScrollEventDetail;
import mx.events.ScrollEventDirection;
import mx.core.EventPriority;
import mx.core.mx_internal;
import mx.events.ScrollEventDetail;

use namespace mx_internal;


[Mixin]
/**
 * 
 *  Defines the methods and properties required to perform instrumentation for the 
 *  Container class. 
 * 
 *  @see mx.core.Container
 *  
 */
public class ContainerAutomationImpl extends UIComponentAutomationImpl
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
        Automation.registerDelegateClass(Container, ContainerAutomationImpl);
    }   
    
    /**
     *  Constructor.
     * @param obj Container object to be automated.     
     */
    public function ContainerAutomationImpl(obj:Container)
    {
        super(obj);
    
        obj.addEventListener(ScrollEvent.SCROLL, scroll_eventHandler, false, 0, true);
        obj.addEventListener(MouseEvent.MOUSE_WHEEL, mouseWheelHandler,
                false, EventPriority.DEFAULT+1, true );
    }

    /**
     *  @private
     */
    private function get container():Container
    {
        return uiComponent as Container;
    }

    /**
     *  @private
     *  Holds the previous scroll event object. This is used to prevent recording
     *  multiple scroll events.
     */
    private var previousEvent:ScrollEvent;
    
    /**
     *  @private
     *  Flag used to control recording of scroll events.
     *  MouseWheel events are recorded as they are handled specially by the containers.
     *  The scrollEvent generated doesnot contain proper information for playback. Hence
     *  we record and playback mouseWheel events.
     */
    private var skipScrollEvent:Boolean = false;
    
    //----------------------------------
    //  automationName
    //----------------------------------

    /**
     *  @private
     */
    override public function get automationName():String
    {
        return container.label || super.automationName;
    }

    //----------------------------------
    //  automationValue
    //----------------------------------

    /**
     *  @private
     */
    override public function get automationValue():Array
    {
        if (container.label && container.label.length != 0)
            return [ container.label ];
        
        var result:Array = [];
        
        var n:int = numAutomationChildren;
        for (var i:int = 0; i < n; i++)
        {
            var child:IAutomationObject = getAutomationChildAt(i);
            var x:Array = child.automationValue;
            if (x && x.length != 0)
                result.push(x);
        }
        
        return result;
    }

    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override public function replayAutomatableEvent(event:Event):Boolean
    {
        if (event is ScrollEvent)
        {
            var scrollEvent:ScrollEvent = new ScrollEvent(ScrollEvent.SCROLL);
            
            var se:ScrollEvent = ScrollEvent(event);
            
            var vReplayer:IAutomationObject = (container.verticalScrollBar as IAutomationObject);
            var hReplayer:IAutomationObject = (container.horizontalScrollBar as IAutomationObject);
            if (se.direction == ScrollEventDirection.VERTICAL && vReplayer)
            {
                vReplayer.replayAutomatableEvent(se);
            }
            else if (se.direction == ScrollEventDirection.HORIZONTAL && hReplayer)
            {
                hReplayer.replayAutomatableEvent(se);
            }
            else
            {
//                throw new Error(StringUtil.substitute(resourceScrollDirection, se.direction));
            }

            return true;
        }
        else if (event is MouseEvent && event.type == MouseEvent.MOUSE_WHEEL)
        {
            var help:IAutomationObjectHelper = Automation.automationObjectHelper;
            help.replayMouseEvent(uiComponent, event as MouseEvent);
            return true;
        }

        return super.replayAutomatableEvent(event);
    }
    
    /**
     *  @private
     */
    override public function createAutomationIDPart(child:IAutomationObject):Object
    {
        var help:IAutomationObjectHelper = Automation.automationObjectHelper;
        return help.helpCreateIDPart(uiAutomationObject, child);
    }

    /**
     *  @private
     */
    override public function resolveAutomationIDPart(part:Object):Array
    {
        var help:IAutomationObjectHelper = Automation.automationObjectHelper;
        return help.helpResolveIDPart(uiAutomationObject, part);
    }

    //----------------------------------
    //  numAutomationChildren
    //----------------------------------
    
    /**
     *  @private
     */
    override public function get numAutomationChildren():int
    {
        return container.numChildren + container.numRepeaters;
    }
    
    /**
     *  @private
     */
    override public function getAutomationChildAt(index:int):IAutomationObject
    {
        if (index < container.numChildren)
        {
            var d:Object = container.getChildAt(index);
            return d as IAutomationObject;
        }   
        
        var r:Object = container.childRepeaters[index - container.numChildren];
        return r as IAutomationObject;
    }
    
    //----------------------------------
    //  automationTabularData
    //----------------------------------

    /**
     *  @private
     */
    override public function get automationTabularData():Object
    {
        return new ContainerTabularData(uiAutomationObject);
    }
    
    //--------------------------------------------------------------------------
    //
    //  Event handlers
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    public function scroll_eventHandler(event:ScrollEvent):void
    {
        // we skip recording a scroll event if a mouse wheel
        // event has been recorded
        if (skipScrollEvent)
        {
            skipScrollEvent = false;
            return;
        }   
        if(event.detail == ScrollEventDetail.THUMB_TRACK)
            return;
        // the checks have been added to prevent multiple recording
        // of the same scroll event
        if(!previousEvent || (event.delta && previousEvent.delta != event.delta) ||
             previousEvent.detail != event.detail ||
             previousEvent.direction != event.direction ||
             previousEvent.position != event.position ||
             previousEvent.type != event.type)
        {
            recordAutomatableEvent(event);
            previousEvent = event.clone() as ScrollEvent;
        }
    }
    
    /**
     *  @private
     */
     private function mouseWheelHandler(event:MouseEvent):void
     {
        skipScrollEvent = true;
        if(event.target == uiComponent)
        {   
            recordAutomatableEvent(event, true);
        }
     }
    
}

}