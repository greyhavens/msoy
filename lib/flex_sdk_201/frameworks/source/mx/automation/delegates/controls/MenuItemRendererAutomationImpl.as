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
import mx.automation.delegates.core.UIComponentAutomationImpl;
import mx.controls.menuClasses.MenuItemRenderer;
import mx.core.mx_internal;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  MenuItem class.
 * 
 *  @see mx.controls.menuClasses.MenuItemRenderer 
 *
 */
public class MenuItemRendererAutomationImpl extends UIComponentAutomationImpl 
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
        Automation.registerDelegateClass(MenuItemRenderer, MenuItemRendererAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj MenuItemRenderer object to be automated.     
     */
    public function MenuItemRendererAutomationImpl(obj:MenuItemRenderer)
    {
        super(obj);
    }

    //----------------------------------
    //  menuItemRenderer
    //----------------------------------

    /**
     *  @private
     *  storage for the owner component
     */
    protected function get menuItemRenderer():MenuItemRenderer
    {
        return uiComponent as MenuItemRenderer;
    }

    //----------------------------------
    //  automationName
    //----------------------------------

    /**
     *  @private
     */
    override public function get automationName():String
    {
        return menuItemRenderer.getLabel().text || super.automationName;
    }

    //----------------------------------
    //  automationValue
    //----------------------------------

    /**
     *  @private
     */
    override public function get automationValue():Array
    {
        return [menuItemRenderer.getLabel().text];
    }

}

}