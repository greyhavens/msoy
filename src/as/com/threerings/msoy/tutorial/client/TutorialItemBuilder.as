//
// $Id$

package com.threerings.msoy.tutorial.client {

/**
 * Builder for tutorial items. Constructed by the tutorial director with all required item fields
 * in place. After desired mutation of values, the item may be queued to the director.
 */
public class TutorialItemBuilder
{
    /**
     * Creates a new builder for the given item. The given director is used to queue up the item.
     */
    public function TutorialItemBuilder (item :TutorialItem, director :TutorialDirector)
    {
        _item = item;
        _director = director;
    }

    /**
     * Sets the limiting function for the item we are building.
     * @param checkAvailable the predicate to test if the item should still be available, for
     *        example a room publishing suggestion should not popup if the user has gone into a
     *        game. The prototype of the function should that of a standard predicate, as follows:
     *        <listing>
     *            function availableFn () :Boolean;
     *        </listing>
     */
    public function limit (checkAvailable :Function) :TutorialItemBuilder
    {
        _item.checkAvailable = checkAvailable;
        return this;
    }

    /**
     * Adds a button for the item we are building.
     * @param text the text of the button, for example "Show Me"
     * @param onClick the function to call when the button is pressed. The function prototype
              should be paramterless and return void, as follows:
     *        <listing>
     *            function buttonFn () :void;
     *        </listing>
     */
    public function button (text :String, onClick :Function) :TutorialItemBuilder
    {
        _item.buttonText = text;
        _item.onClick = onClick;
        return this;
    }

    /**
     * Sets a flag on the item that prevents it from being ignored.
     */
    public function noIgnore () :TutorialItemBuilder
    {
        _item.ignorable = false;
        return this;
    }

    /**
     * Queues up the item to be displayed. The builder may no longer be used after this.
     */
    public function queue () :void
    {
        _director.queueItem(_item);
        _item = null;
        _director = null;
    }

    protected var _item :TutorialItem;
    protected var _director :TutorialDirector;
}
}
