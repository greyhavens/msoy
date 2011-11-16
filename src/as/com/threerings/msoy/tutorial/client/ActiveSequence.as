//
// $Id$

package com.threerings.msoy.tutorial.client {

import com.threerings.msoy.client.Prefs;

/**
 * A tutorial sequence that is currently on display by the director.
 */
internal class ActiveSequence
{
    /**
     * Creates a new active sequence.
     */
    public function ActiveSequence (sequence :TutorialSequence)
    {
        _sequence = sequence;
        _progress = Prefs.getTutorialProgress(_sequence.id);
    }

    /**
     * Gets the current item to display.
     */
    public function get item () :TutorialItem
    {
        if (_progress < _sequence.items.length) {
            return _sequence.items[_progress];
        }
        return null;
    }

    /**
     * Marks the current item as shown and returns true if the next item should be shown too.
     */
    public function advance () :Boolean
    {
        var complete :Boolean = ++_progress >= _sequence.size();
        if (complete) {
            _progress = int.MAX_VALUE;
        }
        if (_sequence.singles || complete) {
            if (_sequence.isPersisted()) {
                Prefs.setTutorialProgress(_sequence.id, _progress);
            }
            return false;
        }
        return true;
    }

    /**
     * Returns true if the given item is in the sequence.
     */
    public function hasItem (item :TutorialItem) :Boolean
    {
        return _sequence.items.indexOf(item) >= 0;
    }

    protected var _sequence :TutorialSequence;
    protected var _progress :int;
}
}
