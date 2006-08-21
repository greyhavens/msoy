//
// $Id$

package com.threerings.msoy.game.server;

import java.util.ArrayList;
import java.util.HashMap;

import com.samskivert.util.ArrayUtil;
import com.samskivert.util.CollectionUtil;
import com.samskivert.util.RandomUtil;

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
import com.threerings.msoy.game.data.PropertySetEvent;

/**
 * A manager for "flash" games in msoy.
 */
public class FlashGameManager extends GameManager
    implements FlashGameProvider, TurnGameManager
{
    public FlashGameManager ()
    {
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

        sendPrivateMessage(playerIdx, msg, data);
    }

    // from FlashGameProvider
    public void addToCollection (
        ClientObject caller, String collName, byte[][] data,
        boolean clearExisting, InvocationService.InvocationListener listener)
        throws InvocationException
    {
        validateUser(caller);
        if (_collections == null) {
            _collections = new HashMap<String, ArrayList<byte[]>>();
        }

        // figure out if we're adding to an existing collection
        // or creating a new one
        ArrayList<byte[]> list = null;
        if (!clearExisting) {
            list = _collections.get(collName);
        }
        if (list == null) {
            list = new ArrayList<byte[]>();
            _collections.put(collName, list);
        }

        CollectionUtil.addAll(list, data);
    }

    // from FlashGameProvider
    public void getFromCollection (
        ClientObject caller, String collName, boolean consume, int count,
        String msgOrPropName, int playerIndex,
        InvocationService.ConfirmListener listener)
        throws InvocationException
    {
        validateUser(caller);

        int srcSize = 0;
        if (_collections != null) {
            ArrayList<byte[]> src = _collections.get(collName);
            srcSize = (src == null) ? 0 : src.size();
            if (srcSize >= count) {
                byte[][] result = new byte[count][];
                for (int ii=0; ii < count; ii++) {
                    int pick = RandomUtil.getInt(srcSize);
                    if (consume) {
                        result[ii] = src.remove(pick);
                        srcSize--;

                    } else {
                        result[ii] = src.get(pick);
                    }
                }

                if (playerIndex >= 0 && playerIndex < _gameObj.players.length) {
                    sendPrivateMessage(playerIndex, msgOrPropName, result);

                } else {
                    setProperty(msgOrPropName, result, -1);
                }
                // SUCCESS!
                listener.requestProcessed();
                return;
            }
        }

        // TODO: decide what we want to return here
        throw new InvocationException(String.valueOf(srcSize));
    }

    // from FlashGameProvider
    public void mergeCollection (
        ClientObject caller, String srcColl, String intoColl,
        InvocationService.InvocationListener listener)
        throws InvocationException
    {
        validateUser(caller);

        if (_collections != null) {
            ArrayList<byte[]> src = _collections.remove(srcColl);
            if (src != null) {
                ArrayList<byte[]> dest = _collections.get(intoColl);
                if (dest == null) {
                    _collections.put(intoColl, src);
                } else {
                    dest.addAll(src);
                }
                return; // success
            }
        }

        // TODO: silently fail? All non-existant collections should be
        // treated as empty?

        // if we reached this point, either there are no collections
        // or there is no collection with that name
        // TODO: there is no translation for this code
        throw new InvocationException("e.no_such_collection");
    }

    /**
     * Helper method to send a private message to the specified player
     * index (must already be verified).
     */
    protected void sendPrivateMessage (
        int playerIdx, String msg, Object data)
        throws InvocationException
    {
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
     * Helper method to post a property set event.
     */
    protected void setProperty (String propName, Object value, int index)
    {
        _gameObj.postEvent(
            new PropertySetEvent(_gameObj.getOid(), propName, value, index));
    }

    /**
     * Validate that the specified user has access to do things in the game.
     */
    protected void validateUser (ClientObject caller)
        throws InvocationException
    {
        if (getPresentPlayerIndex(caller.getOid()) == -1) {
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

    /** The map of collections, lazy-initialized. */
    protected HashMap<String, ArrayList<byte[]>> _collections;
}
