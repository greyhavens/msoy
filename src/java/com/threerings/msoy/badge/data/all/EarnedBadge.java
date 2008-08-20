//
// $Id$

package com.threerings.msoy.badge.data.all;

import com.threerings.msoy.data.all.DeploymentConfig;

public class EarnedBadge extends Badge
{
    /**
     * When this badge was earned.
     * Long, instead of a long, because we can't stream longs to the ActionScript client.
     */
    public Long whenEarned;

    /** Constructs a new empty EarnedBadge. */
    public EarnedBadge ()
    {
    }

    /** Constructs a new EarnedBadge with the specified type. */
    public EarnedBadge (int badgeCode, int level, String levelUnits, int coinValue, long whenEarned)
    {
        super(badgeCode, level, levelUnits, coinValue);
        this.whenEarned = whenEarned;
    }

    public String toString ()
    {
        return "badgeCode=" + badgeCode + " level=" + level + " whenEarned=" + whenEarned;
    }

    public static String getImageUrl (int badgeCode, int level)
    {
        return DeploymentConfig.staticMediaURL + BADGE_IMAGE_DIR + Integer.toHexString(badgeCode) +
            "_" + level + "f" + BADGE_IMAGE_TYPE;
    }

    @Override // from Badge
    public String imageUrl ()
    {
        return getImageUrl(badgeCode, level);
    }
}
