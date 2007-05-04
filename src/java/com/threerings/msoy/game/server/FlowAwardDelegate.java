//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.samskivert.io.PersistenceException;

import com.samskivert.util.Invoker;

import com.threerings.crowd.data.PlaceConfig;

import com.threerings.parlor.game.data.GameConfig;

import com.threerings.parlor.game.server.GameManager;
import com.threerings.parlor.game.server.GameManagerDelegate;


import com.threerings.msoy.data.UserAction;

import com.threerings.msoy.server.FlowAwardTracker;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.admin.server.RuntimeConfig;

import static com.threerings.msoy.Log.log;

/**
 * A GameManagerDelegate for awarding flow.
 */
public class FlowAwardDelegate extends GameManagerDelegate
{
    /** Actually takes care of flow tracking and awarding. */
    public FlowAwardTracker tracker = new FlowAwardTracker();

    /**
     * Construct a FlowAwardDelegate.
     */
    public FlowAwardDelegate (GameManager gmgr)
    {
        super(gmgr);
    }

    @Override
    public void didInit (PlaceConfig config)
    {
        super.didInit(config);

        final int gameId = getGameId();

        // then load up our anti-abuse factor
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _antiAbuseFactor = MsoyServer.memberRepo.getFlowRepository().getAntiAbuseFactor(
                        gameId);

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
                setFlowPerMinute(
                    (int)((RuntimeConfig.server.hourlyGameFlowRate * _antiAbuseFactor) / 60d));
            }

            protected double _antiAbuseFactor;
        });
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

        // TODO: Zell? Something's half-done here.
        if (_playerMinutes != 0) {
            final int gameId = getGameId();
            MsoyServer.invoker.postUnit(new Invoker.Unit() {
                public boolean invoke () {
                    try {
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

    @Override
    public void didShutdown ()
    {
        super.didShutdown();

        tracker.shutdown();
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

    /** TODO? CRUFT? ZELL? */
    protected int _playerMinutes;
}
