//
// $Id$

package com.threerings.msoy.data {

import com.threerings.presents.data.InvocationCodes;

/**
 * General codes and constants for the whole shebang.
 */
public class MsoyCodes extends InvocationCodes
{
    /** Defines our base invocation services group. */
    public static const BASE_GROUP :String = "msoy.base";

    /** Defines our game invocation services group. */
    public static const GAME_GROUP :String = "msoy.game";

    /** Defines our world invocation services group. */
    public static const WORLD_GROUP :String = "msoy.world";

    /** Identifies our general message bundle. */
    public static const GENERAL_MSGS :String = "general";

    /** Identifies our chat message bundle. */
    public static const CHAT_MSGS :String = "chat";

    /** Identifies our game message bundle. */
    public static const GAME_MSGS :String = "game";

    /** Identifies our item message bundle. */
    public static const ITEM_MSGS :String = "item";

    /** Identifies our prefs message bundle. */
    public static const PREFS_MSGS :String = "prefs";

    /** Identifies our notification message bundle. */
    public static const NOTIFY_MSGS :String = "notify";

    /** Identifies our editing message bundle. */
    public static const EDITING_MSGS :String = "editing";
}
}
