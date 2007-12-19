////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.core
{

import flash.display.DisplayObject;
import flash.events.Event;
import mx.events.FlexEvent;

[ExcludeClass]

/**
 *  @private
 */
public class FlexApplicationBootstrap extends FlexModuleFactory
{
	include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

    /**
	 *  Constructor.
	 */
	public function FlexApplicationBootstrap()
    {
        // Register for "ready" first, because we may already be ready.
		addEventListener("ready", readyHandler);
		
		super();
    }

	//--------------------------------------------------------------------------
	//
	//  Event handlers
	//
	//--------------------------------------------------------------------------

    /**
	 *  @private
	 */
    public function readyHandler(event:Event):void
    {
        removeEventListener("ready", readyHandler);
        
		var o:Object = create();
        
		if (o is DisplayObject)
		{
            addChild(DisplayObject(o));
		    o.dispatchEvent(new FlexEvent(FlexEvent.APPLICATION_COMPLETE));
		}

    }
}

}
