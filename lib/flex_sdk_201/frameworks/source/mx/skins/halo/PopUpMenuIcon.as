////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.skins.halo
{

import flash.display.Graphics;
import mx.core.mx_internal;

/**
 *  The skin for all the states of the icon in a PopUpMenuButton.
 */
public class PopUpMenuIcon extends PopUpIcon
{
    include "../../core/Version.as";    
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor	 
     */ 
    public function PopUpMenuIcon()
    {
        super();
    }
    
    //--------------------------------------------------------------------------
    //
    //  Overriden Methods
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
        g.moveTo(0, 0);
        g.lineTo(w / 2, height);
        g.lineTo(w, 0);
    }
}

}
