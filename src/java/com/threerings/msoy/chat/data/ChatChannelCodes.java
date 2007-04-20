//
// $Id$

package com.threerings.msoy.chat.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * Codes and constants relating to the chat channel services.
 */
public interface ChatChannelCodes extends InvocationCodes
{
    /** An error returned when a player requests to join or invite someone to a non-existent
     * channel. */
    public static final String E_NO_SUCH_CHANNEL = "e.no_such_channel";
}
