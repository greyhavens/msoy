//
// $Id$

package com.threerings.msoy.game.server;

import java.util.logging.Level;

import com.threerings.presents.client.InvocationService;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.PlaceObject;

import com.threerings.ezgame.server.EZGameManager;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.game.data.MsoyGameConfig;
import com.threerings.msoy.game.data.MsoyGameMarshaller;
import com.threerings.msoy.game.data.MsoyGameObject;
import com.threerings.msoy.server.MsoyServer;

import static com.threerings.msoy.Log.log;

/**
 * Manages a MetaSOY game.
 */
public class MsoyGameManager extends EZGameManager
    implements MsoyGameProvider
{
    public MsoyGameManager ()
    {
        super();

        // we make a few modifications to the standard delegate
        // so that we can publish flow rates in the game object.
        _flowDelegate = new FlowAwardDelegate(this) {
            protected void setFlowPerMinute (int flowPerMinute) {
                super.setFlowPerMinute(flowPerMinute);
                checkFlowPerMinute();
            }
        };
        addDelegate(_flowDelegate);
    }

    // from MsoyGameProvider
    public void awardFlow (
        ClientObject caller, int amount, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        _flowDelegate.tracker.awardFlow(caller.getOid(), amount);
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
        _msoyGameObj.setMsoyGameService((MsoyGameMarshaller)
            MsoyServer.invmgr.registerDispatcher(new MsoyGameDispatcher(this)));

        checkFlowPerMinute();
    }

    @Override
    protected void didShutdown ()
    {
        MsoyServer.invmgr.clearDispatcher(_msoyGameObj.msoyGameService);

        super.didShutdown();
    }

    /**
     * Check to see if we have both our game object and we know the flow per minute.
     * When we do, publish the flow per minute value in the gameobject.
     */
    protected void checkFlowPerMinute ()
    {
        int fpm = _flowDelegate.tracker.getFlowPerMinute();
        if (_msoyGameObj != null && fpm != -1) {
            _msoyGameObj.setFlowPerMinute(fpm);
        }
    }

    protected MsoyGameObject _msoyGameObj;

    protected FlowAwardDelegate _flowDelegate;
}
