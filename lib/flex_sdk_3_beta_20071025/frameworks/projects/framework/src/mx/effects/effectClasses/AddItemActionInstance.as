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
import mx.effects.IEffectTargetHost;

/**
 *  The AddItemActionInstance class implements the instance class
 *  for the AddItemAction effect.
 *  Flex creates an instance of this class when it plays a AddItemAction
 *  effect; you do not create one yourself.
 *
 *  @see mx.effects.AddItemAction
 */  
public class AddItemActionInstance extends ActionEffectInstance
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
    public function AddItemActionInstance(target:Object)
    {
        super(target);
    }
    
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

        effectTargetHost.addDataEffectItem(target);
                        
        // We're done...
        finishRepeat();
    }
}

}
