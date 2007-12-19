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
import mx.controls.CheckBox;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  CheckBox control.
 * 
 *  @see mx.controls.CheckBox 
 *
 */
public class CheckBoxAutomationImpl extends ButtonAutomationImpl 
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
            Automation.registerDelegateClass(CheckBox, CheckBoxAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj CheckBox object to be automated.     
     */
    public function CheckBoxAutomationImpl(obj:CheckBox)
    {
        super(obj);
    }

    /**
     *  @private
     *  storage for the owner component
     */
    protected function get chk():CheckBox
    {
        return uiComponent as CheckBox;
    }
    
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
        var result:String = chk.selected ? "[X]" : "[ ]";
        if (chk.label || chk.toolTip)
            result += " " + (chk.label || chk.toolTip);
        return [ result ];
    }

}
}