//
// $Id$

package com.threerings.msoy.hood {

/**
 * Represents something in a neighborhood: currently either a friend or a group.
 */
public class Neighbor
{
    /** The number of members occupying this neighbor place. */
    public var population :int;

    /** The names of friends present in this place. */
    public var friends :Array;

    /** The id that represents this place (scene or game). */
    public var placeId :int;

    /** The name of this place. */
    public var name :String;

    /**
     * Populates an existing neighbor subclass with data common to this superclass.
     */
    public static function fromJSON (neighbor: Neighbor, JSON: Object) :void
    {
        neighbor.name = JSON.name;
        neighbor.population = JSON.pcount;
        neighbor.placeId = JSON.placeId;
        neighbor.friends = JSON.friends ? JSON.friends : [];
    }
}
}
