package com.threerings.msoy.game.client {

import flash.events.Event;

import flash.utils.ByteArray;

import com.threerings.util.FlashObjectMarshaller;
import com.threerings.util.Name;

import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.parlor.game.client.GameController;

import com.threerings.parlor.turn.client.TurnGameController;
import com.threerings.parlor.turn.client.TurnGameControllerDelegate;

import com.threerings.msoy.msoy_internal;

import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.game.data.FlashGameObject;
import com.threerings.msoy.game.data.PropertySetEvent;
import com.threerings.msoy.game.data.PropertySetListener;

import com.metasoy.game.MessageReceivedEvent;
import com.metasoy.game.PropertyChangedEvent;
import com.metasoy.game.StateChangedEvent;

/**
 * A controller for flash games.
 */
public class FlashGameController extends GameController
    implements TurnGameController, PropertySetListener, MessageListener
{
    /** The implementation of the GameObject interface for users. */
    public var userGameObj :UserGameObject;

    public function FlashGameController ()
    {
        addDelegate(_turnDelegate = new TurnGameControllerDelegate(this));
    }

    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        _fgObj = (plobj as FlashGameObject);
        userGameObj = new UserGameObject(_ctx as MsoyContext, _fgObj);
        super.willEnterPlace(plobj);
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);
        _fgObj = null;
    }

    // from TurnGameController
    public function turnDidChange (turnHolder :Name) :void
    {
        dispatchUserEvent(
            new StateChangedEvent(StateChangedEvent.TURN_CHANGED));
    }

    // from PropertySetListener
    public function propertyWasSet (event :PropertySetEvent) :void
    {
        // notify the user game
        dispatchUserEvent(new PropertyChangedEvent(
            event.getName(), event.getValue(), event.getOldValue(),
            event.getIndex()));
    }

    // from MessageListener
    public function messageReceived (event :MessageEvent) :void
    {
        if (FlashGameObject.USER_MESSAGE == event.getName()) {
            var args :Array = event.getArgs();
            dispatchUserEvent(new MessageReceivedEvent(
                (args[0] as String),
                FlashObjectMarshaller.decode((args[1] as ByteArray))));
        }
    }

    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        return new FlashGameView(ctx as MsoyContext, this);
    }

    override protected function gameDidStart () :void
    {
        super.gameDidStart();
        dispatchUserEvent(
            new StateChangedEvent(StateChangedEvent.GAME_STARTED));
    }

    override protected function gameDidEnd () :void
    {
        super.gameDidEnd();
        dispatchUserEvent(
            new StateChangedEvent(StateChangedEvent.GAME_ENDED));
    }

    protected function dispatchUserEvent (event :Event) :void
    {
        userGameObj.msoy_internal::dispatch(event);
    }

    protected var _fgObj :FlashGameObject;

    protected var _turnDelegate :TurnGameControllerDelegate;
}
}
