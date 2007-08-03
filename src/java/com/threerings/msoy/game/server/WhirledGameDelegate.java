//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;
import com.samskivert.util.Invoker;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationMarshaller;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.data.GameConfig;
import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.rating.server.RatingManagerDelegate;
import com.threerings.parlor.rating.server.persist.RatingRepository;
import com.threerings.util.Name;

import com.whirled.data.WhirledGame;
import com.whirled.data.WhirledGameMarshaller;
import com.whirled.server.WhirledGameDispatcher;
import com.whirled.server.WhirledGameProvider;

import com.threerings.msoy.data.UserAction;

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

    // from WhirledGameProvider
    public void awardFlow (ClientObject caller, int amount,
                           InvocationService.InvocationListener listener)
        throws InvocationException
    {
        tracker.awardFlow(caller.getOid(), amount);
    }

    @Override
    public void didStartup (PlaceObject plobj)
    {
        super.didStartup(plobj);

        // wire up our WhirledGameService
        if (plobj instanceof WhirledGame) {
            _wgame = (WhirledGame)plobj;
            _invmarsh = MsoyGameServer.invmgr.registerDispatcher(new WhirledGameDispatcher(this));
            _wgame.setWhirledGameService((WhirledGameMarshaller)_invmarsh);
        }

        // then load up our anti-abuse factor
        final int gameId = getGameId();
        MsoyGameServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _antiAbuseFactor =
                        MsoyGameServer.memberRepo.getFlowRepository().getAntiAbuseFactor(gameId);

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
            MsoyGameServer.invmgr.clearDispatcher(_invmarsh);
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
            MsoyGameServer.invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    try {
                        MsoyGameServer.memberRepo.noteGameEnded(gameId, playerMins);
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
        return MsoyGameServer.ratingRepo;
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
        if (_wgame != null) {
            _wgame.setFlowPerMinute(flowPerMinute);
        }
    }

    /** A reference to our game object, casted appropriately or null if the game does not implement
     * {@link WhirledGame}. */
    protected WhirledGame _wgame;

    /** Keep our invocation service registration so that we can unload it at shutdown. */
    protected InvocationMarshaller _invmarsh;
}
