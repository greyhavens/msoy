//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.crowd.chat.client.ChatDisplay;

import com.threerings.crowd.chat.data.ChatMessage;

import com.threerings.msoy.chat.data.TimedMessageDisplay;

public class HistoryList
    implements ChatDisplay
{
    /**
     * @return the current size of the history.
     */
    public function size () :int
    {
        return _history.length;
    }

    /**
     * Get the history entry at the specified index.
     */
    public function get (index :int) :TimedMessageDisplay
    {
        return (_history[index] as TimedMessageDisplay);
    }

    // from interface ChatDisplay
    public function clear () :void
    {
        var size :int = _history.length;
        _history.length = 0; // truncate the array
    }

    // from interface ChatDisplay
    public function displayMessage (msg :ChatMessage, alreadyDisplayed :Boolean) :Boolean
    {
        if (_history.length == MAX_HISTORY) {
            _history.splice(0, PRUNE_HISTORY);
        }
        _history.push(new TimedMessageDisplay(msg));
        return false;
    }

    /** The maximum number of history entries we'll keep. */
    protected static const MAX_HISTORY :int = 1000;

    /** The number of history entries we'll prune when we hit the max. */
    protected static const PRUNE_HISTORY :int = 100;

    /** The array in which we store historical chat. */
    protected var _history :Array = new Array();
}
}
