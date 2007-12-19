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
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.ui.Keyboard;
import mx.automation.Automation;
import mx.controls.ButtonBar;
import mx.core.mx_internal;
import mx.core.EventPriority;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  ButtonBar control.
 * 
 *  @see mx.controls.ButtonBar 
 *
 */
public class ButtonBarAutomationImpl extends NavBarAutomationImpl 
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
            Automation.registerDelegateClass(ButtonBar, ButtonBarAutomationImpl);
    }   

    /**
     *  Constructor.
     * @param obj ButtonBar object to be automated.     
     */
    public function ButtonBarAutomationImpl(obj:ButtonBar)
    {
        super(obj);
    }

    /**
     *  @private
     *  storage for the owner component
     */
    protected function get buttonBar():ButtonBar
    {
        return uiComponent as ButtonBar;
    }
    
    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override public function recordAutomatableEvent(
                                event:Event, cacheable:Boolean = false):void
    {
        if (buttonBar.simulatedClickTriggerEvent == null ||
            buttonBar.simulatedClickTriggerEvent is MouseEvent)
        {
            super.recordAutomatableEvent(event, cacheable);
        }
    }

    //--------------------------------------------------------------------------
    //
    //  Event handlers
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function keyDownHandler(event:KeyboardEvent):void 
    {
        switch (event.keyCode)
        {
            case Keyboard.DOWN:
            case Keyboard.RIGHT:
            case Keyboard.UP:
            case Keyboard.LEFT:
                recordAutomatableEvent(event);
                break;  
        }
    }

}
}
