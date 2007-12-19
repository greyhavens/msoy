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
import mx.core.mx_internal;

/**
 *  @private
 *  The MetadataEvent class defines the event type for metadata and cue point events.
 */
public class MetadataEvent extends Event 
{
    include "../core/Version.as";
    
    //--------------------------------------------------------------------------
    //
    //  Class constants
    //
    //--------------------------------------------------------------------------

	/**
	 *  @private
	 *  Event type for metadata events.
	 */
	mx_internal static const METADATA:String = "metadataReceived";

	/**
	 *  The MetadataEvent.CUE_POINT constant defines the value of the 
	 *  <code>type</code> property of the event object for a <code>cuePoint</code> event.
	 * 
     *	<p>The properties of the event object have the following values:</p>
	 *  <table class=innertable>
	 *     <tr><th>Property</th><th>Value</th></tr>
     *     <tr><td><code>bubbles</code></td><td>false</td></tr>
     *     <tr><td><code>cancelable</code></td><td>false</td></tr>
     *     <tr><td><code>currentTarget</code></td><td>The Object that defines the 
     *       event listener that handles the event. For example, if you use 
     *       <code>myButton.addEventListener()</code> to register an event listener, 
     *       myButton is the value of the <code>currentTarget</code>. </td></tr>
     *     <tr><td><code>info</code></td><td>The index of the cue point 
     *       in the cue point Array.</td></tr>
     *     <tr><td><code>target</code></td><td>The Object that dispatched the event; 
     *       it is not always the Object listening for the event. 
     *       Use the <code>currentTarget</code> property to always access the 
     *       Object listening for the event.</td></tr>
	 *  </table>
	 *
	 *  @eventType cuePoint
	 */
	public static const CUE_POINT:String = "cuePoint";

	/**
	 *  @private
	 *  Cue Point type constant.  <code>MetadataEvent.info.type</code>
	 *  value for Navigation cue points embedded in FLV.
	 *
	 *  @see MetadataEvent
	 */
	mx_internal static const NAVIGATION:String = "navigation";

	/**
	 *  @private
	 *  Cue Point type constant.  <code>MetadataEvent.info.type</code>
	 *  value for Event cue points embedded in FLV.
	 *
	 *  @see MetadataEvent
	 */
	mx_internal static const EVENT:String = "event";

	/**
	 *  The value of the <code>MetadataEvent.info.type</code> property for ActionScript
	 *  cue points.  These cue points are not embedded in the FLV file but defined
	 *  using ActionScript at run time.
	 *
	 *  <p>The MetadataEvent.ACTION_SCRIPT constant defines the value of the 
	 *  <code>type</code> property of the event object for a <code>actionscript</code> event.</p>
	 * 
     *	<p>The properties of the event object have the following values:</p>
	 *  <table class=innertable>
	 *     <tr><th>Property</th><th>Value</th></tr>
     *     <tr><td><code>bubbles</code></td><td>false</td></tr>
     *     <tr><td><code>cancelable</code></td><td>false</td></tr>
     *     <tr><td><code>currentTarget</code></td><td>The Object that defines the 
     *       event listener that handles the event. For example, if you use 
     *       <code>myButton.addEventListener()</code> to register an event listener, 
     *       myButton is the value of the <code>currentTarget</code>. </td></tr>
     *     <tr><td><code>info</code></td><td>The index of the cue point 
     *       in the cue point Array.</td></tr>
     *     <tr><td><code>target</code></td><td>The Object that dispatched the event; 
     *       it is not always the Object listening for the event. 
     *       Use the <code>currentTarget</code> property to always access the 
     *       Object listening for the event.</td></tr>
	 *  </table>
	 *
	 *  @eventType actionscript
	 */
	public static const ACTION_SCRIPT:String = "actionscript";

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
	 *  @param bubbles Specifies whether the event can bubble up the display list hierarchy.
	 *
	 *  @param cancelable Specifies whether the behavior associated with 
	 *  the event can be prevented.
	 *
	 *  @param info The index of the cue point in the cue point Array.
     */
	public function MetadataEvent(type:String, bubbles:Boolean = false,
								  cancelable:Boolean = false,
								  info:Object = null) 
	{
		super(type, bubbles, cancelable);

		this.info = info;
	}
	
    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

	//----------------------------------
	//  info
	//----------------------------------

    /**
	 *  The index of the cue point in the cue point Array.
     */    
	public var info:Object;

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
		return new MetadataEvent(type, bubbles, cancelable, info);
	}
}

}
