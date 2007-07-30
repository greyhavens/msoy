//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.util.IntIntMap;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.UserAction;

import static com.threerings.msoy.Log.log;

/**
 * Handles the updating of a player's humanity asessment. And, as we all know, humanity can use all
 * the help it can get.
 */
public class HumanityHelper
{
    /**
     * Called for each action taken by the member since their last humanity assessment.
     */
    public void noteRecord (MemberActionLogRecord record)
    {
        // right now we only care about time spent playing games
        if (record.actionId != UserAction.PLAYED_GAME.getNumber()) {
            return;
        }

        // the data is "gameId seconds"
        String[] data = record.data.split("\\s");
        if (data.length == 2) {
            try {
                int gameId = Integer.parseInt(data[0]);
                int seconds = Integer.parseInt(data[1]);
                _timeInGames.increment(gameId, seconds);
            } catch (Exception e) {
                log.warning("Malformed PLAYED_GAME details " + record + ".");
            }
        } else {
            log.warning("Malformed PLAYED_GAME details " + record + ".");
        }
    }

    /**
     * Called after all actions have been noted to comptue the new humanity assessment.
     */
    public int computeNewHumanity (int memberId, int currentHumanity, int secsSinceLast)
    {
        // this will  be multiplied by MAX_HUMANITY and added to their current value
        double adjust = 0;

        // compute an adjustment based on gameplay time
        adjust += computeGameplayAdjustment(memberId, secsSinceLast);

        // actually adjust their current value and bound it
        currentHumanity += (int)Math.round(MemberObject.MAX_HUMANITY * adjust);
        return Math.max(Math.min(currentHumanity, MemberObject.MAX_HUMANITY), 0);
    }

    protected double computeGameplayAdjustment (int memberId, int secsSinceLast)
    {
        // do some very straightforward and clearly labeled math
        int totalTime = 0;
        for (IntIntMap.IntIntEntry entry : _timeInGames.entrySet()) {
            totalTime += entry.getIntValue();
        }
        double hoursOfPlay = totalTime / HOUR_IN_SECONDS;
        double daysSinceLast = secsSinceLast / (24 * HOUR_IN_SECONDS);
        double hoursPerDay = hoursOfPlay / daysSinceLast;

        // based on their average hours of play per day, adjust their humanity assessment, less
        // than two means more human, greater than four means increasingly less human
        double adjust = 0;
        if (hoursPerDay <= 0) {
            // no adjustments due to gameplay time
        } else if (hoursPerDay <= 2) {
            adjust = 0.1;
        } else if (hoursPerDay > 6) {
            adjust = -0.2;
        } else if (hoursPerDay > 4) {
            adjust = -0.1;
        } else /* (hoursPerDay > 2) */ {
            // no adjustments due to gameplay time
        }

        // TEMP: log this so that we can eyeball what's happening for a while
        if (adjust != 0) {
            log.info("Adjusting humanity [id=" + memberId + ", hpd=" + hoursPerDay +
                     ", adjust=" + adjust + "].");
        }
        // END TEMP

        return adjust;
    }

    protected IntIntMap _timeInGames = new IntIntMap();

    protected static final double HOUR_IN_SECONDS = 60 * 60;
}
