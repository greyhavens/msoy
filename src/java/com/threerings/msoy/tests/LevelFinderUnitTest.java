//
// $Id$

package com.threerings.msoy.tests;

import java.util.Arrays;

import org.junit.Assert;
import org.junit.Test;

import com.threerings.msoy.server.LevelFinder;

public class LevelFinderUnitTest
{
    @Test public void testCoinsForLevel ()
    {
        LevelFinder levels = new LevelFinder();
        Assert.assertTrue(levels.getCoinsForLevel(0) >= STARTING_COINS);
        Assert.assertTrue(levels.getCoinsForLevel(1) >= STARTING_COINS);
        // test that the difference between coins of subsequent levels keeps going up
        for (int ii = 2, lastDelta = 0; ii <= 200; ++ii) {
            int delta = levels.getCoinsForLevel(ii) - levels.getCoinsForLevel(ii - 1);
            if (delta <= lastDelta) {
                Assert.assertTrue("Coin delta did not increase [level=" + ii + ", delta=" + delta +
                    ", lastDelta=" + lastDelta + "]", delta > lastDelta);
            }
            lastDelta = delta;
        }
    }

    @Test public void testFindLevel ()
    {
        LevelFinder levels = new LevelFinder();
        Assert.assertTrue(levels.findLevel(0) == 1);
        Assert.assertTrue(levels.findLevel(STARTING_COINS) == 1);
        for (int coins = STARTING_COINS, lastLevel = 1; coins <= 1000000; coins += 1000) {
            int level = levels.findLevel(coins);
            if (level < lastLevel) {
                Assert.assertTrue("Level decreased [coins=" + coins + "]", level >= lastLevel);
            }
            lastLevel = level;
        }
    }

    @Test public void testAgainstOldCode ()
    {
        LevelFinder levels = new LevelFinder();
        OldCode oldCode = new OldCode();
        // I'm fine letting the new code disagree with the starting coins
        for (int coins = 0; coins < STARTING_COINS; coins += 100) {
            Assert.assertEquals(1, levels.findLevel(coins));
            Assert.assertEquals(0, oldCode.findLevel(coins));
        }
        int MAX_TEST = 1000000;
        int level = 0;
        for (int coins = STARTING_COINS; coins < MAX_TEST; coins += 100) {
            Assert.assertEquals("level mismatch for coins=" + coins,
                level = levels.findLevel(coins), oldCode.findLevel(coins));
        }
        //System.out.println(level);
    }

    /** Relevant code copied from old version of MemberManager (and external references
      * removed). */
    protected static class OldCode
    {
        public OldCode ()
        {
            _levelForFlow = new int[256];
            for (int ii = 0; ii < BEGINNING_FLOW_LEVELS.length; ii++) {
                _levelForFlow[ii] = BEGINNING_FLOW_LEVELS[ii] + STARTING_COINS;
            }
            calculateLevelsForFlow(BEGINNING_FLOW_LEVELS.length);
        }

        public int findLevel (int accCoins)
        {
            int level = Arrays.binarySearch(_levelForFlow, accCoins);
            if (level < 0) {
                level = -1 * level - 1;
                final int length = _levelForFlow.length;
                if (level == length) {
                    final int[] temp = new int[length*2];
                    System.arraycopy(_levelForFlow, 0, temp, 0, length);
                    _levelForFlow = temp;
                    calculateLevelsForFlow(length);
                    return findLevel(accCoins);
                }
                level--;
            }
            level++;

            return level;
        }

        protected void calculateLevelsForFlow (int fromIndex)
        {
            for (int ii = fromIndex; ii < _levelForFlow.length; ii++) {
                _levelForFlow[ii] = _levelForFlow[ii-1] + (int)((ii * 17.8 - 49) * (3000 / 60));
            }
        }

        protected int[] _levelForFlow;
        protected static final int[] BEGINNING_FLOW_LEVELS = { 0, 300, 900, 1800, 3000, 5100, 8100 };
    }

    protected static final int STARTING_COINS = LevelFinder.STARTING_COINS;
}

