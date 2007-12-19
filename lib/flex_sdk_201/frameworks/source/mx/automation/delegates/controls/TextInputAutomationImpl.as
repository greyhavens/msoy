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
import mx.automation.Automation;
import mx.automation.IAutomationObjectHelper;
import mx.automation.delegates.TextFieldAutomationHelper;
import mx.automation.delegates.core.UIComponentAutomationImpl;
import mx.core.mx_internal;
import mx.core.UITextField;
import mx.controls.TextInput;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  TextInput control.
 * 
 *  @see mx.controls.TextInput 
 *
 */
public class TextInputAutomationImpl extends UIComponentAutomationImpl 
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
        Automation.registerDelegateClass(TextInput, TextInputAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj TextInput object to be automated.     
     */
    public function TextInputAutomationImpl(obj:TextInput)
    {
        super(obj);
    }

    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     *  storage for the owner component
     */
    protected function get  textInput():TextInput
    {
        return uiComponent as TextInput;
    }

    /**
     *  @private
     *  Generic record/replay logic for textfields.
     */
    private var automationHelper:TextFieldAutomationHelper;
        
    //--------------------------------------------------------------------------
    //
    //  Overridden properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  automationValue
    //----------------------------------

    /**
     *  @private
     */
    override public function get automationValue():Array
    {
        return [ textInput.text ];
    }


    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override public function replayAutomatableEvent(interaction:Event):Boolean
    {
        return ((automationHelper &&
                 automationHelper.replayAutomatableEvent(interaction)) ||
                super.replayAutomatableEvent(interaction));
    }
    
    //--------------------------------------------------------------------------
    //
    //  Event handlers
    //
    //--------------------------------------------------------------------------

    override protected function componentInitialized():void
    {
        super.componentInitialized();
        var textField:UITextField = textInput.getTextField();
        automationHelper = new TextFieldAutomationHelper(uiComponent, uiAutomationObject, textField)
    }

    /**
     *  @private
     *  Prevent duplicate ENTER key recordings. 
     */
    override protected function keyDownHandler(event:KeyboardEvent):void
    {
        ;
    }
        
}
}