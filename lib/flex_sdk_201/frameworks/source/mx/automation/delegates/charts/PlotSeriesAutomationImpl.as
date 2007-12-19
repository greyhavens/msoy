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
import flash.geom.Point;

import mx.automation.Automation;
import mx.charts.ChartItem;
import mx.charts.series.PlotSeries;
import mx.core.IFlexDisplayObject;
import mx.charts.series.items.PlotSeriesItem;

[Mixin]
/**
 * 
 *  Defines the methods and properties required to perform instrumentation for the 
 *  PlotSeries class. 
 * 
 *  @see mx.charts.series.PlotSeries
 *  
 */
public class PlotSeriesAutomationImpl extends SeriesAutomationImpl 
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
        Automation.registerDelegateClass(PlotSeries, PlotSeriesAutomationImpl);
    }   
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *  @param obj PlotSeries object to be automated.     
     */
    public function PlotSeriesAutomationImpl(obj:PlotSeries)
    {
        super(obj);
        
        plotSeries = obj;
    }

    /**
     *  @private
     */
    private var plotSeries:PlotSeries;

    //--------------------------------------------------------------------------
    //
    //  Overridden properties
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override public function get automationName():String
    {
        if(plotSeries.xField && plotSeries.yField)
            return String(plotSeries.xField + ";" + plotSeries.yField);

        return super.automationName;
    }
    
    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function getChartItemLocation(item:ChartItem):Point
    {
        if (item is PlotSeriesItem)
        {
            var aItem:PlotSeriesItem = item as PlotSeriesItem;
            var x:int = aItem.x;
            var y:int = aItem.y;
            
            var p:Point = new Point(x,y);
            p = plotSeries.localToGlobal(p);
            p = plotSeries.owner.globalToLocal(p);
            return p;
        }
    
        return super.getChartItemLocation(item);    
    }

}

}