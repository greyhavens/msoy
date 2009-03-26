//
// $Id$

package com.threerings.msoy.chat.data;

import com.samskivert.util.ArrayUtil;
import com.threerings.crowd.chat.data.ChatCodes;

public interface MsoyChatCodes extends ChatCodes
{
    /** A user chat mode for a paid broadcast message. */
    public static final byte PAID_BROADCAST_MODE = 5;

    /** String translations for the various chat modes. */
    public static final String[] XLATE_MODES =
        ArrayUtil.append(ChatCodes.XLATE_MODES, "paidbroadcast");
}
