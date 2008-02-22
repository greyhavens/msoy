////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2006 Adobe Macromedia Software LLC and its licensors.
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.events
{

import flash.events.Event;

/**
 *  The AIREvent class represents the event object passed to
 *  the event listener for several AIR-specific events.
 */
public class AIREvent extends Event
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class constants
    //
    //--------------------------------------------------------------------------

    /**
     *  Dispatched when this application gets activated.
     *
     *  @eventType applicationActivate
     */
    public static const APPLICATION_ACTIVATE:String = "applicationActivate";

    /**
     *  Dispatched when this application gets deactivated.
     *
     *  @eventType applicationDeactivate
     */
    public static const APPLICATION_DEACTIVATE:String = "applicationDeactivate";
    
    /**
     *  Dispatched when the Window or WindowedApplication becomes visible
     *  after completing its initial layout
     * 
     *  @eventType windowComplete
     */
    public static const WINDOW_COMPLETE:String = "windowComplete";

    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     *
     *  @param type The event type; indicates the action that caused the event.
     *
     *  @param bubbles Specifies whether the event can bubble up
     *  the display list hierarchy.
     *
     *  @param cancelable Specifies whether the behavior
     *  associated with the event can be prevented.
     */
    public function AIREvent(type:String, bubbles:Boolean = false,
                             cancelable:Boolean = false)
    {
        super(type, bubbles, cancelable);
    }

    //--------------------------------------------------------------------------
    //
    //  Overridden methods: Event
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override public function clone():Event
    {
        return new AIREvent(type, bubbles, cancelable);
    }
}

}
