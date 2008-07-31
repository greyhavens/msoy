//
// $Id$

package com.threerings.msoy.game.server;

import com.samskivert.util.StringUtil;

import com.threerings.parlor.game.data.GameConfig;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.server.PlaceManagerDelegate;

import com.whirled.game.server.WhirledGameManager;

import com.threerings.msoy.game.data.MsoyGameConfig;

/**
 * Manages a MetaSOY game.
 */
@EventThread
public class MsoyGameManager extends WhirledGameManager
{
    public MsoyGameManager ()
    {
        super();
    }

    // from interface WhirledGameProvider
    public void awardTrophy (ClientObject caller, String ident, int playerId,
                             InvocationService.InvocationListener listener)
        throws InvocationException
    {
        _trophyDelegate.awardTrophy(caller, ident, playerId, listener);
    }

    // from interface WhirledGameProvider
    public void awardPrize (ClientObject caller, String ident, int playerId,
                            InvocationService.InvocationListener listener)
        throws InvocationException
    {
        _trophyDelegate.awardPrize(caller, ident, playerId, listener);
    }

    // from interface WhirledGameProvider
    public void endGameWithScores (
        ClientObject caller, int[] playerOids, int[] scores, int payoutType,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
    	_awardDelegate.endGameWithScores(caller, playerOids, scores, payoutType, listener);
    }

    // from interface WhirledGameProvider
    public void endGameWithWinners (
        ClientObject caller, int[] winnerOids, int[] loserOids, int payoutType,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        _awardDelegate.endGameWithWinners(caller, winnerOids, loserOids, payoutType, listener);
    }

    /**
     * Returns true if the game is multiplayer, which happens when:
     * <ul>
     *   <li> The game is a party game, or
     *   <li> The table contains more than one player slot
     * </ul>
     */
    public boolean isMultiplayer ()
    {
        return (_gameconfig.getMatchType() == GameConfig.PARTY)
            // NOTE: originally the isMultiPlayer check in the delegate returned true
            // for seated continuous, but if there's only 1 slot.. ?
            || (_gameconfig.players.length > 1);
    }

    @Override
    public void addDelegate (PlaceManagerDelegate delegate)
    {
        super.addDelegate(delegate);

        if (delegate instanceof AwardDelegate) {
            _awardDelegate = (AwardDelegate) delegate;
        } else if (delegate instanceof AgentTraceDelegate) {
            _traceDelegate = (AgentTraceDelegate) delegate;
        } else if (delegate instanceof TrophyDelegate) {
        	_trophyDelegate = (TrophyDelegate) delegate;
        }
    }

    @Override // from WhirledGameManager
    public void agentTrace (ClientObject caller, String trace)
    {
        super.agentTrace(caller, trace);
        _traceDelegate.recordAgentTrace(trace);
    }

    @Override // from PlaceManager
    public String where ()
    {
        if (_config == null || _plobj == null) {
            return super.where();
        }
        MsoyGameConfig cfg = (MsoyGameConfig)_config;
        return "[" + cfg.name + ":" + cfg.getGameId() + ":" + _gameobj.getOid() +
            "(" + StringUtil.toString(_gameobj.players) + ")";
    }

    @Override // from GameManager
    protected long getNoShowTime ()
    {
        // because in Whirled we start the game before the client begins downloading the game
        // media, we have to be much more lenient about noshow timing (or revamp a whole bunch of
        // other shit which maybe we'll do later)
        return 1000L * ((getPlayerSlots() == 1) ? 180 : 90);
    }

    /** A delegate that takes care of awarding flow and ratings. */
    protected AwardDelegate _awardDelegate;

    /** A delegate that takes care of awarding trophies and prizes.. */
    protected TrophyDelegate _trophyDelegate;

    /** A delegate that handles agent traces.. */
    protected AgentTraceDelegate _traceDelegate;
}
