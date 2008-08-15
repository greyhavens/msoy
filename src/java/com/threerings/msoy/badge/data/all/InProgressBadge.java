//
// $Id$

package com.threerings.msoy.badge.data.all;

import com.threerings.msoy.data.all.DeploymentConfig;

public class InProgressBadge extends Badge
{
    /** The progress that has been made on the badge, in [0, 1) */
    public float progress;

    /** The number of coins that will be given for the successful completion of this badge. */
    public int coinReward;

    /** Constructs a new empty EarnedBadge. */
    public InProgressBadge ()
    {
    }

    public InProgressBadge (int badgeCode, int nextLevel, String levelUnits, float progress,
        int coinReward)
    {
        super(badgeCode, nextLevel, levelUnits);

        this.progress = progress;
        this.coinReward = coinReward;
    }

    @Override // from Badge
    public String imageUrl ()
    {
        return DeploymentConfig.staticMediaURL + BADGE_IMAGE_DIR + Integer.toHexString(badgeCode) +
            "_" + (level > 0 ? (level - 1) + "f" : level + "e") + BADGE_IMAGE_TYPE;
    }

    public String toString ()
    {
        return "badgeCode=" + badgeCode + " nextLevel=" + level + " progress=" + progress;
    }
}
