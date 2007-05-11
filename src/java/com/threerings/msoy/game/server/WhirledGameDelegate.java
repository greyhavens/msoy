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
import com.threerings.parlor.game.server.GameManagerDelegate;

import com.whirled.data.WhirledGameMarshaller;
import com.whirled.data.WhirledGameObject;
import com.whirled.server.WhirledGameDispatcher;
import com.whirled.server.WhirledGameProvider;

import com.threerings.msoy.data.UserAction;
import com.threerings.msoy.server.FlowAwardTracker;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.admin.server.RuntimeConfig;

import static com.threerings.msoy.Log.log;

/**
 * Handles Whirled game services like awarding flow.
 */
public class WhirledGameDelegate extends GameManagerDelegate
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
        if (plobj instanceof WhirledGameObject) {
            _whobj = (WhirledGameObject)plobj;
            _invmarsh = MsoyServer.invmgr.registerDispatcher(new WhirledGameDispatcher(this));
            _whobj.setWhirledGameService((WhirledGameMarshaller)_invmarsh);
        }

        // then load up our anti-abuse factor
        final int gameId = getGameId();
        MsoyServer.invoker.postUnit(new Invoker.Unit() {
            public boolean invoke () {
                try {
                    _antiAbuseFactor =
                        MsoyServer.memberRepo.getFlowRepository().getAntiAbuseFactor(gameId);

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
            MsoyServer.invmgr.clearDispatcher(_invmarsh);
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

        // TODO: This value is currently always zero; see declaration below.
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
        if (_whobj != null) {
            _whobj.setFlowPerMinute(flowPerMinute);
        }
    }

    /** A reference to our game object, casted appropriately or null if the game does not implement
     * {@link WhirledGameObject}. */
    protected WhirledGameObject _whobj;

    /** Keep our invocation service registration so that we can unload it at shutdown. */
    protected InvocationMarshaller _invmarsh;

    /**
     * Cumulative player minutes for this game, reported when the game ends and used by the
     * anti-abuse algorithm.
     * 
     * TODO: Not yet implemented. Should track players entering and leaving.
     */
    protected int _playerMinutes;
}
