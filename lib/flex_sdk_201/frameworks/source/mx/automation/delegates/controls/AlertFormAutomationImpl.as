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
import flash.events.KeyboardEvent;
import flash.ui.Keyboard;

import mx.automation.Automation;
import mx.automation.IAutomationManager;
import mx.automation.IAutomationObject;
import mx.automation.delegates.core.UIComponentAutomationImpl;
import mx.controls.alertClasses.AlertForm;
import mx.controls.Button;
import mx.core.EventPriority;
import mx.core.mx_internal;
import flash.events.Event;
import mx.automation.IAutomationObject;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the AlertForm class. 
 * 
 *  @see mx.controls.alertClasses.AlertForm
 *  
 */
public class AlertFormAutomationImpl extends UIComponentAutomationImpl 
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
            Automation.registerDelegateClass(AlertForm, AlertFormAutomationImpl);
    }   
    
    /**
     *  Constructor.
     * @param obj AlertForm object to be automated.     
     */
    public function AlertFormAutomationImpl(obj:AlertForm)
    {
        super(obj);
        
        alertForm = obj;
    }
    
    private var alertForm:AlertForm;
    
    override protected function componentInitialized():void
    {   
        super.componentInitialized();
        for each(var b:Button in alertForm.buttons)
        {
            // we want to record escape key before alertForm closes the alert
            b.addEventListener(KeyboardEvent.KEY_DOWN, alertKeyDownHandler,
                                false, EventPriority.DEFAULT+1, true);
        }
    }
 

    /**
     *  @private
     */
    private function alertKeyDownHandler(event:KeyboardEvent):void
    {
        if (event.keyCode == Keyboard.ESCAPE)
        {
            // we want to record the escape key as invoked from the button.
            var am:IAutomationManager = Automation.automationManager;
            var delegate:IAutomationObject = event.target as IAutomationObject;
            if(am && delegate)
                am.recordAutomatableEvent(delegate, event);
        }
    }
}
}