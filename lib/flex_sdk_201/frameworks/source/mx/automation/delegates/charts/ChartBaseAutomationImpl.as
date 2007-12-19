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

import mx.automation.Automation;
import mx.automation.AutomationIDPart;
import mx.automation.tabularData.ChartBaseTabularData;
import mx.automation.IAutomationManager;
import mx.automation.IAutomationObject;
import mx.automation.IAutomationObjectHelper;
import mx.automation.IAutomationTabularData;
import mx.automation.delegates.core.UIComponentAutomationImpl;
import mx.charts.ChartItem;
import mx.charts.HitData;
import mx.charts.chartClasses.ChartBase;
import mx.charts.events.ChartItemEvent;

[Mixin]
/**
 * 
 *  Defines the methods and properties required to perform instrumentation for the 
 *  ChartBase base class. 
 * 
 *  @see mx.charts.chartClasses.ChartBase
 *  
 */
public class ChartBaseAutomationImpl extends UIComponentAutomationImpl 
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
        Automation.registerDelegateClass(ChartBase, ChartBaseAutomationImpl);
    }   
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *  @param obj ChartBase object to be automated. 
     */
    public function ChartBaseAutomationImpl(obj:ChartBase)
    {
        super(obj);
        
        chartBase = obj;
        
        obj.addEventListener(ChartItemEvent.ITEM_ROLL_OVER, chartItemClickHandler, false, 0, true);
//      obj.addEventListener(ChartItemEvent.ITEM_ROLL_OUT, chartItemClickHandler, false, 0, true);
        obj.addEventListener(ChartItemEvent.ITEM_CLICK, chartItemClickHandler, false, 0, true);
        obj.addEventListener(ChartItemEvent.ITEM_DOUBLE_CLICK, chartItemDoubleClickHandler, false, 0, true);
    }

    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  chartBase
    //----------------------------------

    /**
     *  @private
     *  storage for the owner component
     */
    private var chartBase:ChartBase;


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

    /**
     *  @private
     */
    override public function getAutomationChildAt(index:int):IAutomationObject
    {
        return chartBase.series[index] ;    
    }

    /**
     *  @private
     */
    override public function get numAutomationChildren():int
    {
        return chartBase.series.length;
    }
   
    /**
     *  @private
     */
    override public function get automationTabularData():Object
    {
        return new ChartBaseTabularData(uiAutomationObject);
    }

    //--------------------------------------------------------------------------
    //
    //  Event handlers
    //
    //--------------------------------------------------------------------------

    /**
     * @private
     */
    private function chartItemClickHandler(event:ChartItemEvent):void
    {
        var am:IAutomationManager = Automation.automationManager;
        var itemCount:int = event.hitSet.length;
        
        event.localX = int(event.localX);
        event.localY = int(event.localY);
        
        for(var i:int = 0; i < itemCount; ++i)
        {       
            var data:HitData = event.hitSet[i];

            var ao:IAutomationObject = data.element as IAutomationObject;
            if(ao)
                am.recordAutomatableEvent(ao, event);
        }
    }

    /**
     * @private
     */
    private function chartItemDoubleClickHandler(event:ChartItemEvent):void
    {
        var am:IAutomationManager = Automation.automationManager;
        var itemCount:int = event.hitSet.length;
        
        for(var i:int = 0; i < itemCount; ++i)
        {       
            var data:HitData = event.hitSet[i];

            var ao:IAutomationObject = data.element as IAutomationObject;
            if(ao)
                am.recordAutomatableEvent(ao, event);
        }
    }
}

}