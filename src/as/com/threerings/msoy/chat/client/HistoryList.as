//
// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.util.ObserverList;

import com.threerings.crowd.chat.data.ChatMessage;
import com.threerings.crowd.chat.data.SystemMessage;

import com.threerings.msoy.chat.data.TimedMessageDisplay;

public class HistoryList
{
    /**
     * @return the current size of the history.
     */
    public function size () :int
    {
        return _history.length;
    }

    /**
     * Clear the history.
     */
    public function clear () :void
    {
        var size :int = _history.length;
        _history.length = 0; // truncate the array
        notify(size);
    }

    /**
     * Get the history entry at the specified index.
     */
    public function get (index :int) :TimedMessageDisplay
    {
        return (_history[index] as TimedMessageDisplay);
    }

    /**
     * Add a chat overlay to the list of those interested in hearing about history changes.
     */
    public function addChatOverlay (overlay :ChatOverlay) :void
    {
        _obs.add(overlay);
    }

    /**
     * Add a chat overlay to the list of those interested in hearing about history changes.
     */
    public function removeChatOverlay (overlay :ChatOverlay) :void
    {
        _obs.remove(overlay);
    }

    /**
     * Records the supplied chat message to this history list. Bumps off the oldest message if the
     * history list is at its maximum size. Notifies any chat overlay observers.
     */
    public function addMessage (msg :ChatMessage) :void
    {
        var adjusted :int;
        if (_history.length == MAX_HISTORY) {
            _history.splice(0, PRUNE_HISTORY);
            adjusted = PRUNE_HISTORY;

        } else {
            adjusted = 0;
        }

        _history.push(new TimedMessageDisplay(msg));
        notify(adjusted);
    }

    /**
     * Filters out all transient (feedback, etc.) messages from this history.
     */
    public function filterTransient () :void
    {
        _history = _history.filter(function (item :TimedMessageDisplay, index :int, 
                                             array :Array) :Boolean {
            return !(item.msg is SystemMessage);
        });
    }

    /**
     * Notifies interested ChatOverlays that there has been a change to the history.
     */
    protected function notify (adjustment :int) :void
    {
        _obs.apply(function (overlay :ChatOverlay) :void {
            overlay.historyUpdated(adjustment);
        });
    }

    /** The array in which we store historical chat. */
    protected var _history :Array = new Array();

    /** A list of overlays interested in history. */
    protected var _obs :ObserverList = new ObserverList();

    /** The maximum number of history entries we'll keep. */
    protected static const MAX_HISTORY :int = 1000;

    /** The number of history entries we'll prune when we hit the max. */
    protected static const PRUNE_HISTORY :int = 100;
}
}
