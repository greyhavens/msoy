////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.effects.effectClasses
{

import flash.events.Event;
import mx.core.mx_internal;
import mx.controls.listClasses.ListBase;

/**
 *  The UnconstrainItemActionInstance class implements the instance class
 *  for the UnconstrainItemAction effect.
 *  Flex creates an instance of this class when it plays a UnconstrainItemAction
 *  effect; you do not create one yourself.
 *
 *  @see mx.effects.UnconstrainItemAction
 */  
public class UnconstrainItemActionInstance extends ActionEffectInstance
{
    include "../../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *
     *  @param target The Object to animate with this effect.
     */
    public function UnconstrainItemActionInstance(target:Object)
    {
        super(target);
    }
    
    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    // where should this live?
    // and what should it be called? (rendererHost, maybe?)
    
    public var effectHost:ListBase = null
    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override public function initEffect(event:Event):void
    {
        super.initEffect(event);
    }
    
    /**
     *  @private
     */
    override public function play():void
    {
        // Dispatch an effectStart event from the target.
        super.play();   

        effectTargetHost.unconstrainRenderer(target);
                        
        // We're done...
        finishRepeat();
    }
}

}
