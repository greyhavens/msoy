//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.ezgame.server.EZGameManager;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;
import com.threerings.presents.client.InvocationService.InvocationListener;
import com.threerings.presents.client.InvocationService.ResultListener;

/**
 * Manages a MetaSOY game.
 */
public class MsoyGameManager extends EZGameManager
    implements MsoyGameProvider
{
    public static final int GLOBAL_FPM_BUDGET = 100;

    // from MsoyGameProvider
    public void awardFlow (ClientObject caller, int playerId, int amount,
                           InvocationListener listener)
        throws InvocationException
    {
        validateUser(caller);

        // do no sanity checking here: the cap will take care of all such concerns
        _flowAwarded[getPresentPlayerIndex(playerId)] += amount;
    }

    // from MsoyGameProvider
    public void getAvailableFlow (ClientObject caller, int playerId, ResultListener listener)
        throws InvocationException
    {
        validateUser(caller);

        int pIdx = getPresentPlayerIndex(playerId);
        listener.requestProcessed(flowCap(pIdx, now()) - _flowAwarded[pIdx]);
    }

    @Override
    protected void gameDidStart ()
    {
        super.gameDidStart();
        
        _gameStartTime = (int) (System.currentTimeMillis() / 1000);

        int cnt = getPlayerCount();
        _flowAwarded = new int[cnt];
        _flowRateBudget = new int[cnt];

        int[] humanities = new int[cnt];
        int totalHumanity = 0;
        for (int i = 0; i < cnt; i ++) {
            humanities[i] = (int) (Math.random() * 0x100); // TODO, duh
            totalHumanity += humanities[i];
        }

        for (int i = 0; i < _flowRateBudget.length; i ++) {
            _flowRateBudget[i] = (GLOBAL_FPM_BUDGET * humanities[i]) / totalHumanity;
        }
    }

    @Override
    protected void playerGameDidEnd (int pidx)
    {
        grantAwardedFlow(pidx, now());
    }

    @Override
    protected void gameDidEnd ()
    {
        super.gameDidEnd();
        for (int i = 0; i < _gameObj.getPlayerCount(); i ++) {
            if (isActivePlayer(i)) {
                grantAwardedFlow(i, now());
            }
        }
    }

    // calculate the flow grant cap for this player at the given 'now' time
    protected int flowCap (int pidx, int now)
    {
        int secondsPlayed = now - _gameStartTime; 
        return (_flowRateBudget[pidx] * secondsPlayed) / 60; 
    }

    // possibly cap and then actually grant the flow the game awarded to this player
    protected void grantAwardedFlow (int pidx, int now)
    {
        int flow = Math.min(_flowAwarded[pidx], flowCap(pidx, now));
        // MsoyServer.memberMan.grantFlow(getPlayerName(pidx), flow, GrantType.GAME, blah blah);
    }

    protected int now ()
    {
        return (int) (System.currentTimeMillis() / 1000);
    }
    
    protected int _gameStartTime;
    protected int[] _flowAwarded;
    protected int[] _flowRateBudget;
}
