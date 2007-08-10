//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.game.data.PlayerObject;

import static com.threerings.msoy.Log.log;

/**
 * Handles the tracking and award of flow during a game. This is somewhat complexified by the fact
 * that we might be running in an AVR game on a world server or in a normal game on a game server
 * and we have to magically do the right thing. Yay magic.
 */
public class FlowAwardTracker
{
    /**
     * Initialize this flow award tracker.
     */
    public void init (int flowPerMinute, UserAction grantAction, String detailsPrefix)
    {
        _flowPerMinute = flowPerMinute;
        _grantAction = grantAction;
        _detailsPrefix = detailsPrefix;
    }

    /**
     * Get the flow per minute, or -1 if not initialized.
     */
    public int getFlowPerMinute ()
    {
        return _flowPerMinute;
    }

    /**
     * Start the clock ticking on flow accumulation.
     */
    public void startTracking ()
    {
        if (_tracking) {
            return;
        }
        _tracking = true;

        // note the time at which we started for flow calculations
        int startStamp = now();
        for (FlowRecord record : _flowRecords.values()) {
            record.beganStamp = startStamp;
        }
    }

    /**
     * Stop the clock ticking on flow accumulation.
     */
    public void stopTracking ()
    {
        if (!_tracking) {
            return;
        }
        _tracking = false;

        // note all remaining player's seconds played
        int endStamp = now();
        for (FlowRecord record : _flowRecords.values()) {
            // only track players that haven't already left
            if (record.beganStamp != 0) {
                record.secondsPlayed += endStamp - record.beganStamp;
                record.beganStamp = 0;
            }
        }
    }

    /**
     * Return the total number of seconds that members were being tracked.
     */
    public int getTotalTrackedSeconds ()
    {
        int total = _totalTrackedSeconds;

        int now = _tracking ? now() : 0;
        for (FlowRecord record : _flowRecords.values()) {
            total += record.secondsPlayed;

            if (_tracking && record.beganStamp != 0) {
                total += (now - record.beganStamp);
            }
        }

        return total;
    }

    /**
     * Start tracking the specified member.
     */
    public void addMember (int oid)
    {
        if (_flowRecords.containsKey(oid)) {
            return;
        }

        // create a flow record for this occupant
        int memberId = 0;
        double humanity = 0;

        // this is messy, but we'll sort it out later
        if (MsoyGameServer.isActive()) {
            PlayerObject player = (PlayerObject) MsoyGameServer.omgr.getObject(oid);
            if (player != null) {
                memberId = player.getMemberId();
                humanity = player.getHumanity();
            }

        } else if (MsoyServer.isActive()) {
            MemberObject member = (MemberObject) MsoyServer.omgr.getObject(oid);
            if (member != null) {
                memberId = member.getMemberId();
                humanity = member.getHumanity();
            }
        }

        if (memberId == 0) { // this should never happen
            log.warning("Failed to lookup member [oid=" + oid + "]");
            return;
        }

        // create a flow record to track awarded flow and remember things about the member we'll
        // need to know when they're gone
        FlowRecord record = new FlowRecord(memberId, humanity);
        _flowRecords.put(oid, record);

        // if we're currently tracking, note that they're "starting" immediately
        if (_tracking) {
            record.beganStamp = now();
        }
    }

    /**
     * Remove the member from being tracked and actually persist the flow they've
     * been awarded.
     */
    public void removeMember (int oid)
    {
        // remove their flow record and grant them the flow
        final FlowRecord record = _flowRecords.remove(oid);
        if (record == null) {
            log.warning("No flow record found [oid=" + oid + "]");
            return;
        }

        // if they're leaving in the middle of things, update their secondsPlayed,
        // just so that it's correct for calculations below
        if (_tracking) {
            record.secondsPlayed += now() - record.beganStamp;
            record.beganStamp = 0;
        }

        // since we're dropping this record, we need to record the seconds played
        _totalTrackedSeconds += record.secondsPlayed;

        // see if we even care
        if (record.awarded == 0 || record.memberId == MemberName.GUEST_ID) {
            return;
        }

        // see if we're initialized
        if (_flowPerMinute == -1) {
            log.warning("Unknown flow rate, but there's a grant. Wha?");
            return;
        }

        // see how much they actually get (also uses their secondsPlayed)
        int flowBudget = (int) ((record.humanity * _flowPerMinute * record.secondsPlayed) / 60);
        final int awarded = Math.min(record.awarded, flowBudget);
        final String details = _detailsPrefix + " " + record.secondsPlayed;

        // actually grant their flow award
        if (awarded > 0) {
            if (MsoyServer.isActive()) {
                MsoyServer.memberMan.grantFlow(record.memberId, awarded, _grantAction, details);

            } else if (MsoyGameServer.isActive()) {
                MsoyGameServer.invoker.postUnit(new Invoker.Unit("FlowAwardTracker.grantFlow") {
                    public boolean invoke () {
                        try {
                            MsoyGameServer.memberRepo.getFlowRepository().grantFlow(
                                record.memberId, awarded, _grantAction, details);
                        } catch (PersistenceException pe) {
                            log.log(Level.WARNING, "Failed to grant flow [mid=" + record.memberId +
                                    ", amount=" + awarded + ", action=" + _grantAction +
                                    ", details=" + details + "].", pe);
                        }
                        return false;
                    }
                });
            }
        }
    }

    /**
     * Remove all current members, granting them their flow.
     */
    public void shutdown ()
    {
        stopTracking();

        // do all the grants
        int[] oids = _flowRecords.intKeySet().toIntArray();
        for (int oid : oids) {
            removeMember(oid);
        }

        // put the kibosh
        _flowPerMinute = -1;
    }

    /**
     * Award an absolute amount of flow to the specified player.
     *
     * @return the actual flow awarded, or -1 if the playerOid is unknown.
     */
    public int awardFlow (int playerOid, int amount)
    {
        FlowRecord record = _flowRecords.get(playerOid);
        if (record == null) {
            return -1;
        }

        int available = getAwardableFlow(record);
        // the final amount of flow to pay out is accumulated in-memory and not
        // capped until the ending
        record.awarded += amount;

        // for immediate flow payouts that don't have to be precise, we try to make our estimate
        // more precise (nobody likes to see their flow actually drop at the end of a game) by
        // taking the cap into account
        int cappedAmount = Math.min(available, amount);
        if (cappedAmount > 0) {
            reportFlowAward(record.memberId, cappedAmount);
        }
        return cappedAmount;
    }

    /**
     * Award the specified player with a percentage of their maximum possible flow.
     *
     * @param percentage a number between 0 and 1, indicating the player's performance.
     */
    public int awardFlowPercentage (int playerOid, float percentage)
    {
        FlowRecord record = _flowRecords.get(playerOid);
        if (record == null) {
            return -1;
        }

        // bound the percentage in
        percentage = Math.max(0, Math.min(1, percentage));

        int amount = (int) Math.round(percentage * getAwardableFlow(record));
        if (amount > 0) {
            record.awarded += amount;
            reportFlowAward(record.memberId, amount);
        }
        return amount;
    }

    /**
     * Get the amount of flow that may be awarded to the specified player.
     */
    public int getAwardableFlow (int playerOid)
    {
        FlowRecord record = _flowRecords.get(playerOid);
        return (record == null) ? 0 : getAwardableFlow(record);
    }

    /**
     * Get the available flow that can be awarded to the player with the specified record.
     */
    protected int getAwardableFlow (FlowRecord record)
    {
        int secondsOfPlay = record.secondsPlayed;
        if (record.beganStamp != 0) {
            secondsOfPlay += now() - record.beganStamp;
        }
        int flowBudget = (int) ((record.humanity * _flowPerMinute * secondsOfPlay) / 60);
        // Don't let the available be less than 0.
        // The awarded amount can be higher than the budget up until the point of actual reward
        return Math.max(0, flowBudget - record.awarded);
    }

    /**
     * Convenience method to calculate the current timestmap in seconds.
     */
    protected static int now ()
    {
        return (int) (System.currentTimeMillis() / 1000);
    }

    protected void reportFlowAward (int memberId, int deltaFlow)
    {
        if (MsoyGameServer.isActive()) {
            MsoyGameServer.worldClient.reportFlowAward(memberId, deltaFlow);

        } else if (MsoyServer.isActive()) {
            MsoyServer.gameReg.reportFlowAward(null, memberId, deltaFlow);

        } else {
            log.warning("Can't report flow award, where the hell are we? [id=" + memberId +
                        ", flow=" + deltaFlow + "].");
        }
    }

    /**
     * A record of flow awarded, even for guests.
     */
    protected static class FlowRecord
    {
        protected double humanity;
        protected int memberId;
        protected int awarded;
        protected int beganStamp;
        protected int secondsPlayed;

        protected FlowRecord (int memberId, double humanity)
        {
            this.humanity = humanity;
            this.memberId = memberId;
            this.awarded = 0;
        }
    }

    protected int _flowPerMinute = -1; // marker for 'unknown'.

    /** If true, the clock is ticking and participants are earning flow potential. */
    protected boolean _tracking;

    /** The action to use when granting flow. */
    protected UserAction _grantAction;

    /** Counts the total number of seconds that have elapsed during 'tracked' time,
     * for each tracked member that is no longer present with a FlowRecord. */
    protected int _totalTrackedSeconds = 0;

    /** Passed along when granting flow. */
    protected String _detailsPrefix;

    protected HashIntMap<FlowRecord> _flowRecords = new HashIntMap<FlowRecord>();
}
