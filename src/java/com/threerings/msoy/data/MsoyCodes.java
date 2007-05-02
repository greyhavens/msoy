//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * General codes and constants for the whole shebang.
 */
public interface MsoyCodes extends InvocationCodes
{
    /** Defines our base invocation services group. */
    public static final String BASE_GROUP = "msoy.base";

    /** Defines our game invocation services group. */
    public static final String GAME_GROUP = "msoy.game";

    /** Defines our world invocation services group. */
    public static final String WORLD_GROUP = "msoy.world";

    /** The translation message bundle for our general client bits. */
    public static final String GENERAL_MSGS = "general";

    /** The translation message bundle for our chat messages. */
    public static final String CHAT_MSGS = "chat";

    /** The translation message bundle for our Java game applet. */
    public static final String GAME_MSGS = "game";

    /** The translation message bundle for our group services. */
    public static final String GROUP_MSGS = "group";

    /** The translation message bundle for our stats services. */
    public static final String STATS_MSGS = "stats";
}
