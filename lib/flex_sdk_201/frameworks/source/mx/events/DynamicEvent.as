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
 *  This subclass of Event is dynamic, meaning that you can set
 *  arbitrary event properties on its instances at runtime.
 *
 *  <p>By contrast, Event and its other subclasses are non-dynamic,
 *  meaning that you can only set properties that are declared
 *  in those classes.
 *  When prototyping an application, using a DynamicEvent can be
 *  easier because you don't have to write an Event subclass
 *  to declare the properties in advance.
 *  However, you should eventually eliminate your DynamicEvents
 *  and write Event subclasses because these are faster and safer.
 *  A DynamicEvent is so flexible that the compiler can't help you
 *  catch your error when you set the wrong property or assign it
 *  a value of an incorrect type.</p>
 *
 *  <p>Example:</p>
 *
 *  <pre>
 *  var event:DynamicEvent = new DynamicEvent("credentialsChanged");
 *  event.name = name;
 *  event.passsword = password; // misspelling won't be caught!
 *  dispatchEvent(event);
 *  </pre>
 */
public dynamic class DynamicEvent extends Event
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
	 *  @param type The event type; indicates the action that caused the event.
	 *
	 *  @param bubbles Specifies whether the event can bubble up
	 *  the display list hierarchy.
	 *
	 *  @param cancelable Specifies whether the behavior
	 *  associated with the event can be prevented.
	 */
	public function DynamicEvent(type:String, bubbles:Boolean = false,
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
		return new DynamicEvent(type, bubbles, cancelable);
	}
}

}
