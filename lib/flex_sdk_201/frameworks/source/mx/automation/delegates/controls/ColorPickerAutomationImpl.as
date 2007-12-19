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
import flash.utils.getTimer;
import mx.automation.Automation;
import mx.automation.IAutomationObjectHelper;
import mx.controls.ComboBase;
import mx.controls.ColorPicker;
import mx.core.mx_internal;
import mx.events.ColorPickerEvent;
import mx.events.DropdownEvent;
import mx.events.ItemClickEvent;
import mx.utils.StringUtil;
import mx.automation.IAutomationObject;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  ColorPicker control.
 * 
 *  @see mx.controls.ColorPicker 
 *
 */
public class ColorPickerAutomationImpl extends ComboBaseAutomationImpl 
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
            Automation.registerDelegateClass(ColorPicker, ColorPickerAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj ColorPicker object to be automated.     
     */
    public function ColorPickerAutomationImpl(obj:ColorPicker)
    {
        super(obj);

        obj.addEventListener(DropdownEvent.OPEN, openCloseHandler, false, 0, true);
        obj.addEventListener(DropdownEvent.CLOSE, openCloseHandler, false, 0, true);
        obj.addEventListener(ColorPickerEvent.CHANGE, recordAutomatableEvent, false, 0, true);
    }
    
    /**
     *  @private
     *  storage for the owner component
     */
    protected function get colorPicker():ColorPicker
    {
        return uiComponent as ColorPicker;
    }


    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

   /**
     *  @private
     *  Replays the event specified by the parameter if possible.
     *
     *  @param interaction The event to replay.
     * 
     *  @return Whether or not a replay was successful.
     */
    override public function replayAutomatableEvent(event:Event):Boolean
    {
        var help:IAutomationObjectHelper = Automation.automationObjectHelper;
        if (event is ColorPickerEvent)
        {
            var cpEvent:ColorPickerEvent = ColorPickerEvent(event);
            switch (event.type)
            {
                case ColorPickerEvent.CHANGE:
                case ColorPickerEvent.ENTER:
                {
                    colorPicker.selectedColor = cpEvent.color;
                    // this is bad... we should be simulating the low level
                    // keyboard or mouse events.
                    colorPicker.dispatchEvent(cpEvent);
                    colorPicker.close();
                    addLayoutCompleteSynchronization();
                    return true;
                }
                default:
                {
                    throw new Error(StringUtil.substitute("resourceNotImplemented", event.type));
                }
            }
        }
        else if (event is DropdownEvent)
        {

            var cbdEvent:DropdownEvent = DropdownEvent(event);
            if (cbdEvent.triggerEvent is KeyboardEvent)
            {
                var kbEvent:KeyboardEvent = new KeyboardEvent(KeyboardEvent.KEY_DOWN);
                kbEvent.keyCode = (cbdEvent.type == DropdownEvent.OPEN
                                   ? Keyboard.DOWN
                                   : Keyboard.UP);
                kbEvent.ctrlKey = true;
                help.replayKeyboardEvent(uiComponent, kbEvent);
            }
            else //triggerEvent is MouseEvent
            {
                if ((cbdEvent.type == DropdownEvent.OPEN && !colorPicker.showingDropdown) ||
                    (cbdEvent.type == DropdownEvent.CLOSE && colorPicker.showingDropdown))
                {
					help.replayClick(colorPicker.ComboDownArrowButton);
                }
            }
 

            var completeTime:Number = getTimer() +
                colorPicker.getStyle(cbdEvent.type == DropdownEvent.OPEN ?
                         "openDuration" :
                         "closeDuration");

            help.addSynchronization(function():Boolean
            {
                return getTimer() >= completeTime;
            });

            return true;
        }

        return super.replayAutomatableEvent(event);
    }

    //--------------------------------------------------------------------------
    //
    //  Event handlers
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    private function openCloseHandler(event:DropdownEvent):void
    {
        if (event.triggerEvent)
            recordAutomatableEvent(event);
    }

}
}