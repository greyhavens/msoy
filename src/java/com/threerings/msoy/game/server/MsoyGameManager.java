//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.parlor.game.data.GameConfig;
import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.server.PlaceManagerDelegate;

import com.whirled.game.data.GameContentOwnership;
import com.whirled.game.data.GameData;
import com.whirled.game.data.WhirledPlayerObject;
import com.whirled.game.server.WhirledGameManager;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.ItemPackRepository;
import com.threerings.msoy.item.server.persist.LevelPackRepository;

import com.threerings.msoy.bureau.server.MsoyBureauClient;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.server.persist.TrophyRepository;

import static com.threerings.msoy.Log.log;

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

    // from interface PrizeProvider
    public void awardTrophy (ClientObject caller, String ident, int playerId,
                             InvocationService.InvocationListener listener)
        throws InvocationException
    {
        _trophyDelegate.awardTrophy(caller, ident, playerId, listener);
    }

    // from interface PrizeProvider
    public void awardPrize (ClientObject caller, String ident, int playerId,
                            InvocationService.InvocationListener listener)
        throws InvocationException
    {
        _trophyDelegate.awardPrize(caller, ident, playerId, listener);
    }

    // from interface WhirledGameProvider
    public void endGameWithScores (
        ClientObject caller, int[] playerOids, int[] scores, int payoutType, int gameMode,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
    	_awardDelegate.endGameWithScores(
            caller, playerOids, scores, payoutType, gameMode, listener);
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
     * Returns true if the game is multiplayer, which is true if the game is not a SEATED_GAME
     * (fixed table size, player count established at game start) with exactly one player.
     */
    public boolean isMultiplayer ()
    {
        return (_gameconfig.getMatchType() != GameConfig.SEATED_GAME) || (getPlayerCount() > 1);
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
    public void agentReady (ClientObject caller)
    {
        super.agentReady(caller);

        MsoyBureauClient client = (MsoyBureauClient)_bureauReg.lookupClient(getBureauId());
        if (client == null) {
            log.warning("Agent ready but no bureau client?", "gameMgr", this);
        } else {
            client.agentAdded();
            _agentAdded = true;
        }
    }

    @Override // from WhirledGameManager
    public void agentTrace (ClientObject caller, String[] trace)
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
        StringBuilder sbuf = new StringBuilder();
        sbuf.append("[");
        sbuf.append(cfg.game.name).append(":").append(cfg.getGameId());
        sbuf.append(":").append(_gameObj.getOid());
        StringUtil.toString(sbuf, _gameobj.players);
        sbuf.append("]");
        return sbuf.toString();
    }

    @Override // from PlaceManager
    protected void didShutdown ()
    {
        super.didShutdown();

        if (_agentAdded) {
            MsoyBureauClient client = (MsoyBureauClient)_bureauReg.lookupClient(getBureauId());
            if (client != null) {
                client.agentRemoved();
            }
        }
    }

    @Override // from WhirledGameManager
    protected void resolveContentOwnership (BodyObject body, final ResultListener<Void> listener)
    {
        final PlayerObject plobj = (PlayerObject)body;
        final Game game = ((MsoyGameConfig)getGameConfig()).game;
        if (plobj.isContentResolved(game.gameId) || plobj.isContentResolving(game.gameId)) {
            listener.requestCompleted(null);
            return;
        }

        final GameContentOwnership resolving =  new GameContentOwnership(game.gameId,
            GameData.RESOLUTION_MARKER, WhirledPlayerObject.RESOLVING);
        plobj.addToGameContent(resolving);
        _invoker.postUnit(
            new ContentOwnershipUnit(game.gameId, game.getSuiteId(), plobj.getMemberId()) {
                @Override public void handleSuccess() {
                    if (!plobj.isActive()) {
                        listener.requestCompleted(null);
                        return; // the player has logged off, nevermind
                    }
                    plobj.startTransaction();
                    try {
                        for (GameContentOwnership ownership : _content) {
                            plobj.addToGameContent(ownership);
                        }
                    } finally {
                        plobj.removeFromGameContent(resolving);
                        plobj.addToGameContent(new GameContentOwnership(game.gameId,
                            GameData.RESOLUTION_MARKER, WhirledPlayerObject.RESOLVED));
                        plobj.commitTransaction();
                    }

                    listener.requestCompleted(null);
                }

                @Override protected ItemPackRepository getIpackRepo() {
                    return _ipackRepo;
                }
                @Override protected LevelPackRepository getLpackRepo() {
                    return _lpackRepo;
                }
                @Override protected TrophyRepository getTrophyRepo() {
                    return _trophyRepo;
                }

                @Override protected String getFailureMessage () {
                    return "Failed to resolve content [game=" + _gameconfig.getGameId() +
                        ", who=" + plobj.who() + "].";
                }
            });
    }

    /** A delegate that takes care of awarding flow and ratings. */
    protected AwardDelegate _awardDelegate;

    /** A delegate that takes care of awarding trophies and prizes.. */
    protected TrophyDelegate _trophyDelegate;

    /** A delegate that handles agent traces.. */
    protected AgentTraceDelegate _traceDelegate;

    /** Tracks whether we added an agent to our parent session. */
    protected boolean _agentAdded;

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected ItemPackRepository _ipackRepo;
    @Inject protected LevelPackRepository _lpackRepo;
    @Inject protected TrophyRepository _trophyRepo;
}
