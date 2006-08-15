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
     * If an array element was updated, get the index, or -1 if not applicable.
     */
    public function get index () :int
    {
        return _index;
    }

    /**
     * Constructor.
     */
    public function PropertyChangedEvent (
        propName :String, newValue :Object, oldValue :Object, index :int = -1)
    {
        super(TYPE);
        _name = propName;
        _newValue = newValue;
        _oldValue = oldValue;
        _index = index;
    }

    override public function clone () :Event
    {
        return new PropertyChangedEvent(_name, _newValue, _oldValue, _index);
    }

    /** Our implementation details. */
    protected var _name :String;
    protected var _newValue :Object;
    protected var _oldValue :Object;
    protected var _index :int;
}
}
