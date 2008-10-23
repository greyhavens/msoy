//
// $Id$

package com.threerings.msoy.badge.data.all;

import com.threerings.msoy.data.all.DeploymentConfig;
import com.threerings.msoy.data.all.MediaDesc;
import com.threerings.msoy.data.all.StaticMediaDesc;

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
        return DeploymentConfig.staticMediaURL + BADGE_IMAGE_DIR + getImageFilename() + 
            BADGE_IMAGE_TYPE;
    }
    
    // TODO: can this become abstract in Badge and replace Badge.imageUrl?
    public MediaDesc imageMedia ()
    {
        return new StaticMediaDesc(MediaDesc.IMAGE_PNG, "badge", getImageFilename());
    }
    
    public String toString ()
    {
        return "badgeCode=" + badgeCode + " nextLevel=" + level + " progress=" + progress;
    }
    
    protected String getImageFilename()
    {
        return Integer.toHexString(badgeCode) + "_" + (level > 0 ? (level - 1) + "f" : level + "e");
    }
}
