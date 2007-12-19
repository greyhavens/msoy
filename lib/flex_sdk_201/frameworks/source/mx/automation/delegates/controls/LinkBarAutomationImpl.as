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
import mx.controls.LinkBar;
import mx.core.EventPriority;

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  LinkBar control.
 * 
 *  @see mx.controls.LinkBar 
 *
 */
public class LinkBarAutomationImpl extends NavBarAutomationImpl 
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
        Automation.registerDelegateClass(LinkBar, LinkBarAutomationImpl);
    }   

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj LinkBar object to be automated.     
     */
    public function LinkBarAutomationImpl(obj:LinkBar)
    {
        super(obj);
    }

    /**
     *  @private
     *  storage for the owner component
     */
    protected function get linkBar():LinkBar
    {
        return uiComponent as LinkBar;
    }
    
    /**
     *  @private
     */
    private var preventRecording:Boolean = false;
    

    /**
     *  @private
     */
    override public function recordAutomatableEvent(event:Event,
                                           cacheable:Boolean = false):void
    {
        if (!preventRecording)
            super.recordAutomatableEvent(event, cacheable);
        
        preventRecording = false;
    }

    /**
     *  @private
     */
    override protected function keyDownHandler(event:KeyboardEvent):void
    {
        preventRecording = true;
    }
     
}

}