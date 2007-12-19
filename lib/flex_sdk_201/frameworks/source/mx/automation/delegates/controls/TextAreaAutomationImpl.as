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
import mx.automation.delegates.core.ScrollControlBaseAutomationImpl;
import mx.controls.TextArea;
import mx.core.mx_internal;
import mx.core.UITextField;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  TextArea control.
 * 
 *  @see mx.controls.TextArea 
 *
 */
public class TextAreaAutomationImpl extends ScrollControlBaseAutomationImpl 
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
        Automation.registerDelegateClass(TextArea, TextAreaAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj TextArea object to be automated.     
     */
    public function TextAreaAutomationImpl(obj:TextArea)
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
    protected function get  textArea():TextArea
    {
        return uiComponent as TextArea;
    }

    /**
     *  @private
     *  Generic record/replay logic for textfields.
     */
    private var automationHelper:TextFieldAutomationHelper;

    //----------------------------------
    //  automationValue
    //----------------------------------

    /**
     *  @private
     */
    override public function get automationValue():Array
    {
        return [ textArea.text ];
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

    /**
     *  @private
     */
    override protected function componentInitialized():void
    {
        super.componentInitialized();
        var textField:UITextField = textArea.getTextField();
        automationHelper = new TextFieldAutomationHelper(uiComponent, uiAutomationObject, textField);
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