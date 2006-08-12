package com.metasoy.game {

import flash.events.Event;

/**
 * Property change events are dispatched after the property change was
 * validated on the server.
 */
public class PropertyChangedEvent extends Event
{
    /** The type of a property change event. */
    public static const TYPE :String = "PropChanged";

    /**
     * Get the name of the property that changed.
     */
    public function get name () :String
    {
        return _name;
    }

    /**
     * Get the property's new value.
     */
    public function get newValue () :Object
    {
        return _newValue;
    }

    /**
     * Get the property's previous value (handy!).
     */
    public function get oldValue () :Object
    {
        return _oldValue;
    }

    /**
     * Constructor.
     */
    public function PropertyChangedEvent (
            propName :String, newValue :Object, oldValue :Object)
    {
        super(TYPE);
        _name = propName;
        _newValue = newValue;
        _oldValue = oldValue;
    }

    override public function clone () :Event
    {
        return new PropertyChangedEvent(_name, _newValue, _oldValue);
    }

    protected var _name :String;
    protected var _newValue :Object;
    protected var _oldValue :Object;
}
}
