//
// $Id$

package com.threerings.msoy.badge.data.all;

import com.threerings.msoy.data.all.DeploymentConfig;

public class InProgressBadge extends Badge
{
    /** The badge level that the member is working towards. */
    public int nextLevel;

    /** The progress that has been made on the badge, in [0, 1) */
    public float progress;

    /** The number of coins that will be given for the successful completion of this badge. */
    public int coinReward;

    /** Constructs a new empty EarnedBadge. */
    public InProgressBadge ()
    {
    }

    public InProgressBadge (int badgeCode, int nextLevel, float progress, int coinReward)
    {
        super(badgeCode);

        this.nextLevel = nextLevel;
        this.progress = progress;
    }

    @Override // from Badge
    public String imageUrl ()
    {
        return DeploymentConfig.staticMediaURL + BADGE_IMAGE_DIR + Integer.toHexString(badgeCode) +
            "_" + (nextLevel > 0 ? (nextLevel - 1) + "f" : nextLevel + "e") + BADGE_IMAGE_TYPE;
    }

    @Override // from Badge
    public boolean equals (Object o)
    {
        if (o instanceof InProgressBadge) {
            InProgressBadge other = (InProgressBadge)o;
            return super.equals(other) && other.nextLevel == this.nextLevel &&
                other.progress == this.progress;
        }
        return false;
    }

    @Override // from Badge
    public int hashCode ()
    {
        return badgeCode * nextLevel + (int)(progress * 100);
    }

    public String toString ()
    {
        return "badgeCode=" + badgeCode + " nextLevel=" + nextLevel + " progress=" + progress;
    }
}
