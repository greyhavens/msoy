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
import mx.charts.series.BubbleSeries;
import mx.core.IFlexDisplayObject;
import mx.charts.series.items.BubbleSeriesItem;

[Mixin]
/**
 * 
 *  Defines the methods and properties required to perform instrumentation for the 
 *  BubbleSeries class. 
 * 
 *  @see mx.charts.series.BubbleSeries
 *  
 */
public class BubbleSeriesAutomationImpl extends SeriesAutomationImpl 
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
        Automation.registerDelegateClass(BubbleSeries, BubbleSeriesAutomationImpl);
    }   
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *  @param obj BubbleSeries object to be automated. 
     */
    public function BubbleSeriesAutomationImpl(obj:BubbleSeries)
    {
        super(obj);
        
        bubbleSeries = obj;
    }

    /**
     *  @private
     */
    private var bubbleSeries:BubbleSeries;

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
        if (item is BubbleSeriesItem)
        {
            var aItem:BubbleSeriesItem = item as BubbleSeriesItem;
            var x:int = aItem.x;
            var y:int = aItem.y;
            
            var p:Point = new Point(x,y);
            p = bubbleSeries.localToGlobal(p);
            p = bubbleSeries.owner.globalToLocal(p);
            return p;
        }
        
        return super.getChartItemLocation(item);    
    }

}

}