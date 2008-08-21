//
// $Id$

package com.threerings.msoy.badge.data.all;

import com.threerings.msoy.data.all.DeploymentConfig;

public class InProgressBadge extends Badge
{
    /** The progress that has been made on the badge, in [0, 1).  A progress of -1 indicates that
     * progress is not appropriate for this level of this badge, and should not be displayed */
    public float progress;

    /** Constructs a new empty EarnedBadge. */
    public InProgressBadge ()
    {
    }

    public InProgressBadge (int badgeCode, int nextLevel, String levelUnits, int coinValue,
        float progress)
    {
        super(badgeCode, nextLevel, levelUnits, coinValue);

        this.progress = progress;
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
