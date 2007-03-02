//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.data.EZGameConfig;
import com.threerings.ezgame.server.EZGameManager;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyGameObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.web.data.MemberName;

import static com.threerings.msoy.Log.log;

/**
 * Manages a MetaSOY game.
 */
public class MsoyGameManager extends EZGameManager
    implements MsoyGameProvider
{
    // from MsoyGameProvider
    public void awardFlow (
        ClientObject caller, int amount, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        FlowRecord record = _flowRecords.get(caller.getOid());
        if (record == null) {
            log.warning("Unknown game occupant asking for flow? [caller=" + caller.who() + "].");
            throw new InvocationException(E_INTERNAL_ERROR);
        }

        // the final amount of flow this game attempts to pay out is accumulated in-memory
        // and not capped until the game ends or the player leaves
        int previouslyAwarded = record.awarded;
        record.awarded += amount;

        // for immediate flow payouts that don't have to be precise, we try to make our
        // estimate more precise (nobody likes to see their flow actually drop at the
        // end of a game) by taking the cap into account
        int secondsSoFar = now() - record.beganStamp;
        int flowBudget = (int) ((record.humanity * _msoyGameObj.flowPerMinute * secondsSoFar) / 60);
        int cappedAmount = Math.min(amount, flowBudget - previouslyAwarded);
        if (cappedAmount > 0) {
            MemberObject mObj = (MemberObject) caller;
            mObj.setFlow(mObj.flow + cappedAmount);
        }
    }

    @Override
    protected PlaceObject createPlaceObject ()
    {
        return new MsoyGameObject();
    }
    
    @Override
    protected void didStartup ()
    {
        super.didStartup();

        _msoyGameObj = (MsoyGameObject) _plobj;

        // figure out the game's 
        _antiAbuseFactor = -1; // magic number for 'pending'
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _antiAbuseFactor =
                        MsoyServer.memberRepo.getFlowRepository().getAntiAbuseFactor(
                            ((MsoyGameConfig) _config).persistentGameId);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to fetch game's anti-abuse factor [where=" +
                            where() + "]", pe);
                    // if for some reason our anti-abuse mechanism is on the blink, let's eat
                    // humble pie and treat them all like upstanding citizens
                    _antiAbuseFactor = 1.0f;
                }
                return true; // = call handleResult()
            }

            // here, we're back on the dobj thread
            public void handleResult ()
            {
                _msoyGameObj.setFlowPerMinute((int)
                    ((RuntimeConfig.server.hourlyGameFlowRate * _antiAbuseFactor) / 60d));
            }
        });
    }

    @Override
    protected void gameDidStart ()
    {
        super.gameDidStart();

        _startStamp = now();

        for (int i = 0; i < _plobj.occupants.size(); i ++) {
            initOccupant(_plobj.occupants.get(i));
        }
    }

    @Override
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        // grant all awarded flow
        for (IntMap.IntEntry<FlowRecord> entry : _flowRecords.intEntrySet()) {
            grantAwardedFlow(entry.getIntKey(), entry.getValue());
        }

        // TODO: Zell? Something's half-done here.
        if (_playerMinutes != 0) {
            MsoyServer.invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    try {
                        int gameId = ((MsoyGameConfig) _config).persistentGameId;
                        MsoyServer.memberRepo.noteGameEnded(gameId, _playerMinutes);
                    } catch (PersistenceException pe) {
                        log.log(Level.WARNING, "Failed to note end of game [where=" + where() + "]", pe);
                    }
                    return false;
                }
            });
        }

        // clear everything out for possible game restart,
        // or in case new occupants enter
        _flowRecords.clear();
        _startStamp = 0;
    }

    @Override // from PlaceManager
    protected void bodyEntered (int oid)
    {
        super.bodyEntered(oid);

        if (_startStamp > 0) {
            // let any occupant potentially earn flow
            initOccupant(oid);
        }
    }

    @Override // from PlaceManager
    protected void bodyLeft (int oid)
    {
        super.bodyLeft(oid);

        if (_startStamp > 0) {
            // remove their flow record and grant them the flow
            FlowRecord flowRecord = _flowRecords.remove(oid);
            if (flowRecord != null) {
                grantAwardedFlow(oid, flowRecord);

            } else {
                log.warning("No flow record found [where=" + where() + ", oid=" + oid + "]");
                return;
            }
        }
    }

    protected void initOccupant (int oid)
    {
        MemberObject member = (MemberObject) MsoyServer.omgr.getObject(oid);
        if (member == null) {
            // this should never happen
            log.warning("Failed to lookup member [where=" + where() + ", oid=" + oid + "]");
            return;
        }

        // TODO: we can probably remove this when we're confident with the code
        if (_flowRecords.containsKey(oid)) {
            log.warning("Flow record already present [where=" + where() + ", oid=" + oid + "]");
            return;
        }

        // create a flow record to track awarded flow and remember things
        // about the member we'll need to know when they're gone
        _flowRecords.put(oid, new FlowRecord(member));
    }

    // possibly cap and then actually grant the flow the game awarded to this player
    protected void grantAwardedFlow (int oid, FlowRecord record)
    {
        // see if we even care
        if (record.awarded == 0 || record.memberId == MemberName.GUEST_ID) {
            return;
        }
        // see if we're initialized
        if (_antiAbuseFactor == -1) {
            log.warning("Unknown flow rate, but there's a grant. Wha?");
            return;
        }
        // see how much they actually get
        int secondsPlayed = now() - record.beganStamp;
        int awarded = Math.min(record.awarded,
            (int) ((record.humanity * _msoyGameObj.flowPerMinute * secondsPlayed) / 60));

        // award it
        MsoyServer.memberMan.grantFlow(
            record.memberId, awarded, UserAction.PLAYED_GAME,
            ((EZGameConfig) getConfig()).persistentGameId + " " + secondsPlayed); 
    }

    /**
     * Convenience method to calculate the current timestmap in seconds.
     */
    protected static int now ()
    {
        return (int) (System.currentTimeMillis() / 1000);
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

        protected FlowRecord (MemberObject memObj)
        {
            this.humanity = memObj.getHumanity();
            this.memberId = memObj.getMemberId();
            this.awarded = 0;
            this.beganStamp = now();
        }
    }

    protected MsoyGameObject _msoyGameObj;

    /** The time at which the game started, in seconds, or 0 if the game
     * has not yet started. */
    protected int _startStamp;
    protected double _antiAbuseFactor;
    protected int _playerMinutes;
    protected IntMap<FlowRecord> _flowRecords = new HashIntMap<FlowRecord>();
}
