//
// $Id$

package com.threerings.msoy.reminders.gwt;

/**
 * Defines the types of reminders.
 */
public enum ReminderType
{
    /** Reminds the user to bookmark the application (facebook). */
    BOOKMARK,

    /** Reminds the user that they have won a trophy recently and it has not yet been published. */
    TROPHY,

    /** Data-driven suggestive sell, tip etc. */
    PROMOTION
}
