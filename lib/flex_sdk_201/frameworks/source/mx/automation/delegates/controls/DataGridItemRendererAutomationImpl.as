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

import mx.automation.Automation;
import mx.automation.IAutomationObject;
import mx.automation.delegates.core.UITextFieldAutomationImpl;
import mx.controls.dataGridClasses.DataGridItemRenderer;
import mx.core.mx_internal;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  DataGridItemRenderer class.
 * 
 *  @see mx.controls.dataGridClasses.DataGridItemRenderer 
 *
 */
public class DataGridItemRendererAutomationImpl extends UITextFieldAutomationImpl 
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
        Automation.registerDelegateClass(DataGridItemRenderer, DataGridItemRendererAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj DataGridItem object to be automated.     
     */
    public function DataGridItemRendererAutomationImpl(obj:DataGridItemRenderer)
    {
        super(obj);
    }

    /**
     *  @private
     */
    protected function get itemRenderer():DataGridItemRenderer
    {
        return uiTextField as DataGridItemRenderer;
    }
    
}

}