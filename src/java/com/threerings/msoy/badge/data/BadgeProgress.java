//
// $Id$

package com.threerings.msoy.badge.data;

/**
 * Information about the progress in being awarded a new badge.
 */
public class BadgeProgress
{
    /** The highest level that the user is currently qualified for. */
    public int highestLevel;

    /** Numer of units required to reach the next level. */
    public int nextLevelRequiredUnits;

    /** Number of units the user has acquired to reach the next level. */
    public int nextLevelAcquiredUnits;

    public BadgeProgress (int highestLevel, int nextLevelRequiredUnits, int nextLevelAcquiredUnits)
    {
        this.highestLevel = highestLevel;
        this.nextLevelRequiredUnits = nextLevelRequiredUnits;
        this.nextLevelAcquiredUnits = nextLevelAcquiredUnits;
    }

    public float getNextLevelProgress ()
    {
        return (nextLevelRequiredUnits > 0 ?
            Math.min((float)nextLevelAcquiredUnits / (float)nextLevelRequiredUnits, 1f) : 0);
    }
}
