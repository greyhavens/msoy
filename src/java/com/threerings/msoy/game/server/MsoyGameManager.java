//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.IntIntMap;
import com.samskivert.util.Invoker;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.ezgame.server.EZGameManager;
import com.threerings.msoy.game.data.MsoyGameObject;
import com.threerings.msoy.server.MsoyServer;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.client.InvocationService.InvocationListener;

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
        validateUser(caller);

        // simply add up what the game tells us; the final payout is limited by the cap
        _flowAwarded.increment(playerId, amount);
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

        _gameStartTime = 0;
    }


    @Override // from PlaceManager
    protected void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);
        if (_gameStartTime > 0) {
            // let any occupant potentially earn flow
            _msoyGameObj.addToFlowRates(new FlowRate(bodyOid, FLOW_PER_MINUTE_PER_PLAYER));
            _flowBeganStamp.put(bodyOid, now());
        }
    }

    @Override // from PlaceManager
    protected void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);
        if (_gameStartTime > 0) {
            grantAwardedFlow(bodyOid);
            _msoyGameObj.removeFromFlowRates(bodyOid);
        }
    }

    @Override
    protected void gameDidStart ()
    {
        super.gameDidStart();
        
        _gameStartTime = (int) (System.currentTimeMillis() / 1000);

        _flowBeganStamp = new IntIntMap();
        _flowAwarded = new IntIntMap();

        _msoyGameObj.flowRates = new DSet<FlowRate>();

        for (int i = 0; i < _plobj.occupants.size(); i ++) {
            int oid = _plobj.occupants.get(i);
            _msoyGameObj.addToFlowRates(new FlowRate(oid, 123));
            _flowBeganStamp.put(oid, now());
        }
    }

    @Override
    protected void gameDidEnd ()
    {
        int[] oidArr = _flowAwarded.getKeys();
        for (int i = 0; i < oidArr[i]; i ++) {
            grantAwardedFlow(oidArr[i]);
        }
        _flowAwarded.clear();

        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
//                try {
//                    int gameId = ((MsoyGameConfig) _config).persistentGameId;
//                    MsoyServer.memberRepo.noteGameEnded(gameId, _playerMinutes);
//                } catch (PersistenceException pe) {
//                    log.log(Level.WARNING, "Failed to note end of game [where=" + where() + "]", pe);
//                }
                return false;
            }
        });
    }

    // possibly cap and then actually grant the flow the game awarded to this player
    protected void grantAwardedFlow (int bodyOid)
    {
        int awarded = _flowAwarded.get(bodyOid);
        if (awarded == 0) {
            return;
        }
        int then = _flowBeganStamp.get(bodyOid);
        if (then == 0) {
            // flow awarded to body who was never here, ignore silently
            return;
        }
        FlowRate rate = _msoyGameObj.flowRates.get(bodyOid);
        if (rate == null) {
            log.warning("No flow rate found [bodyOid=" + bodyOid + "]");
            return;
        }
        int now = now();
        int available = (rate.flowRate * (now - then)) / 60;
        awarded = Math.min(awarded, available);
        // MsoyServer.memberMan.grantFlow(bodyOid, awarded, GrantType.GAME, blah blah);
    }

    protected int now ()
    {
        return (int) (System.currentTimeMillis() / 1000);
    }

    protected MsoyGameObject _msoyGameObj;
    protected int _gameStartTime;
    protected int _playerMinutes;
    protected IntIntMap _flowAwarded;
    protected IntIntMap _flowBeganStamp;
}
