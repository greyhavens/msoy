////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.automation
{

import flash.display.DisplayObject;
import flash.utils.getQualifiedClassName;

import mx.automation.IAutomationMouseSimulator;
import mx.automation.IAutomationObjectHelper;
import mx.core.IUIComponent;
import mx.core.mx_internal;

use namespace mx_internal;

/**
 * The Automation class defines the entry point for the Flex Automation framework.
 */
public class Automation
{
    //--------------------------------------------------------------------------
    //
    //  Class variables
    //
    //--------------------------------------------------------------------------
    
    /**
     *  @private
     *  Component class to Delegate class map
     */
    mx_internal static var delegateClassMap:Object;

    //--------------------------------------------------------------------------
    //
    //  Class properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  automationManager
    //----------------------------------

    /**
     *  @private
     */
    private static var _automationManager:IAutomationManager;
    
    /**
     * The IAutomationManager instance.
     */
    public static function get automationManager():IAutomationManager
    {
        return _automationManager;
    }

    /**
     * @private
     */
    public static function set automationManager(manager:IAutomationManager):void
    {
        _automationManager = manager;
        _automationObjectHelper = manager as IAutomationObjectHelper;
    }

    //----------------------------------
    //  automationObjectHelper
    //----------------------------------

    /**
     *  @private
     */
    private static var _automationObjectHelper:IAutomationObjectHelper;
    
    /**
     * The available IAutomationObjectHelper instance.
     */
    public static function get automationObjectHelper():IAutomationObjectHelper
    {
        return _automationObjectHelper;
    }

    //----------------------------------
    //  initialized
    //----------------------------------

    /**
     * Contains <code>true</code> if the automation module has been initialized.
     */
    public static function get initialized():Boolean
    {
        return _automationManager != null;
    }

    //----------------------------------
    //  mouseSimulator
    //----------------------------------

    /**
     *  @private
     */
    private static var _mouseSimulator:IAutomationMouseSimulator;
        
    /**
     * The currently active mouse simulator.
     */
    public static function get mouseSimulator():IAutomationMouseSimulator
    {
        return _mouseSimulator;
    }

    /**
     * @private
     */
    public static function set mouseSimulator(ms:IAutomationMouseSimulator):void
    {
        _mouseSimulator = ms;
    }
    
    //--------------------------------------------------------------------------
    //
    //  Class methods
    //
    //--------------------------------------------------------------------------

    /**
     *  Registers the component class and delegate class association with Automation.
     * 
     *  @param compClass The component class. 
     * 
     *  @param delegateClass The delegate class associated with the component.
     */
    public static function registerDelegateClass(compClass:Class, delegateClass:Class):void
    {
        if(!delegateClassMap)
            delegateClassMap = {};

        var className:String = getQualifiedClassName(compClass);
        delegateClassMap[className] = delegateClass;
    }
}

}
