//
// $Id$

package com.threerings.msoy.game.server;

import com.threerings.util.Name;

import com.threerings.presents.data.ClientObject;
import com.threerings.presents.data.InvocationCodes;
import com.threerings.presents.dobj.AccessController;
import com.threerings.presents.client.InvocationService;
import com.threerings.presents.server.InvocationException;

import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.parlor.game.server.GameManager;

import com.threerings.parlor.turn.server.TurnGameManager;

import com.threerings.msoy.server.MsoyObjectAccess;
import com.threerings.msoy.server.MsoyServer;

import com.threerings.msoy.game.data.FlashGameObject;
import com.threerings.msoy.game.data.FlashGameMarshaller;

/**
 * A manager for "flash" games in msoy.
 */
public class FlashGameManager extends GameManager
    implements FlashGameProvider, TurnGameManager
{
    public FlashGameManager ()
    {
        // TODO: create a full-fledged class to do this, so that
        // it's clearer what's going on with the _nextPlayerIndex
        addDelegate(_turnDelegate = new FlashGameTurnDelegate(this));
    }

    // from TurnGameManager
    public void turnWillStart ()
    {
    }

    // from TurnGameManager
    public void turnDidStart ()
    {
    }

    // from TurnGameManager
    public void turnDidEnd ()
    {
    }

    // from FlashGameProvider
    public void endTurn (
        ClientObject caller, int nextPlayerIndex,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        validateStateModification(caller);
        _turnDelegate.endTurn(nextPlayerIndex);
    }

    // from FlashGameProvider
    public void endGame (
        ClientObject caller, int[] winners,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        validateStateModification(caller);

        _winnerIndexes = winners;
        endGame();
    }

    // from FlashGameProvider
    public void sendMessage (
        ClientObject caller, int playerIdx, String msg, byte[] data,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        validateUser(caller);

        if (playerIdx < 0 || playerIdx >= _gameObj.players.length) {
            // TODO: this code has no corresponding translation
            throw new InvocationException("m.invalid_player_index");
        }

        BodyObject target = getPlayer(playerIdx);
        if (target == null) {
            // TODO: this code has no corresponding translation
            throw new InvocationException("m.player_not_around");
        }

        target.postMessage(
            FlashGameObject.USER_MESSAGE + ":" + _gameObj.getOid(),
            new Object[] { msg, data });
    }

    /**
     * Validate that the specified user has access to do things in the game.
     */
    protected void validateUser (ClientObject caller)
        throws InvocationException
    {
        if (!_gameObj.occupants.contains(caller.getOid())) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
    }

    /**
     * Validate that the specified listener has access to make a
     * change.
     */
    protected void validateStateModification (ClientObject caller)
        throws InvocationException
    {
        validateUser(caller);

        Name holder = _gameObj.turnHolder;
        if (holder != null &&
                !holder.equals(((BodyObject) caller).getVisibleName())) {
            throw new InvocationException(InvocationCodes.ACCESS_DENIED);
        }
    }

    @Override
    protected Class<? extends PlaceObject> getPlaceObjectClass ()
    {
        return FlashGameObject.class;
    }

    @Override
    protected AccessController getAccessController ()
    {
        return MsoyObjectAccess.GAME;
    }

    @Override
    protected void didStartup ()
    {
        super.didStartup();

        _gameObj = (FlashGameObject) _plobj;

        _gameObj.setFlashGameService(
            (FlashGameMarshaller) MsoyServer.invmgr.registerDispatcher(
            new FlashGameDispatcher(this), false));
    }

    @Override
    protected void didShutdown ()
    {
        MsoyServer.invmgr.clearDispatcher(_gameObj.flashGameService);

        super.didShutdown();
    }

    @Override
    protected void assignWinners (boolean[] winners)
    {
        if (_winnerIndexes != null) {
            for (int index : _winnerIndexes) {
                if (index >= 0 && index < winners.length) {
                    winners[index] = true;
                }
            }
            _winnerIndexes = null;
        }
    }

    /** A nice casted reference to the game object. */
    protected FlashGameObject _gameObj;

    /** Our turn delegate. */
    protected FlashGameTurnDelegate _turnDelegate;

    /** The array of winners, after the user has filled it in. */
    protected int[] _winnerIndexes;
}
