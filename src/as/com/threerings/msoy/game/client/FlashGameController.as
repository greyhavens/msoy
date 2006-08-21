package com.threerings.msoy.game.client {

import flash.events.Event;

import flash.utils.ByteArray;

import com.threerings.util.FlashObjectMarshaller;
import com.threerings.util.Name;

import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageListener;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
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

    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        _mctx = (ctx as MsoyContext);
        super.init(ctx, config);
    }

    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        _fgObj = (plobj as FlashGameObject);
        userGameObj = new UserGameObject(_mctx, _fgObj);

        _mctx.getClientObject().addListener(_userListener);

        super.willEnterPlace(plobj);
    }

    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        super.didLeavePlace(plobj);

        _mctx.getClientObject().removeListener(_userListener);

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
        var name :String = event.getName();
        if (FlashGameObject.USER_MESSAGE == name) {
            dispatchUserMessage(event.getArgs());

        } else if (FlashGameObject.GAME_CHAT == name) {
            // this is chat send by the game, let's route it like
            // localChat, which is also sent by the game
            userGameObj.localChat(String(event.getArgs()[0]));
        }
    }

    /**
     * Called by our user listener when we receive a message event
     * on the user object.
     */
    protected function messageReceivedOnUserObject (event :MessageEvent) :void
    {
        // see if it's a message about user games
        var msgName :String =
            FlashGameObject.USER_MESSAGE + ":" + _fgObj.getOid();
        if (msgName == event.getName()) {
            dispatchUserMessage(event.getArgs());
        }
    }

    /**
     * Dispatch the user message.
     */
    protected function dispatchUserMessage (args :Array) :void
    {
        dispatchUserEvent(new MessageReceivedEvent(
            (args[0] as String),
            FlashObjectMarshaller.decode(args[1])));
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

    /** A casted reference to our context. */
    protected var _mctx :MsoyContext;

    protected var _fgObj :FlashGameObject;

    protected var _turnDelegate :TurnGameControllerDelegate;

    protected var _userListener :MessageAdapter =
        new MessageAdapter(messageReceivedOnUserObject);
}
}
