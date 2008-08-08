//
// $Id$

package com.threerings.msoy.badge.data.all;

public class InProgressBadge extends Badge
{
    /** The badge level that the member is working towards. */
    public int nextLevel;

    /** The progress that has been made on the badge, in [0, 1) */
    public float progress;

    /** Constructs a new empty EarnedBadge. */
    public InProgressBadge ()
    {
    }

    public InProgressBadge (int badgeCode, int nextLevel, float progress)
    {
        super(badgeCode);

        this.nextLevel = nextLevel;
        this.progress = progress;
    }
}
