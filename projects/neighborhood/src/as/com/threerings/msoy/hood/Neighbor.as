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
    /** The scene id, if any, that represents this place. */
    public var sceneId :int;

    /**
     * Populates an existing neighbor subclass with data common to this superclass.
     */
    public static function fromJSON(neighbor: Neighbor, JSON: Object) :void
    {
        neighbor.population = JSON.pop;
        neighbor.sceneId = JSON.sceneId;
        neighbor.friends = JSON.friends ? JSON.friends : [];
    }

    /** Return a name for this neighbour. Must be overriden. */
    public function getName () :String
    {
        return null;
    }
}
}
