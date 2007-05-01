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

import com.threerings.ezgame.server.EZGameManager;

import com.threerings.msoy.admin.server.RuntimeConfig;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyGameMarshaller;
import com.threerings.msoy.game.data.MsoyGameObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.msoy.data.all.MemberName;

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

        // the final amount of flow this game attempts to pay out is accumulated in-memory and not
        // capped until the game ends or the player leaves
        int previouslyAwarded = record.awarded;
        record.awarded += amount;

        // for immediate flow payouts that don't have to be precise, we try to make our estimate
        // more precise (nobody likes to see their flow actually drop at the end of a game) by
        // taking the cap into account
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
        _msoyGameObj.setMsoyGameService((MsoyGameMarshaller)
            MsoyServer.invmgr.registerDispatcher(new MsoyGameDispatcher(this)));

        // figure out the game's anti-abuse factor
        _antiAbuseFactor = -1; // magic number for 'pending'
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _antiAbuseFactor = MsoyServer.memberRepo.getFlowRepository().getAntiAbuseFactor(
                        _gameconfig.getGameId());
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
                _msoyGameObj.setFlowPerMinute(
                    (int)((RuntimeConfig.server.hourlyGameFlowRate * _antiAbuseFactor) / 60d));
            }
        });
    }

    @Override
    protected void gameDidStart ()
    {
        super.gameDidStart();

        // note the time at which the game started for flow calculations
        int startStamp = now();
        for (FlowRecord record : _flowRecords.values()) {
            record.beganStamp = startStamp;
        }
    }

    @Override
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        // note all remaining player's seconds played
        int endStamp = now();
        for (FlowRecord record : _flowRecords.values()) {
            record.secondsPlayed = endStamp - record.beganStamp;
            record.beganStamp = 0;
        }

        // TODO: Zell? Something's half-done here.
        if (_playerMinutes != 0) {
            MsoyServer.invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    try {
                        int gameId = _gameconfig.getGameId();
                        MsoyServer.memberRepo.noteGameEnded(gameId, _playerMinutes);
                    } catch (PersistenceException pe) {
                        log.log(Level.WARNING, "Failed to note end of game [where=" +
                                where() + "]", pe);
                    }
                    return false;
                }
            });
        }
    }

    @Override // from PlaceManager
    protected void bodyEntered (int oid)
    {
        super.bodyEntered(oid);

        // create a flow record for this occupant
        MemberObject member = (MemberObject) MsoyServer.omgr.getObject(oid);
        if (member == null) { // this should never happen
            log.warning("Failed to lookup member [where=" + where() + ", oid=" + oid + "]");
            return;
        }

        // TODO: we can probably remove this when we're confident with the code
        if (_flowRecords.containsKey(oid)) {
            log.warning("Flow record already present [where=" + where() + ", oid=" + oid + "]");
            return;
        }

        // create a flow record to track awarded flow and remember things about the member we'll
        // need to know when they're gone
        FlowRecord record = new FlowRecord(member);
        _flowRecords.put(oid, record);

        // if the game is already in play, note that they're "starting" immediately
        if (_gameobj.isInPlay()) {
            record.beganStamp = now();
        }
    }

    @Override // from PlaceManager
    protected void bodyLeft (int oid)
    {
        super.bodyLeft(oid);

        // remove their flow record and grant them the flow
        FlowRecord record = _flowRecords.remove(oid);
        if (record == null) {
            log.warning("No flow record found [where=" + where() + ", oid=" + oid + "]");
            return;
        }

        // see if we even care
        if (record.awarded == 0 || record.memberId == MemberName.GUEST_ID) {
            return;
        }

        // see if we're initialized
        if (_antiAbuseFactor == -1) {
            log.warning("Unknown flow rate, but there's a grant. Wha?");
            return;
        }

        // if they're leaving in the middle of a game, update their secondsPlayed
        if (record.beganStamp != 0) {
            record.secondsPlayed = now() - record.beganStamp;
            record.beganStamp = 0;
        }

        // see how much they actually get
        int availFlow = (int) ((record.humanity * _msoyGameObj.flowPerMinute *
                                record.secondsPlayed) / 60);
        int awarded = Math.min(record.awarded, availFlow);

        // award it
        MsoyServer.memberMan.grantFlow(
            record.memberId, awarded, UserAction.PLAYED_GAME,
            _gameconfig.getGameId() + " " + record.secondsPlayed); 
    }

    @Override
    protected void didShutdown ()
    {
        MsoyServer.invmgr.clearDispatcher(_msoyGameObj.msoyGameService);

        super.didShutdown();
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
        protected int secondsPlayed;

        protected FlowRecord (MemberObject memObj)
        {
            this.humanity = memObj.getHumanity();
            this.memberId = memObj.getMemberId();
            this.awarded = 0;
        }
    }

    protected MsoyGameObject _msoyGameObj;

    protected double _antiAbuseFactor;
    protected int _playerMinutes;
    protected IntMap<FlowRecord> _flowRecords = new HashIntMap<FlowRecord>();
}
