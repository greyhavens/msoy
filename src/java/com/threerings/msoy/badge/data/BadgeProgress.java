//
// $Id$

package com.threerings.msoy.badge.data;

public class BadgeProgress
{
    String unitName;
    int requiredUnits;
    int acquiredUnits;

    public BadgeProgress (String unitName, int requiredUnits, int acquiredUnits)
    {
        this.unitName = unitName;
        this.requiredUnits = requiredUnits;
        this.acquiredUnits = acquiredUnits;
    }

    public boolean isComplete ()
    {
        return acquiredUnits >= requiredUnits;
    }

    public float getPercentComplete ()
    {
        return Math.max((float)acquiredUnits / (float)requiredUnits, 1f);
    }
}
