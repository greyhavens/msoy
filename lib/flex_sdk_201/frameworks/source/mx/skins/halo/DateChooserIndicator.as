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
import mx.skins.ProgrammaticSkin;

use namespace mx_internal;

/**
 *  The skins of the DateChooser's indicators for 
 *  displaying today, rollover and selected dates.
 */
public class DateChooserIndicator extends ProgrammaticSkin
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
    public function DateChooserIndicator()
    {
        super();             
    }
    
    //--------------------------------------------------------------------------
    //
    //  Variables
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    mx_internal var indicatorColor:String = "rollOverColor";    

    //--------------------------------------------------------------------------
    //
    //  Overridden methods
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
        g.lineStyle(0, getStyle("themeColor"), 0)
        g.beginFill(getStyle(mx_internal::indicatorColor));
        g.drawRect(1, 0, w - 2, h);
        g.endFill();
	}
}

}
