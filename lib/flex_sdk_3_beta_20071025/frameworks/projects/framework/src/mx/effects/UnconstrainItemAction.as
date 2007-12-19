////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.effects
{

import mx.core.mx_internal;
import mx.effects.effectClasses.PropertyChanges;
import mx.controls.listClasses.ListBase;
import mx.effects.effectClasses.UnconstrainItemActionInstance;

use namespace mx_internal;

//--------------------------------------
//  Excluded APIs
//--------------------------------------

[Exclude(name="duration", kind="property")]

/**
 *  The UnconstrainItemAction class defines an action effect that
 *  is used in a data-effect definition
 *  to temporarily stop item renderers from being positioned by the
 *  layout algorithm of the parent control. This effect can be used
 *  to allow item renderers in a TileList control to move freely
 *  rather than being constrained to lay in the normal grid defined by the control.
 *  The default data effect class for the TileList control, DefaultTileListEffect, 
 *  uses this effect.
 *
 *  <p>You typically add this effect when your custom data effect moves item renderers.</p>
 *   
 *  @mxml
 *
 *  <p>The <code>&lt;mx:UnconstrainItemAction&gt;</code> tag
 *  inherits all of the tag attributes of its superclass,
 *  and adds no new tag attributes:</p>
 *
 *  <pre>
 *  &lt;mx:UnconstrainItemAction
 *  /&gt;
 *  </pre>
 *
 *  @see mx.effects.effectClasses.UnconstrainItemActionInstance
 *  @see mx.effects.DefaultTileListEffect
 */
public class UnconstrainItemAction extends Effect
{
    include "../core/Version.as";

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
    public function UnconstrainItemAction(target:Object = null)
    {
        super(target);

        instanceClass = UnconstrainItemActionInstance;
    }
    
    //--------------------------------------------------------------------------
    //
    //  Overridden methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override protected function initInstance(instance:IEffectInstance):void
    {
        super.initInstance(instance);
        
        var actionInstance:UnconstrainItemActionInstance  =
            UnconstrainItemActionInstance(instance);

    }
}

}
