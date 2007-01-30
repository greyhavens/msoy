//
// $Id$

package com.threerings.msoy.export {

import flash.events.Event;

public class ControlEvent extends Event
{
    /** An event type dispatched when an Actor has had its appearance
     * changed. Your code should react to this event and possibly redraw
     * the actor, taking into account the orientation and whether the
     * actor is moving.
     * key: unused
     * arg: unused
     */
    public static const APPEARANCE_CHANGED :String = "appearanceChanged";

    /** An event type dispatched when an event is triggered.
     * key: event name
     * arg: unused
     */
    public static const EVENT_TRIGGERED :String = "eventTriggered";

    /** An event type dispatched when this avatar speaks.
     * key: unused
     * arg: unused
     */
    public static const AVATAR_SPOKE :String = "avatarSpoke";

    /** An event type dispatched when this avatar is playing a custom action.
     * key: action name
     * arg: action argument
     */
    public static const ACTION_TRIGGERED :String = "actionTriggered";

    /** An event type dispatched when this client-side instance of the item
     * has gained "control" over the other client-side instances.
     * key: unused
     * arg: unused
     */
    public static const GOT_CONTROL :String = "gotControl";

    /** An event type dispatched when the memory has changed.
     * key: memory name
     * arg: memory value
     */
    public static const MEMORY_CHANGED :String = "memoryChanged";

    /**
     * Retrieve the event target, which will be the MsoyControl instance that
     * dispatched this event.
     */
    override public function get target () :Object
    {
        return super.target;
    }

    /**
     * Retrieve the 'key' for this event, which is a String value
     * whose meaning is determined by the event type.
     */
    public function get key () :String
    {
        return _key;
    }

    /**
     * Retrieve the object 'arg' for this event, which is a value
     * whose meaning is determined by the event type.
     */
    public function get arg () :Object
    {
        return _arg;
    }

    /**
     * Create a new ControlEvent.
     */
    public function ControlEvent (
        type :String, key :String = null, arg :Object = null)
    {
        super(type);
        _key = key;
        _arg = arg;
    }

    // documentation inherited from Event
    override public function clone () :Event
    {
        return new ControlEvent(type, _key, _arg);
    }

    /** Internal storage for our key property. */
    protected var _key :String;

    /** Internal storage for our arg property. */
    protected var _arg :Object;
}
}
