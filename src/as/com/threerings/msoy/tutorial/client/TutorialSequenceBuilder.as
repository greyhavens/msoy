//
// $Id$

package com.threerings.msoy.tutorial.client {

/**
 * Builds a tutorial sequence.
 */
public class TutorialSequenceBuilder
{
    /** @private */
    public function TutorialSequenceBuilder (id :String, director :TutorialDirector)
    {
        _sequence = new TutorialSequence(id);
        _director = director;
    }

    /**
     * Creates a new builder for a suggestion to eventually be added to the sequence.
     */
    public function newSuggestion (text :String) :TutorialItemBuilder
    {
        var id :String = _sequence.id + ":" + _sequence.size();
        var item :TutorialItem = new TutorialItem(Kind.SUGGESTION, id, text);
        item.ignorable = false;
        return new TutorialItemBuilder(item, _director, this);
    }

    /**
     * Sets the limiting function for the sequence we are building.
     * @param checkAvailable the predicate to test if the sequence should still be available, for
     *        example a room publishing sequence should not activate if the user has gone into a
     *        game. The prototype of the function should that of a standard predicate, as follows:
     *        <listing>
     *            function availableFn () :Boolean;
     *        </listing>
     */
    public function limit (checkAvailable :Function) :TutorialSequenceBuilder
    {
        _sequence.checkAvailable = checkAvailable;
        return this;
    }

    /**
     * Sets the sequence to show its items one at a time. Each time the sequence is activated, the
     * next item is shown. The default behavior is to show each items in succession until complete,
     * or restart on each activation if cancelled.
     */
    public function singles () :TutorialSequenceBuilder
    {
        _sequence.singles = true;
        return this;
    }

    /**
     * Don't store the progress of this tutorial, and show it every time.
     */
    public function showAlways () :TutorialSequenceBuilder
    {
        _sequence.persisted = false;
        return this;
    }

    /**
     * Limit the sequence for display exclusively to new users.
     */
    public function newbie () :TutorialSequenceBuilder
    {
        _levels = Levels.NEWBIE;
        return this;
    }

    /**
     * Limit the sequence for display exclusively to beginner users.
     */
    public function beginner () :TutorialSequenceBuilder
    {
        _levels = Levels.BEGINNER;
        return this;
    }

    /**
     * Attempts to activate the sequence and returns true if the activation was successful. The
     * sequence fails to activate if the user has already seen all the items in the sequence or
     * if the sequence is not currently available (including the level range requirement). Once
     * activated, the first unviewed item in the sequence will popup immediately. When a sequence
     * item popup is closed, the next item will popup and so on until all items are viewed. If any
     * item in a sequence is unavailable, the system will stall and recheck it periodically until
     * it is ready to be shown. This allows sequences to wait for a user action.
     * @param dismiss if set and there is a sequence currently active, then that sequence will be
     *        deactivated, including closure of the popup panel, and this one activated instead
     */
    public function activate (dismiss :Boolean = false) :Boolean
    {
        _sequence.checkAvailable = Levels.makeCheck(
            _levels, _director.getMemberLevel, _sequence.checkAvailable);
        var activated :Boolean = _director.activateSequence(_sequence, dismiss);
        _director = null;
        _sequence = null;
        return activated;
    }

    internal function queue (item :TutorialItem) :void
    {
        _sequence.items.push(item);
    }

    protected var _director :TutorialDirector;
    protected var _sequence :TutorialSequence;
    protected var _levels :Levels;
}
}
