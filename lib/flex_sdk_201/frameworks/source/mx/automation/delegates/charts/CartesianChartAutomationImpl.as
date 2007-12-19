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
import mx.automation.tabularData.CartesianChartTabularData;
import mx.automation.IAutomationObject;
import mx.automation.IAutomationObjectHelper;
import mx.automation.IAutomationTabularData;
import mx.automation.delegates.charts.ChartBaseAutomationImpl;
import mx.charts.chartClasses.CartesianChart;

[Mixin]
/**
 * 
 *  Defines the methods and properties required to perform instrumentation for the 
 *  CartesianChart base class. 
 * 
 *  @see mx.charts.chartClasses.CartesianChart
 *  
 */
public class CartesianChartAutomationImpl extends ChartBaseAutomationImpl 
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
        Automation.registerDelegateClass(CartesianChart, CartesianChartAutomationImpl);
    }   
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *  @param obj CartesianChart object to be automated.      
     */
    public function CartesianChartAutomationImpl(obj:CartesianChart)
    {
        super(obj);
        
        cChart = obj;
    }

    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  cartesianChart
    //----------------------------------

    /**
     *  @private
     *  storage for the owner component
     */
    private var cChart:CartesianChart;


    /**
     *  @private
     */
    override public function getAutomationChildAt(index:int):IAutomationObject
    {
        var result:Object;
        var count:int = 0;
        if (cChart.series)
        {   
            count = cChart.series.length ;
            if (index < count)
                result = cChart.series[index];  
        }
        
        if (!result && cChart.secondSeries)
        {
            count += cChart.secondSeries.length ;
            if (index < count)
                result = cChart.secondSeries[index];    
        }
        
        if (!result && cChart.verticalAxisRenderer)
        {
            ++count;
            if (index < count)
                result = cChart.verticalAxisRenderer;
        }
        
        if (!result && cChart.secondVerticalAxisRenderer)
        {
            ++count;
            if (index < count)
                result = cChart.secondVerticalAxisRenderer;
        }

        if (!result && cChart.horizontalAxisRenderer)
        {
            ++count;
            if (index < count)
                result = cChart.horizontalAxisRenderer;
        }
        
        if (!result && cChart.secondHorizontalAxisRenderer)
        {
            ++count;
            if (index < count)
                result = cChart.secondHorizontalAxisRenderer;
        }
        
        return result as IAutomationObject; 
    }

    /**
     *  @private
     */
    override public function get numAutomationChildren():int
    {
        var count:int = 0;
        if (cChart.series)
            count = cChart.series.length ;

        if (cChart.secondSeries)
            count += cChart.secondSeries.length ;
        
        if (cChart.verticalAxisRenderer)
            ++count;
        
        if (cChart.secondVerticalAxisRenderer)
            ++count;

        if (cChart.horizontalAxisRenderer)
            ++count;

        if (cChart.secondHorizontalAxisRenderer)
            ++count;
        
        return count;
    }

    /**
     *  @private
     */
    override public function get automationTabularData():Object
    {
        return new CartesianChartTabularData(uiAutomationObject);
    }

}

}