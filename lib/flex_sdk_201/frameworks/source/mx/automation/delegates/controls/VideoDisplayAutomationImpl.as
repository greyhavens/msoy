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
import mx.controls.VideoDisplay

[Mixin]
/**
 * 
 *  Defines methods and properties required to perform instrumentation for the 
 *  VideoDisplay control.
 * 
 *  @see mx.controls.VideoDisplay 
 *
 */
public class VideoDisplayAutomationImpl extends UIComponentAutomationImpl
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
        Automation.registerDelegateClass(VideoDisplay, VideoDisplayAutomationImpl);
    }   
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * @param obj VideoDisplay object to be automated.     
     */
    public function VideoDisplayAutomationImpl(obj:VideoDisplay)
    {
        super(obj);
        
        recordClick = true;
    }
    
}

}