//
// $Id$

package com.threerings.msoy.room.client {

/**
 * A repository of bindable properties that define how the world is
 * currently behaving.
 */
public class WorldProperties
{
    /**
     * Does the user have room control of their own avatar?
     * This affects:<ul>
     *   <li> clicking on locations
     *   <li> triggering avatar animations and states
     *   <li> changing your avatar
     * </ul>
     */
    [Bindable]
    public var userControlsAvatar :Boolean = true;
}
}
