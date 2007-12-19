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
import mx.automation.tabularData.AxisRendererTabularData;
import mx.automation.IAutomationObject;
import mx.automation.IAutomationObjectHelper;
import mx.automation.IAutomationTabularData;
import mx.automation.delegates.core.UIComponentAutomationImpl;
import mx.charts.AxisRenderer;



[Mixin]
/**
 * 
 *  Defines the methods and properties required to perform instrumentation for the 
 *  AxisRenderer class.
 * 
 *  @see mx.charts.AxisRenderer
 *  
 */
public class AxisRendererAutomationImpl extends UIComponentAutomationImpl 
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
        Automation.registerDelegateClass(AxisRenderer, AxisRendererAutomationImpl);
    }   
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *  @param obj AxisRenderer object to be automated. 
     */
    public function AxisRendererAutomationImpl(obj:AxisRenderer)
    {
        super(obj);
        
        axisRenderer = obj;
    }

    /**
     *  @private
     */
    private var axisRenderer:AxisRenderer;
   
    /**
     *  @private
     */
    override public function get automationTabularData():Object
    {
        return new AxisRendererTabularData(uiAutomationObject);
    }

}

}
