//
// $Id$

package com.threerings.msoy.badge.data;

public class BadgeProgress
{
    int highestLevel;
    int nextLevelRequiredUnits;
    int nextLevelAcquiredUnits;

    public BadgeProgress (int highestLevel, int nextLevelRequiredUnits, int nextLevelAcquiredUnits)
    {
        this.highestLevel = highestLevel;
        this.nextLevelRequiredUnits = nextLevelRequiredUnits;
        this.nextLevelAcquiredUnits = nextLevelAcquiredUnits;
    }

    public float getNextLevelProgress ()
    {
        return (nextLevelRequiredUnits > 0 ?
            Math.max((float)nextLevelAcquiredUnits / (float)nextLevelRequiredUnits, 1f) : 0);
    }
}
