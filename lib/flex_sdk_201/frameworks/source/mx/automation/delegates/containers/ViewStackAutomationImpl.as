////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation.delegates.containers 
{
import flash.display.DisplayObject;
import flash.events.Event;

import mx.automation.Automation;
import mx.automation.IAutomationObject;
import mx.automation.delegates.core.ContainerAutomationImpl;
import mx.containers.ViewStack;
import mx.core.mx_internal;
import mx.events.ChildExistenceChangedEvent;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines the methods and properties required to perform instrumentation for the 
 *  ViewStack class. 
 * 
 *  @see mx.containers.ViewStack
 *  
 */
public class ViewStackAutomationImpl extends ContainerAutomationImpl 
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
        Automation.registerDelegateClass(ViewStack, ViewStackAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj ViewStack object to be automated.     
     */
    public function ViewStackAutomationImpl(obj:ViewStack)
    {
        super(obj);
    }

    /**
     *  @private
     *  storage for the owner component
     */
    protected function get viewStack():ViewStack
    {
        return uiComponent as ViewStack;
    }

}
}