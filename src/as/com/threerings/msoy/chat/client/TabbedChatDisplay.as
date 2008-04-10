// $Id$

package com.threerings.msoy.chat.client {

import com.threerings.crowd.chat.client.ChatDisplay

public interface TabbedChatDisplay extends ChatDisplay
{
    /**
     * Called when the tab that was responsible for the given localtype was closed.
     */
    function tabClosed (localtype :String) :void
}
