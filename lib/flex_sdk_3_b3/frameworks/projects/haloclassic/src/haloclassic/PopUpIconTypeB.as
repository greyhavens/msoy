////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2005 Macromedia, Inc. All Rights Reserved.
//  The following is Sample Code and is subject to all restrictions
//  on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package haloclassic
{

import flash.display.Graphics;
import mx.core.mx_internal;

[ExcludeClass]

/**
 *  Documentation is not currently available.
 *  @review
 */
public class PopUpIconTypeB extends PopUpIcon
{
	include "../mx/core/Version.as";
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor	 
     */ 
    public function PopUpIconTypeB()
    {
        super();
    }
    
    //--------------------------------------------------------------------------
    //
    //  Overriden methods
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
	override protected function updateDisplayList(w:Number, h:Number):void
    {
		super.updateDisplayList(w, h);

        var g:Graphics = graphics;
        
		g.clear();
        g.lineStyle(1, mx_internal::arrowColor);
        g.moveTo(-w / 2, -h / 2);
        g.lineTo(0, height / 2);
        g.lineTo(w / 2, -h / 2);
    }
}

}
