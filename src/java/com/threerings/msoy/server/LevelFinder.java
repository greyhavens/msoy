//
// $Id$

package com.threerings.msoy.server;

import java.util.Arrays;

import com.google.common.base.Preconditions;
import com.threerings.msoy.data.all.CoinAwards;

/**
 * Calculates coin values necessary to reach all levels. Provides forward and reverse lookup and
 * is read-only therefore thread safe.
 */
public class LevelFinder
{
    /** We assume users start with this much money. Those who don't create a profile therefore
     * start in the hole, but that just means it will take longer to get to level 2. */
    public static int STARTING_COINS = CoinAwards.CREATED_ACCOUNT + CoinAwards.CREATED_PROFILE;

    /** Maximum level supported by this class. Yes, everything could be dynamic but there is not
      * much of a use case and this needs to be multithreaded which makes that more complicated. */
    public static int MAX_LEVEL = 1000;

    /**
     * Creates a new level finder.
     */
    public LevelFinder ()
    {
        // initialize our internal array of memoized coin values per level.  Start with 256
        // calculated levels
        _coins = new int[MAX_LEVEL];
        for (int ii = 0; ii < BEGINNING_COIN_LEVELS.length; ii++) {
            // augment the value so the account creation does not cause level 3 to happen
            _coins[ii] = BEGINNING_COIN_LEVELS[ii] + STARTING_COINS;
        }
        calculate(_coins, BEGINNING_COIN_LEVELS.length);
    }

    /**
     * Finds the level placement for the given accumulated coins.
     */
    public int findLevel (int accCoins)
    {
        int level = Arrays.binarySearch(_coins, accCoins);
        if (level < 0) {
            level = -1 * level - 1;

            // if the array isn't big enough, return the max
            if (level == _coins.length) {
                return MAX_LEVEL;
            }

            // this is already 1-based because we want the slot prior to the insertion point, but
            // we need to take the max since our first coin level is 1500
            return Math.max(level, 1);
        }

        // on the nose, convert to 1-based level
        return level + 1;
    }

    /**
     * Get the number of coins required to attain the given level.
     */
    public int getCoinsForLevel (int level)
    {
        level = Math.max(Math.min(level - 1, _coins.length), 0);
        return _coins[level];
    }

    /**
     * Fill in the given array with coin amounts required to reach subsequent levels, assuming
     * the value at fromIndex - 1 is already filled in.
     */
    protected static void calculate (int[] coins, int fromIndex)
    {
        Preconditions.checkArgument(fromIndex > 0);
        Preconditions.checkArgument(coins.length > 0);

        // This equation governs the total coin requirement for a given level (n):
        // coin(n) = coin(n-1) + ((n-1) * 17.8 - 49) * (3000 / 60)
        // where (n-1) * 17.8 - 49 is the equation discovered by PARC researchers that correlates
        // to the time (in minutes) it takes the average WoW player to get from level n-1 to level
        // n, and 3000 is the expected average flow per hour that we hope to drive our system on.
        for (int ii = fromIndex; ii < coins.length; ii++) {
            coins[ii] = coins[ii-1] + (int)((ii * 17.8 - 49) * (3000 / 60));
        }
    }

    /** The array of memoized coin values for each level.  The first few levels are hard coded, the
     * rest are calculated according to the equation in calculateLevelsForFlow() */
    protected int[] _coins;

    /** The required coins for the first few levels is hard-coded. Note that STARTING_MONEY is
      * added to each entry when the initial coin amounts are set up. */
    protected static final int[] BEGINNING_COIN_LEVELS = { 0, 300, 900, 1800, 3000, 5100, 8100 };
}

