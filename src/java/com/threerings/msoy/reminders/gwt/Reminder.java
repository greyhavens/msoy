//
// $Id$

package com.threerings.msoy.reminders.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;

import com.threerings.msoy.game.data.all.Trophy;

/**
 * A message for display to the user reminding them to do something.
 */
public class Reminder
    implements IsSerializable
{
    /**
     * Provides a superclass for notification data types.
     */
    public static class Data
        implements IsSerializable
    {
    }

    /**
     * Specifies data for a trophy notification.
     */
    public static class TrophyData extends Data
    {
        /** The trophy that was earned. */
        public Trophy trophy;

        /** The game name. */
        public String gameName;

        /** The game description. */
        public String gameDesc;

        /** The game icon. */
        public String gameMediaURL;

        /**
         * Creates a new trophy data for serialization.
         */
        public TrophyData ()
        {
        }

        /**
         * Creates a new trophy data.
         */
        public TrophyData (Trophy trophy, String gameName, String gameDesc, String gameMediaURL)
        {
            this.trophy = trophy;
            this.gameName = gameName;
            this.gameDesc = gameDesc;
            this.gameMediaURL = gameMediaURL;
        }
    }

    /** The type of reminder. */
    public ReminderType type;

    /** The data for the reminder, if any. */
    public Data data;
}
