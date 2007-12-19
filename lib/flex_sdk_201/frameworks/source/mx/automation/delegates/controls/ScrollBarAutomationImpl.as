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
import flash.events.EventDispatcher;
import flash.events.MouseEvent;

import mx.automation.Automation;
import mx.automation.IAutomationObjectHelper;
import mx.automation.delegates.core.UIComponentAutomationImpl;
import mx.controls.scrollClasses.ScrollBar;
import mx.core.mx_internal;
import mx.events.ScrollEvent;
import mx.events.ScrollEventDetail;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  ScrollBar class.
 * 
 *  @see mx.controls.scrollClasses.ScrollBar 
 *
 */
public class ScrollBarAutomationImpl extends UIComponentAutomationImpl 
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
        Automation.registerDelegateClass(ScrollBar, ScrollBarAutomationImpl);
    }   
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj ScrollBar object to be automated.     
     */
    public function ScrollBarAutomationImpl(obj:ScrollBar)
    {
        super(obj);
        
        obj.addEventListener(ScrollEvent.SCROLL, scrollHandler, false, 0, true);
    }

    /**
     *  @private
     *  storage for the owner component
     */
    protected function get scroll():ScrollBar
    {
        return uiComponent as ScrollBar;
    }
    
    /**
     *  @private
     */
    private var previousEvent:ScrollEvent;
    
    //----------------------------------
    //  automationValue
    //----------------------------------

    /**
     *  @private
     */
    override public function get automationValue():Array
    {
        return [ scroll.scrollPosition.toString() ];
    }

    /**
     *  @private
     *  Replays ScrollEvents.
     *  ScrollEvents are replayed by simply setting the
     *  <code>verticalScrollPosition</code> or
     *  <code>horizontalScrollPosition</code> properties of the instance.
     */
    override public function replayAutomatableEvent(interaction:Event):Boolean
    {
        if (interaction is ScrollEvent)
        {
            var scrollEvent:ScrollEvent = ScrollEvent(interaction);
            var targetObject:EventDispatcher = null;
            var mouseEvent:MouseEvent = new MouseEvent(MouseEvent.CLICK);
            if (scrollEvent.detail == scroll.lineMinusDetail)
                targetObject = scroll.upArrow;
            else if (scrollEvent.detail == scroll.linePlusDetail)
                targetObject = scroll.downArrow;
            else if (scrollEvent.detail == scroll.pageMinusDetail)
            {
                targetObject = uiComponent;
                mouseEvent.localX = 0;
                mouseEvent.localY = 0;
            }
            else if (scrollEvent.detail == scroll.pagePlusDetail)
            {
                targetObject = uiComponent;
                mouseEvent.localX = scroll.width;
                mouseEvent.localY = scroll.height;
            }
            else if (scrollEvent.detail == ScrollEventDetail.THUMB_POSITION)
            {
                targetObject = scroll.scrollThumb;
                scroll.scrollPosition = scrollEvent.position;
            }
            if (targetObject)
            {
                var help:IAutomationObjectHelper = Automation.automationObjectHelper;
                help.replayClick(targetObject, mouseEvent);
            }
            scroll.scrollPosition = scrollEvent.position;
            return true;
        }
        else
        {
            return super.replayAutomatableEvent(interaction);
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
    private function scrollHandler(event:ScrollEvent):void
    { 
        if(!previousEvent || previousEvent.delta != event.delta ||
             previousEvent.detail != event.detail ||
             previousEvent.direction != event.direction ||
             previousEvent.position != event.position ||
             previousEvent.type != event.type)
        {
            recordAutomatableEvent(event);
            previousEvent = event.clone() as ScrollEvent;
        }
    }

}
}