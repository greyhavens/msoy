/*************************************************************************
 * 
 * ADOBE CONFIDENTIAL
 * __________________
 * 
 *  [2002] - [2007] Adobe Systems Incorporated 
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of Adobe Systems Incorporated and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to Adobe Systems Incorporated
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from Adobe Systems Incorporated.
 */
package mx.events
{
import flash.geom.Rectangle;
import flash.events.Event;
import flash.events.NativeWindowBoundsEvent;

public class FlexNativeWindowBoundsEvent extends NativeWindowBoundsEvent
{
	
	/**
	 *  dispatched when the underlying NativeWindow resizes
	 *
	 *  @eventType windowResize
	 */
	public static const WINDOW_RESIZE:String = "windowResize";
	
	/**
	 *  dispatched when the underlying NativeWindow changes
	 *
	 *  @eventType windowMove
	 */
	public static const WINDOW_MOVE:String = "windowMove";
	
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
     * 
     *  @param beforeBounds The bounds of the window before the resize.
     * 
     *  @param afterBounds The bounds of the window before the resize.
     */
	public function FlexNativeWindowBoundsEvent(type:String, bubbles:Boolean = false, cancelable:Boolean = false, 
					beforeBounds:Rectangle = null, afterBounds:Rectangle = null)
	{
		super(type, bubbles, cancelable, beforeBounds, afterBounds);
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
        return new FlexNativeWindowBoundsEvent(type, bubbles, cancelable, beforeBounds, afterBounds);
    }
}
}