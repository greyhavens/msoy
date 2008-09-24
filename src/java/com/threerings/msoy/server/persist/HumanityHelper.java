//
// $Id$

package com.threerings.msoy.server.persist;

import com.samskivert.util.IntIntMap;

import com.threerings.msoy.data.MsoyCodes;
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
        UserAction.Type type = UserAction.getActionByNumber(record.actionId);
        if (type == null) {
            return; // ignore legacy actions that we've nixed
        }

        switch (type) {
        // most actions we don't care about
        default:
            return;

        // some actions count towards a user's "activities"
        case UPDATED_PROFILE:
        case SENT_FRIEND_INVITE:
        case ACCEPTED_FRIEND_INVITE:
        case CREATED_ITEM:
        case BOUGHT_ITEM:
        case LISTED_ITEM:
            _activities++;
            return;

        // Playing games is our main concern, we want to track play time
        case PLAYED_GAME:
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
            return;
        }
    }

    /**
     * Called after all actions have been noted to comptue the new humanity assessment.
     */
    public int computeNewHumanity (int memberId, int currentHumanity, int secsSinceLast)
    {
        // compute the adjustment based on gameplay time and activities
        float adjust = computeAdjustment(memberId, secsSinceLast);

        // actually adjust their current value and bound it
        currentHumanity += Math.round(MsoyCodes.MAX_HUMANITY * adjust);
        return Math.max(Math.min(currentHumanity, MsoyCodes.MAX_HUMANITY), 0);
    }

    protected float computeAdjustment (int memberId, int secsSinceLast)
    {
        // start out with no adjustment
        float adjust = 0;

        // do some very straightforward and clearly labeled math to compute how much they've
        // been playing each day
        int totalTime = 0;
        for (IntIntMap.IntIntEntry entry : _timeInGames.entrySet()) {
            totalTime += entry.getIntValue();
        }
        float hoursOfPlay = totalTime / HOUR_IN_SECONDS;
        float daysSinceLast = secsSinceLast / (24 * HOUR_IN_SECONDS);
        float hoursPerDay = hoursOfPlay / daysSinceLast;

        // based on their average hours of play per day, adjust their humanity assessment, less
        // than two means more human, greater than four means increasingly less human,
        // between two and four hours is unadjusted
        if (hoursPerDay <= 2) {
            adjust += 0.1f;
        } else if (hoursPerDay > 6) {
            adjust += -0.2f;
        } else if (hoursPerDay > 4) {
            adjust += -0.1f;
        }

        // give them .05 credit for each activity-per-day they've done, up to 4 per day
        float activitiesPerDay = _activities / daysSinceLast;
        adjust += .05f * Math.min(4, activitiesPerDay);

        // TEMP: log this so that we can eyeball what's happening for a while
        if (adjust != 0) {
            log.info("Adjusting humanity [id=" + memberId + ", hpd=" + hoursPerDay +
                ", apd=" + activitiesPerDay + ", adjust=" + adjust + "].");
        }
        // END TEMP

        return adjust;
    }

    protected IntIntMap _timeInGames = new IntIntMap();

    protected int _activities;

    protected static final float HOUR_IN_SECONDS = 60 * 60;
}
