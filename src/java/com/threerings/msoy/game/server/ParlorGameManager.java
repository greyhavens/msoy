//
// $Id$

package com.threerings.msoy.game.server;

import com.google.inject.Inject;

import com.samskivert.util.Invoker;
import com.samskivert.util.ResultListener;
import com.samskivert.util.StringUtil;

import com.threerings.presents.annotation.EventThread;
import com.threerings.presents.annotation.MainInvoker;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.server.PlaceManagerDelegate;

import com.threerings.parlor.game.data.GameConfig;

import com.whirled.bureau.data.GameAgentObject;
import com.whirled.game.data.GameContentOwnership;
import com.whirled.game.data.GameData;
import com.whirled.game.data.WhirledPlayerObject;
import com.whirled.game.server.WhirledGameManager;

import com.threerings.msoy.data.MsoyUserObject;

import com.threerings.msoy.item.data.all.Game;
import com.threerings.msoy.item.server.persist.ItemPackRepository;
import com.threerings.msoy.item.server.persist.LevelPackRepository;

import com.threerings.msoy.bureau.server.MsoyBureauClient;
import com.threerings.msoy.game.data.GameSummary;
import com.threerings.msoy.game.data.ParlorGameConfig;
import com.threerings.msoy.game.data.ParlorGameObject;
import com.threerings.msoy.game.data.PlayerObject;
import com.threerings.msoy.game.server.persist.TrophyRepository;

import com.threerings.msoy.party.server.PartyRegistry;

import static com.threerings.msoy.Log.log;

/**
 * Manages a MetaSOY game.
 */
@EventThread
public class ParlorGameManager extends WhirledGameManager
{
    public ParlorGameManager ()
    {
        super();
    }

    // from interface ContentProvider
    public void consumeItemPack (ClientObject caller, String ident,
                                 InvocationService.InvocationListener listener)
        throws InvocationException
    {
        _contentDelegate.consumeItemPack(caller, ident, listener);
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
        } else if (delegate instanceof ContentDelegate) {
            _contentDelegate = (ContentDelegate) delegate;
        }
    }

    @Override
    public void bodyWillEnter (BodyObject body)
    {
        if (body instanceof MsoyUserObject) {
            _partyReg.userEnteringPlace((MsoyUserObject)body, _gameObj);
        }

        // Note: for entering parties, we want to add the PartySummary first, then the occInfo

        super.bodyWillEnter(body);
    }

    @Override
    public void bodyWillLeave (BodyObject body)
    {
        super.bodyWillLeave(body);

        // Note: for leaving parties, we want to remove the occInfo first, then the PartySummary

        if (body instanceof MsoyUserObject) {
            _partyReg.userLeavingPlace((MsoyUserObject)body, _gameObj);
        }
    }

    @Override // from WhirledGameManager
    public void agentReady (ClientObject caller)
    {
        super.agentReady(caller);

        MsoyBureauClient client = (MsoyBureauClient)_bureauReg.lookupClient(getBureauId());
        if (client == null) {
            log.warning("Agent ready but no bureau client?", "game", where());
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
        StringBuilder buf = new StringBuilder();
        buf.append(getGameId());
        if (_gameObj != null) {
            buf.append(", oid=").append(_gameObj.getOid());
        }
        return buf.toString();
    }

    @Override // from PlaceManager
    protected PlaceObject createPlaceObject ()
    {
        return new ParlorGameObject();
    }

    @Override // from PlaceManager
    protected void didStartup ()
    {
        super.didStartup();

        _gameObj = (ParlorGameObject) _plobj;
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
    protected void bodyEntered (final int bodyOid)
    {
        super.bodyEntered(bodyOid);

        // note that this player is playing this game (note that we don't have to clear the player
        // game because that is done automatically when the player logs off of the game server)
        PlayerObject plobj = (PlayerObject)_omgr.getObject(bodyOid);
        if (plobj != null) {
            _playerActions.updatePlayerGame(plobj, new GameSummary(getGame()));
        }
    }

    @Override // from WhirledGameManager
    protected GameAgentObject createAgent ()
    {
        // TEMP: hack to prevent agent creation if we're a single player game and this game is
        // configured to use its agent only for multiplayer games
        return (getGame().isAgentMPOnly() && !isMultiplayer()) ? null : super.createAgent();
    }

    @Override // from WhirledGameManager
    protected void resolveContentOwnership (BodyObject body, final ResultListener<Void> listener)
    {
        final PlayerObject plobj = (PlayerObject)body;
        final Game game = getGame();
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
                    plobj.addToGameContent(
                        new GameContentOwnership(game.gameId, GameData.RESOLUTION_MARKER,
                                                 WhirledPlayerObject.RESOLVED));
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
                return "Failed to resolve content [game=" + where() + ", who=" + plobj.who() + "].";
            }
        });
    }

    protected Game getGame ()
    {
        return ((ParlorGameConfig)getGameConfig()).game;
    }

    /** A casted reference to the GameObject. */
    protected ParlorGameObject _gameObj;

    /** A delegate that takes care of awarding flow and ratings. */
    protected AwardDelegate _awardDelegate;

    /** A delegate that takes care of awarding trophies and prizes. */
    protected TrophyDelegate _trophyDelegate;

    /** A delegate that takes care of content services. */
    protected ContentDelegate _contentDelegate;

    /** A delegate that handles agent traces.. */
    protected AgentTraceDelegate _traceDelegate;

    /** Tracks whether we added an agent to our parent session. */
    protected boolean _agentAdded;

    @Inject protected @MainInvoker Invoker _invoker;
    @Inject protected ItemPackRepository _ipackRepo;
    @Inject protected LevelPackRepository _lpackRepo;
    @Inject protected PartyRegistry _partyReg;
    @Inject protected TrophyRepository _trophyRepo;
    @Inject protected PlayerNodeActions _playerActions;
}
