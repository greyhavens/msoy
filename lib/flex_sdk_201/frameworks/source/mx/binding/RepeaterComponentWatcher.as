////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.binding
{

import flash.events.Event;
import mx.core.mx_internal;

use namespace mx_internal;

[ExcludeClass]

/**
 *  @private
 */
public class RepeaterComponentWatcher extends PropertyWatcher
{
    include "../core/Version.as";

	//--------------------------------------------------------------------------
	//
	//  Constructor
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
     *
     *  Create a RepeaterComponentWatcher
     *
     *  @param prop The name of the property to watch.
     *  @param event The event type that indicates the property has changed.
	 */
    public function RepeaterComponentWatcher(propertyName:String,
                                             events:Object)
    {
		super(propertyName, events);
    }

	//--------------------------------------------------------------------------
	//
	//  Properties
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
    private var clones:Array;

	/**
	 *  @private
	 */
    private var original:Boolean = true;

	//--------------------------------------------------------------------------
	//
	//  Overridden methods: Watcher
	//
	//--------------------------------------------------------------------------

	/**
	 *  @private
	 */
    override public function updateChildren():void
    {
        if (original)
        {
            updateClones();
        }
        else
        {
            super.updateChildren();
        }
    }

	/**
	 *  @private
	 */
    override protected function shallowClone():Watcher
    {
        return new RepeaterComponentWatcher(propertyName, events);
    }

	/**
	 *  @private
	 */
    private function updateClones():void
    {
        var components:Array = value as Array;

        if (components)
        {
            if (clones)
                clones = clones.splice(0, components.length);
            else
                clones = [];

            for (var i:int = 0; i < components.length; i++)
            {
                var clone:RepeaterComponentWatcher = RepeaterComponentWatcher(clones[i]);
                
                if (!clone)
                {
                    clone = RepeaterComponentWatcher(deepClone(i));
                    clone.original = false;
                    clones[i] = clone;
                }

                clone.value = components[i];
                clone.updateChildren();
            }
        }
    }

	//--------------------------------------------------------------------------
	//
	//  Event handlers
	//
	//--------------------------------------------------------------------------

    /**
     *  Invokes super's notifyListeners() on each of the clones.
     */
    override public function notifyListeners(commitEvent:Boolean):void
    {
        if (original)
        {
            if (clones)
            {
                for (var i:int = 0; i < clones.length; i++)
                {
                    RepeaterComponentWatcher(clones[i]).notifyListeners(commitEvent);
                }
            }
        }
        else
        {
            super.notifyListeners(commitEvent);
        }
    }
}

}
