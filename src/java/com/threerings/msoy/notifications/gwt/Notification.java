//
// $Id$

package com.threerings.msoy.notifications.gwt;

import com.google.gwt.user.client.rpc.IsSerializable;
import com.threerings.msoy.game.data.all.Trophy;

/**
 * A message for display to the user about something of interest, a suggestion or tip etc.
 */
public class Notification
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

        /**
         * Creates a new trophy data for serialization.
         */
        public TrophyData ()
        {
        }

        /**
         * Creates a new trophy data.
         */
        public TrophyData (Trophy trophy)
        {
            this.trophy = trophy;
        }
    }

    /** The type of notification. */
    public NotificationType type;

    /** The data for the message, if any. */
    public Data data;
}
