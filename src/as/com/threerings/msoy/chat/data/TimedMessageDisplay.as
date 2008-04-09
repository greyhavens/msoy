//
// $Id$

package com.threerings.msoy.chat.data {

import flash.utils.getTimer; // function import

import com.threerings.util.Log;

import com.threerings.crowd.chat.data.ChatMessage;

/**
 * A class to keep track of a given Chat Message and the time at which it was shown to the player.
 */
public class TimedMessageDisplay
{
    public function TimedMessageDisplay (msg :ChatMessage) 
    {
        _msg = msg;
    }

    public function get msg () :ChatMessage
    {
        return _msg;
    }

    /**
     * Returns the time at which this message was displayed, or -1 if it has not yet been displayed.
     */
    public function get displayedAt () :int
    {
        return _timestamp;
    }

    public function showingNow () :void
    {
        if (_timestamp == -1) {
            _timestamp = getTimer();
        } else {
            Log.getLog(this).debug("refusing to set timestamp that's already set [cur=" + 
                _timestamp + ", now=" + getTimer() + "]");
        }
    }

    protected var _msg :ChatMessage;
    protected var _timestamp :int = -1;
}
}
