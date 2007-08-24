//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.TreeMap;
import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.ArrayIntSet;
import com.samskivert.util.HashIntMap;
import com.samskivert.util.Invoker;
import com.samskivert.util.StringUtil;
import com.threerings.util.Name;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.dobj.DObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.data.GameCodes;
import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.rating.server.RatingManagerDelegate;
import com.threerings.parlor.rating.server.persist.RatingRepository;

import com.threerings.ezgame.server.EZGameManager;

import com.whirled.data.WhirledGame;
import com.whirled.data.WhirledGameMarshaller;
import com.whirled.server.WhirledGameDispatcher;
import com.whirled.server.WhirledGameProvider;

import com.threerings.msoy.data.MsoyUserObject;
import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.server.MsoyBaseServer;

import com.threerings.msoy.admin.server.RuntimeConfig;

import static com.threerings.msoy.Log.log;

/**
 * Handles Whirled game services like awarding flow.
 */
public class WhirledGameDelegate extends RatingManagerDelegate
    implements WhirledGameProvider
{
    /** Actually takes care of flow tracking and awarding. */
    public FlowAwardTracker tracker = new FlowAwardTracker();

    public WhirledGameDelegate (GameManager gmgr)
    {
        super(gmgr);
    }

    // from interface WhirledGameProvider
    public void endGameWithScores (ClientObject caller, int[] playerOids, int[] scores,
                                   int payoutType, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        verifyIsPlayer(caller);
        if (!_gobj.isInPlay()) {
            throw new InvocationException("e.game_already_ended");
        }

        // convert the players into record indexed on player oid which will weed out duplicates and
        // avoid funny business
        HashIntMap<Player> players = new HashIntMap<Player>();
        for (int ii = 0; ii < playerOids.length; ii++) {
            int availFlow = tracker.getAwardableFlow(playerOids[ii]);
            players.put(playerOids[ii], new Player(playerOids[ii], scores[ii], availFlow));
        }

        // TODO: record scores, convert scores to percentiles
        for (Player player : players.values()) {
            player.percentile = 69; // TEMP
            // scale each players' flow award by their percentile performance
            player.availFlow = (int)Math.ceil(player.availFlow * (player.percentile / 99f));
        }

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // TODO: update ratings

        // now actually end the game
        _gmgr.endGame();
    }

    // from interface WhirledGameProvider
    public void endGameWithWinners (ClientObject caller, int[] winnerOids, int[] loserOids,
                                    int payoutType, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        verifyIsPlayer(caller);
        if (!_gobj.isInPlay()) {
            throw new InvocationException("e.game_already_ended");
        }

        // convert the players into records indexed on player oid to weed out duplicates and avoid
        // any funny business
        HashIntMap<Player> players = new HashIntMap<Player>();
        for (int ii = 0; ii < winnerOids.length; ii++) {
            Player player = new Player(winnerOids[ii], 1, tracker.getAwardableFlow(winnerOids[ii]));
            player.percentile = 74; // winners are 75th percentile
            players.put(winnerOids[ii], player);
        }
        for (int ii = 0; ii < loserOids.length; ii++) {
            Player player = new Player(loserOids[ii], 0, tracker.getAwardableFlow(loserOids[ii]));
            player.percentile = 24; // losers are 25th percentile
            players.put(loserOids[ii], player);
        }

        // award flow according to the rankings and the payout type
        awardFlow(players, payoutType);

        // tell the game manager about our winners which will be used to compute ratings, etc.
        if (_gmgr instanceof EZGameManager) {
            ArrayIntSet winners = new ArrayIntSet();
            for (Player player : players.values()) {
                if (player.score == 1) {
                    winners.add(player.playerOid);
                }
            }
            ((EZGameManager)_gmgr).setWinners(winners.toIntArray());

        } else {
            log.warning("Unable to configure EZGameManager with winners [where=" + where() +
                        ", isa=" + _gmgr.getClass().getName() + "].");
        }

        // now actually end the game
        _gmgr.endGame();
    }

    protected void awardFlow (HashIntMap<Player> players, int payoutType)
    {
        // figure out who ranked where
        TreeMap<Integer,ArrayList<Player>> rankings = new TreeMap<Integer,ArrayList<Player>>();
        for (Player player : players.values()) {
            ArrayList<Player> list = rankings.get(player.score);
            if (list == null) {
                list = new ArrayList<Player>();
            }
            list.add(player);
        }

        switch (payoutType) {
        case WINNERS_TAKE_ALL: // TODO
//            break;

        case CASCADING_PAYOUT: // TODO
//            break;

        case TO_EACH_THEIR_OWN:
            for (Player player : players.values()) {
                player.flowAward = player.availFlow;
            }
            break;
        }

        log.info("Awarding flow [game=" + where() + ", to=" + players + "].");

        // actually award flow and report it to the player
        for (Player player : players.values()) {
            tracker.awardFlow(player.playerOid, player.flowAward);
            DObject user = MsoyBaseServer.omgr.getObject(player.playerOid);
            if (user != null) {
                user.postMessage(WhirledGame.FLOW_AWARDED_MESSAGE,
                                 player.flowAward, player.percentile);
            }
        }
    }

    @Override
    public void didStartup (PlaceObject plobj)
    {
        super.didStartup(plobj);

        // wire up our WhirledGameService
        if (plobj instanceof WhirledGame) {
            _invmarsh = MsoyBaseServer.invmgr.registerDispatcher(new WhirledGameDispatcher(this));
            ((WhirledGame)plobj).setWhirledGameService((WhirledGameMarshaller)_invmarsh);
        }

        // then load up our anti-abuse factor
        final int gameId = getGameId();
        MsoyBaseServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _antiAbuseFactor =
                        MsoyBaseServer.memberRepo.getFlowRepository().getAntiAbuseFactor(gameId);

                } catch (PersistenceException pe) {
                    log.log(Level.WARNING, "Failed to fetch game's anti-abuse factor [where=" +
                            where() + "]", pe);
                    // if for some reason our anti-abuse mechanism is on the blink, assume the
                    // game is innocent until proven guilty
                    _antiAbuseFactor = 1.0f;
                }
                return true; // = call handleResult()
            }

            // here, we're back on the dobj thread
            public void handleResult () {
                setFlowPerMinute(
                    (int)((RuntimeConfig.server.hourlyGameFlowRate * _antiAbuseFactor) / 60d));
            }

            protected double _antiAbuseFactor;
        });
    }

    @Override
    public void didShutdown ()
    {
        super.didShutdown();
        if (_invmarsh != null) {
            MsoyBaseServer.invmgr.clearDispatcher(_invmarsh);
        }
        tracker.shutdown();
    }

    @Override
    public void bodyEntered (int bodyOid)
    {
        super.bodyEntered(bodyOid);
        tracker.addMember(bodyOid);
    }

    @Override
    public void bodyLeft (int bodyOid)
    {
        super.bodyLeft(bodyOid);
        tracker.removeMember(bodyOid);
    }

    @Override
    public void gameDidStart ()
    {
        super.gameDidStart();

        tracker.startTracking();
    }

    @Override
    public void gameDidEnd ()
    {
        super.gameDidEnd();

        tracker.stopTracking();

        int totalSeconds = tracker.getTotalTrackedSeconds();
        int totalMinutes = Math.round(totalSeconds / 60f);
        if (totalMinutes == 0 && totalSeconds > 0) {
            totalMinutes = 1; // round very short games up to 1 minute.
        }
        if (totalMinutes > 0) {
            final int playerMins = totalMinutes;
            final int gameId = getGameId();
            MsoyBaseServer.invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    try {
                        MsoyBaseServer.memberRepo.noteGameEnded(gameId, playerMins);
                    } catch (PersistenceException pe) {
                        log.log(Level.WARNING,
                            "Failed to note end of game [where=" + where() + "]", pe);
                    }
                    return false;
                }
            });
        }
    }

    @Override
    protected int minimumRatedDuration ()
    {
        // don't rate games that last less than 10 seconds
        return 10;
    }
    
    @Override
    protected RatingRepository getRatingRepository ()
    {
        return MsoyBaseServer.ratingRepo;
    }

    @Override
    protected void updateRatingInMemory (int gameId, Name playerName, Rating rating)
    {
        // we don't keep in-memory ratings for whirled
    }

    /**
     * Convenience method to get our game Id.
     */
    protected int getGameId ()
    {
        return ((GameConfig) _plmgr.getConfig()).getGameId();
    }

    /**
     * Called when the anti-abuse factor is known.
     *
     * @return the maximum FlowPerMinute for this game.
     */
    protected void setFlowPerMinute (int flowPerMinute)
    {
        tracker.init(flowPerMinute, UserAction.PLAYED_GAME, String.valueOf(getGameId()));
    }

    /**
     * Checks that the caller in question is a player if the game is not a party game.
     */
    protected void verifyIsPlayer (ClientObject caller)
        throws InvocationException
    {
        MsoyUserObject user = (MsoyUserObject)caller;
        if (_gobj.players.length > 0) {
            if (_gobj.getPlayerIndex(user.getMemberName()) == -1) {
                throw new InvocationException(GameCodes.E_ACCESS_DENIED);
            }
        }
    }

    protected static class Player
    {
        public int playerOid;
        public int score;
        public int availFlow;

        public int percentile;
        public int flowAward;

        public Player (int playerOid, int score, int availFlow) {
            this.playerOid = playerOid;
            this.score = score;
            this.availFlow = availFlow;
        }

        public String toString () {
            return StringUtil.fieldsToString(this);
        }
    }

    /** Keep our invocation service registration so that we can unload it at shutdown. */
    protected InvocationMarshaller _invmarsh;

    /** From WhirledGameControl.as. */
    protected static final int CASCADING_PAYOUT = 0;

    /** From WhirledGameControl.as. */
    protected static final int WINNERS_TAKE_ALL = 1;

    /** From WhirledGameControl.as. */
    protected static final int TO_EACH_THEIR_OWN = 2;
}
