////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation.delegates.charts 
{
import flash.display.DisplayObject;
import flash.events.Event;
import flash.events.MouseEvent;

import mx.automation.Automation;
import mx.automation.IAutomationObjectHelper;
import mx.automation.delegates.core.ContainerAutomationImpl;
import mx.charts.Legend;
import mx.charts.events.LegendMouseEvent;

[Mixin]
/**
 * 
 *  Defines the methods and properties required to perform instrumentation for the 
 *  Legend class. 
 * 
 *  @see mx.charts.Legend
 *  
 */
public class LegendAutomationImpl extends ContainerAutomationImpl 
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
        Automation.registerDelegateClass(Legend, LegendAutomationImpl);
    }   
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *  @param obj Legend object to be automated. 
     */
    public function LegendAutomationImpl(obj:Legend)
    {
        super(obj);
        
        legend = obj;
        
        legend.addEventListener(LegendMouseEvent.ITEM_CLICK, recordAutomatableEvent, false, 0, true);
    }
    
    /**
     *  @private
     *  storage for the owner component
     */
    private var legend:Legend;

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
        var help:IAutomationObjectHelper = Automation.automationObjectHelper;
        var ev:MouseEvent;

        if (event is LegendMouseEvent)
        {
            var legendEvent:LegendMouseEvent = event as LegendMouseEvent;
            if (event.type == LegendMouseEvent.ITEM_CLICK)
            {
                ev = new MouseEvent(MouseEvent.CLICK);
                ev.localX = legendEvent.item.x + legendEvent.item.width/2;
                ev.localY = legendEvent.item.y + legendEvent.item.height/2;
                return help.replayClick(legendEvent.item, ev);
            }
        }
        
        return super.replayAutomatableEvent(event);
    }

}

}