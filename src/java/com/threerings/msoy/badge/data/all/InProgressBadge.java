//
// $Id$

package com.threerings.msoy.badge.data.all;


public class InProgressBadge extends Badge
{
    /** A String representation of the progress that has been made on this badge. */
    public String progress;

    /** Constructs a new empty EarnedBadge. */
    public InProgressBadge ()
    {
    }

    public InProgressBadge (int badgeCode, String progress)
    {
        super(badgeCode);

        this.progress = progress;
    }
}
