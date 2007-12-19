////////////////////////////////////////////////////////////////////////////////
//
//  Copyright (C) 2003-2007 Adobe Systems Incorporated
//  All Rights Reserved. The following is Source Code and is subject to all
//  restrictions on such code as contained in the End User License Agreement
//  accompanying this product.
//
////////////////////////////////////////////////////////////////////////////////

package mx.events
{

import flash.events.Event;
import flash.events.ProgressEvent;

/**
 *  The ResourceEvent class represents an event object that is dispatched
 *  when the ResourceManager loads the resource bundles in a resource module
 *  by calling loadResourceModule().
 *
 *  @see mx.resources.ResourcManager
 */
public class ResourceEvent extends ProgressEvent
{
    include "../core/Version.as";

    //--------------------------------------------------------------------------
    //
    //  Class constants
    //
    //--------------------------------------------------------------------------

    /**
     *  Dispatched when the resource module SWF has finished downloading.     
     *  The <code>ResourceEvent.COMPLETE</code> constant defines the value of the 
     *  <code>type</code> property of the event object for a <code>complete</code> event.
     *
     *  <p>The properties of the event object have the following values:</p>
     *  <table class="innertable">
     *     <tr><th>Property</th><th>Value</th></tr>
     *     <tr><td><code>bubbles</code></td><td>false</td></tr>
     *     <tr><td><code>cancelable</code></td><td>false</td></tr>
     *     <tr><td><code>currentTarget</code></td><td>The Object that defines the 
     *       event listener that handles the event. For example, if you use 
     *       <code>myButton.addEventListener()</code> to register an event listener, 
     *       myButton is the value of the <code>currentTarget</code>. </td></tr>
     *     <tr><td><code>errorText</code></td><td>Empty</td></tr>
     *     <tr><td><code>target</code></td><td>The Object that dispatched the event; 
     *       it is not always the Object listening for the event. 
     *       Use the <code>currentTarget</code> property to always access the 
     *       Object listening for the event.</td></tr>
     *  </table>
     *
     *  @eventType complete
     */
    public static const COMPLETE:String = "complete";
    
    /**
     *  Dispatched when there is an error downloading the resource module SWF.
     *  The <code>ResourceEvent.ERROR</code> constant defines the value of the 
     *  <code>type</code> property of the event object for a <code>error</code> event.
     *
     *  <p>The properties of the event object have the following values:</p>
     *  <table class="innertable">
     *     <tr><th>Property</th><th>Value</th></tr>
     *     <tr><td><code>bubbles</code></td><td>false</td></tr>
     *     <tr><td><code>bytesLoaded</code></td><td>Empty</td></tr>
     *     <tr><td><code>bytesTotal</code></td><td>Empty</td></tr>
     *     <tr><td><code>cancelable</code></td><td>false</td></tr>
     *     <tr><td><code>currentTarget</code></td><td>The Object that defines the 
     *       event listener that handles the event. For example, if you use 
     *       <code>myButton.addEventListener()</code> to register an event listener, 
     *       myButton is the value of the <code>currentTarget</code>. </td></tr>
     *     <tr><td><code>errorText</code></td>An error message.<td></td></tr>
     *     <tr><td><code>target</code></td><td>The Object that dispatched the event; 
     *       it is not always the Object listening for the event. 
     *       Use the <code>currentTarget</code> property to always access the 
     *       Object listening for the event.</td></tr>
     *  </table>
     *
     *  @eventType error
     */
    public static const ERROR:String = "error";

    /**
     *  Dispatched when the resource module SWF is downloading.
     *  The <code>ResourceEvent.PROGRESS</code> constant defines the value of the 
     *  <code>type</code> property of the event object for a <code>progress</code> event.
     *
     *  <p>The properties of the event object have the following values:</p>
     *  <table class="innertable">
     *     <tr><th>Property</th><th>Value</th></tr>
     *     <tr><td><code>bubbles</code></td><td>false</td></tr>
     *     <tr><td><code>bytesLoaded</code></td><td>The number of bytes loaded.</td></tr>
     *     <tr><td><code>bytesTotal</code></td><td>The total number of bytes to load.</td></tr>
     *     <tr><td><code>cancelable</code></td><td>false</td></tr>
     *     <tr><td><code>currentTarget</code></td><td>The Object that defines the 
     *       event listener that handles the event. For example, if you use 
     *       <code>myButton.addEventListener()</code> to register an event listener, 
     *       myButton is the value of the <code>currentTarget</code>. </td></tr>
     *     <tr><td><code>errorText</code></td>Empty<td></td></tr>
     *     <tr><td><code>target</code></td><td>The Object that dispatched the event; 
     *       it is not always the Object listening for the event. 
     *       Use the <code>currentTarget</code> property to always access the 
     *       Object listening for the event.</td></tr>
     *  </table>
     *
     *  @eventType progress
     */
    public static const PROGRESS:String = "progress"; 
    
    //--------------------------------------------------------------------------
    //
    //  Constructor
    //
    //--------------------------------------------------------------------------

    /**
     *  Constructor.
     * 
     *  @param type The type of the event. Possible values are:
     *  <ul>
     *     <li>"progress" (<code>ResourceEvent.PROGRESS</code>);</li>
     *     <li>"complete" (<code>ResourceEvent.COMPLETE</code>);</li>
     *     <li>"error" (<code>ResourceEvent.ERROR</code>);</li>
     *  </ul>
     *
     *  @param bubbles Determines whether the Event object
	 *  participates in the bubbling stage of the event flow.
     *
     *  @param cancelable Determines whether the Event object can be cancelled.
     *
     *  @param bytesLoaded The number of bytes loaded
	 *  at the time the listener processes the event.
     *
     *  @param bytesTotal The total number of bytes
	 *  that will ultimately be loaded if the loading process succeeds.
     *
     *  @param errorText The error message of the error
	 *  when type is ResourceEvent.ERROR.
     *
     *  @tiptext Constructor for <code>ResourceEvent</code> objects.
     */    
    public function ResourceEvent(type:String, bubbles:Boolean = false,
                                  cancelable:Boolean = false,
                                  bytesLoaded:uint = 0, bytesTotal:uint = 0,
                                  errorText:String = null)
    {
        super(type, bubbles, cancelable, bytesLoaded, bytesTotal);
        
        this.errorText = errorText;
    }
    
    //--------------------------------------------------------------------------
    //
    //  Properties
    //
    //--------------------------------------------------------------------------

    //----------------------------------
    //  errorText
    //----------------------------------

    /**
     *  The error message if the <code>type</code> is <code>ERROR</code>;
	 *  otherwise, it is <code>null</code>.
     */
    public var errorText:String;
    
    //--------------------------------------------------------------------------
    //
    //  Overridden properties: Event
    //
    //--------------------------------------------------------------------------

    /**
     *  @private
     */
    override public function clone():Event
    {
        return new ResourceEvent(type, bubbles, cancelable,
                                 bytesLoaded, bytesTotal, errorText);
    }
}

}
