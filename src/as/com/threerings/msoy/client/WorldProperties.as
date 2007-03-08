package com.threerings.msoy.client {

/**
 * A repository of bindable properties that define how the world is
 * currently behaving.
 */
public class WorldProperties
{
    /**
     * Should the placeview show chat?
     */
    [Bindable]
    // TODO: more cleanup. The ChatContainer should automagically disable
    // all chatoverlays except its own. // TODO: after GDC
    public var placeViewShowsChat :Boolean = true;

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
