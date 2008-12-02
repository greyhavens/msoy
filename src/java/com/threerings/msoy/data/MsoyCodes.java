//
// $Id$

package com.threerings.msoy.data;

import com.threerings.presents.data.InvocationCodes;

/**
 * General codes and constants for the whole shebang.
 */
public interface MsoyCodes extends InvocationCodes
{
    /** Defines our member invocation services group. */
    public static final String MEMBER_GROUP = "msoy.member";

    /** Defines our game invocation services group. */
    public static final String GAME_GROUP = "msoy.game";

    /** Defines our world invocation services group. */
    public static final String WORLD_GROUP = "msoy.world";

    /** The translation message bundle for our general client bits. */
    public static final String GENERAL_MSGS = "general";

    /** The translation message bundle for our world client bits. */
    public static final String WORLD_MSGS = "world";

    /** The translation message bundle for our chat messages. */
    public static final String CHAT_MSGS = "chat";

    /** The translation message bundle for our Java game applet. */
    public static final String GAME_MSGS = "game";

    /** The translation message bundle for our item messages. */
    public static final String ITEM_MSGS = "item";

    /** The translation message bundle for our prefs messages. */
    public static final String PREFS_MSGS = "prefs";

    /** The translation message bundle for our notification messages. */
    public static final String NOTIFY_MSGS = "notify";

    /** The translation message bundle for our room editing messages. */
    public static final String EDITING_MSGS = "editing";

    /** The translation message bundle for our passport messages. */
    public static final String PASSPORT_MSGS = "passport";

    /** The translation message bundle for our party messages. */
    public static final String PARTY_MSGS = "party";

    /** The translation message bundle for our studio messages. */
    public static final String STUDIO_MSGS = "studio";

    /** The translation message bundle for our home page grid messages. */
    public static final String HOME_PAGE_GRID_MSGS = "homepagegrid";

    /** The translation message bundle for our group services. */
    public static final String GROUP_MSGS = "group";

    /** The translation message bundle for our stats services. */
    public static final String STATS_MSGS = "stats";

    /** The humanity value for a fully actualized human. */
    public static final int MAX_HUMANITY = 255;

    /** The humanity value for a brand new user. */
    public static final int STARTING_HUMANITY = MAX_HUMANITY/2;
}
