//
// $Id$

package com.threerings.msoy.tutorial.client {

/**
 * A series of ordered tutorial suggestions. Used internally by the tutorial package.
 */
internal class TutorialSequence
{
    /** @private */
    public function TutorialSequence (id :String)
    {
        this.id = id;
    }

    /**
     * Shortcut to get the number of items in this sequence.
     */
    public function size () :int
    {
        return items.length;
    }

    /**
     * Checks the availability of this tutorial sequence for display.
     */
    public function isAvailable () :Boolean
    {
        return checkAvailable == null || checkAvailable();
    }

    internal var id :String;
    internal var items :Array = [];
    internal var checkAvailable :Function;
    internal var singles :Boolean;
}

}
