//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.IntMap;
import com.samskivert.util.Invoker;

import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.data.EZGameConfig;
import com.threerings.ezgame.server.EZGameManager;

import com.threerings.msoy.data.ActionType;
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
    public static final int FLOW_PER_MINUTE_PER_PLAYER = 100;

    // from MsoyGameProvider
    public void awardFlow (ClientObject caller, int playerId, int amount,
                           InvocationListener listener)
        throws InvocationException
    {
        // simply add up what the game tells us; the final payout is limited by the cap
        PlayerFlow flowRecord = _players.get(playerId);
        if (flowRecord != null) {
            flowRecord.awarded += amount;
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
    }


    @Override
    protected void gameDidStart ()
    {
        super.gameDidStart();

        _startStamp = now();

        _msoyGameObj.flowRates = new DSet<FlowRate>();

        for (int i = 0; i < _plobj.occupants.size(); i ++) {
            initOccupant(_plobj.occupants.get(i));
        }

        _antiAbuseFactor = -1; // magic number for 'pending'
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _antiAbuseFactor =
                        MsoyServer.memberRepo.getFlowRepository().getAntiAbuseFactor(
                            ((MsoyGameConfig) _config).persistentGameId);
                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to note end of game [where=" + where() + "]", pe);
                }
                return false;
            }
        });
   
    }

    @Override
    protected void gameDidEnd ()
    {
        super.gameDidEnd();

        if (_antiAbuseFactor == -1) {
            // either things are very broken or the game just started, either way, safe to ignore
            return;
        }
        for (IntMap.IntEntry<PlayerFlow> entry : _players.intEntrySet()) {
            grantAwardedFlow(entry.getIntKey(), entry.getValue());
        }
        _players.clear();

        if (_playerMinutes == 0) {
            return;
        }
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
            PlayerFlow flowRecord = _players.get(oid);
            if (flowRecord == null) {
                log.warning("No flow record found [where=" + where() + ", oid=" + oid + "]");
            } else if (_antiAbuseFactor != -1) {
                grantAwardedFlow(oid, flowRecord);
            }
            _players.remove(oid);
            _msoyGameObj.removeFromFlowRates(oid);
        }
    }

    protected void initOccupant (int oid)
    {
        MemberObject member = MsoyServer.lookupMember((MemberName) getOccupantInfo(oid).username);
        if (member == null) {
            log.warning("Failed to lookup member [where=" + where() + ", oid=" + oid + "]");
            return;
        }
        // TODO: we can probably remove this when we're confident with the code
        if (_players.containsKey(oid)) {
            log.warning("Flow record already present [where=" + where() + ", oid=" + oid + "]");
        }
        int rate = (int) (_antiAbuseFactor * member.getHumanity() * FLOW_PER_MINUTE_PER_PLAYER);
        _players.put(oid, new PlayerFlow(rate));
        _msoyGameObj.addToFlowRates(new FlowRate(oid, rate));
    }


    // possibly cap and then actually grant the flow the game awarded to this player
    protected void grantAwardedFlow (int oid, PlayerFlow record)
    {
        int secondsPlayed = now() - record.beganStamp;
        int awarded = Math.min(record.awarded, (record.rate * secondsPlayed) / 60);

        MsoyServer.memberMan.grantFlow(
            (MemberName) getOccupantInfo(oid).username, awarded,
            ActionType.PlayedGame,
            ((EZGameConfig) getConfig()).persistentGameId + " " + secondsPlayed); 
    }

    protected static int now ()
    {
        return (int) (System.currentTimeMillis() / 1000);
    }

    protected static class PlayerFlow
    {
        protected int rate;
        protected int awarded;
        protected int beganStamp;

        protected PlayerFlow (int rate)
        {
            this.rate = rate;
            this.awarded = 0;
            this.beganStamp = now();
        }
    }
    
    protected MsoyGameObject _msoyGameObj;
    protected int _startStamp;
    protected double _antiAbuseFactor;
    protected int _playerMinutes;
    protected IntMap<PlayerFlow> _players = new HashIntMap<PlayerFlow>();
}
