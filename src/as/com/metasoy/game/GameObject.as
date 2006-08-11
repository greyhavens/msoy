package com.metasoy.game {

import flash.events.IEventDispatcher;

/**
 * The game object that you'll be using to manage your game.
 */
public interface GameObject
    extends IEventDispatcher
{
    /**
     * Data accessor.
     */
    function get data () :Object;
}
}
