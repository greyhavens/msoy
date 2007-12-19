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
import mx.charts.chartClasses.HLOCSeriesBase;
import mx.core.IFlexDisplayObject;
import mx.charts.series.items.HLOCSeriesItem;

[Mixin]
/**
 * 
 *  Defines the methods and properties required to perform instrumentation for the 
 *  CandlestickSeries and HLOCSeries classes. 
 * 
 *  @see mx.charts.chartClasses.HLOCSeriesBase
 *  @see mx.charts.series.CandlestickSeries
 *  @see mx.charts.series.HLOCSeries
 *  
 */
public class HLOCSeriesBaseAutomationImpl extends SeriesAutomationImpl 
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
        Automation.registerDelegateClass(HLOCSeriesBase, HLOCSeriesBaseAutomationImpl);
    }   
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *  @param obj HLOCSeriesBase object to be automated.     
     */
    public function HLOCSeriesBaseAutomationImpl(obj:HLOCSeriesBase)
    {
        super(obj);
        
        hlocSeries = obj;
    }
    
    /**
     *  @private
     *  storage for the owner component
     */
    private var hlocSeries:HLOCSeriesBase;

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
        return hlocSeries.openField + ";" + hlocSeries.closeField;
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
        if (item is HLOCSeriesItem)
        {
            var aItem:HLOCSeriesItem = item as HLOCSeriesItem;
            var x:int = aItem.x;
            var y:int = (aItem.open + aItem.close)/2;
            
            var p:Point = new Point(x,y);
            p = hlocSeries.localToGlobal(p);
            p = hlocSeries.owner.globalToLocal(p);
            return p;
        }
        
        return super.getChartItemLocation(item);    
    }

}

}