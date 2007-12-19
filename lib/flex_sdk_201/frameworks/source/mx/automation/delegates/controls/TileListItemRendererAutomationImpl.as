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
import mx.automation.Automation;
import mx.automation.delegates.core.UIComponentAutomationImpl;
import mx.controls.listClasses.TileListItemRenderer;
import mx.core.mx_internal;

use namespace mx_internal;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  TileListItemRenderer class.
 * 
 *  @see mx.controls.listClasses.TileListItemRenderer 
 *
 */
public class TileListItemRendererAutomationImpl extends UIComponentAutomationImpl 
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
        Automation.registerDelegateClass(TileListItemRenderer, TileListItemRendererAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj TileList object to be automated.     
     */
    public function TileListItemRendererAutomationImpl(obj:TileListItemRenderer)
    {
        super(obj);
    }

    /**
     *  @private
     *  storage for the owner component
     */
    protected function get itemRenderer():TileListItemRenderer
    {
        return uiComponent as TileListItemRenderer;
    }
    
    //--------------------------------------------------------------------------
    //
    //  Overridden properties
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    //----------------------------------
    //  automationName
    //----------------------------------

    /**
     *  @private
     */
    override public function get automationName():String
    {
        return itemRenderer.getLabel().text || super.automationName;
    }
        
}
}