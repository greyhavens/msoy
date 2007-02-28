//
// $Id$

package com.threerings.msoy.export {

import flash.display.DisplayObject;

/**
 * Dispatched as notification that the actor's appearance has changed.
 * getOrientation() and isMoving() should be re-queried to paint
 * the correct visual for the actor in its current state.
 *
 * @eventType com.threerings.msoy.export.ControlEvent.APPEARANCE_CHANGED
 */
[Event(name="appearanceChanged", type="com.threerings.msoy.export.ControlEvent")]


/**
 * Defines actions, accessors and callbacks available to all in-world mobiles. An mobile is
 * something that has an orientation in the scene and can request to change locations.
 */
public class ActorControl extends EntityControl
{
    /**
     * Creates a controller for a mobile. The display object is the mobile's visualization.
     */
    public function ActorControl (disp :DisplayObject)
    {
        super(disp);
    }

    /**
     * Returns our current location in the scene.
     *
     * @return an array containing [ x, y, z ]. x, y, and z are Numbers between 0 and 1 or null if
     * our location is unknown.
     */
    public function getLocation () :Array
    {
        return _location;
    }

    /**
     * Returns the current orientation of this mobile.
     *
     * @return a value between 0 (facing straight ahead) and 360.
     */
    public function getOrientation () :Number
    {
        return _orient;
    }

    /**
     * Returns whether the mobile is currently moving between locations in the scene.
     */
    public function isMoving () :Boolean
    {
        return _isMoving;
    }

    /**
     * Requests that our location be updated. This will result in a call to {@link
     * #appearanceChanged} when the mobile starts moving and another when the mobile arrives at its
     * destination and stops moving.
     *
     * x, y, and z are Numbers between 0 and 1 indicating a percentage of the room's width, height
     * and depth respectively.  orient is a number between 0 (facing straight ahead) and 359.
     */
    public function setLocation (x :Number, y :Number, z: Number, orient :Number) :void
    {
        callHostCode("setLocation_v1", x, y, z, orient);
    }

    /**
     * Requests that our orientation be updated. This will result in a call to the {@link
     * #appearanceChanged} callback.
     */
    public function setOrientation (orient :Number) :void
    {
        callHostCode("setOrientation_v1", orient);
    }

    // from WhirledControl
    override protected function populateProperties (o :Object) :void
    {
        super.populateProperties(o);

        o["appearanceChanged_v1"] = appearanceChanged_v1;
    }

    /**
     * Called when we start or stop moving or change orientation.
     */
    protected function appearanceChanged_v1 (location :Array, orient :Number, moving :Boolean) :void
    {
        _location = location;
        _orient = orient;
        _isMoving = moving;
        dispatch(ControlEvent.APPEARANCE_CHANGED);
    }

    /** Contains our current location in the scene [x, y, z], or null. */
    protected var _location :Array;

    /** Our current orientation, or 0. */
    protected var _orient :Number = 0;

    /** Indicates whether or not we're currently moving. */
    protected var _isMoving :Boolean;
}
}
