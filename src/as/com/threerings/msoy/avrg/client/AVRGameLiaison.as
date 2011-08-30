//
// $Id$

package com.threerings.msoy.avrg.client {

import com.threerings.util.Log;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.dobj.AttributeChangeAdapter;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.client.PlaceController;
import com.threerings.crowd.data.BodyObject;

import com.threerings.msoy.avrg.client.AVRService_AVRGameJoinListener;
import com.threerings.msoy.avrg.data.AVRGameConfig;
import com.threerings.msoy.avrg.data.AVRGameMarshaller;
import com.threerings.msoy.avrg.data.AVRGameObject;
import com.threerings.msoy.avrg.data.AVRMarshaller;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.game.client.GameLiaison;
import com.threerings.msoy.game.client.SplashPlaceView;
import com.threerings.msoy.world.client.WorldContext;

/**
 * Handles the AVRG-specific aspects of the game server connection.
 */
public class AVRGameLiaison extends GameLiaison
    implements AVRService_AVRGameJoinListener
{
    public const log :Log = Log.getLog(this);

    // statically reference classes we require
    AVRGameMarshaller;
    AVRMarshaller;

    public function AVRGameLiaison (ctx :WorldContext, gameId :int)
    {
        super(ctx, gameId);

        // let the social director spy on us and make suggestions
        _wctx.getSocialDirector().trackAVRGame(_gctx);
    }

    override public function clientWillLogon (event :ClientEvent) :void
    {
        super.clientWillLogon(event);

        // AVRG's need access to the world services, too.
        _gctx.getClient().addServiceGroup(MsoyCodes.WORLD_GROUP);
    }

    override public function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);

        var svc :AVRService = (_gctx.getClient().requireService(AVRService) as AVRService);
        svc.activateGame(_gameId, this);

        // Call shutdown when the location is cleared
        var listener :AttributeChangeAdapter;
        listener = new AttributeChangeAdapter(
            function (evt :AttributeChangedEvent) :void {
                if (evt.getName() == BodyObject.LOCATION) {
                    if (evt.getValue() == null) {
                        _gctx.getClient().getClientObject().removeListener(listener);
                        shutdown();
                    }
                }
            });
        _gctx.getClient().getClientObject().addListener(listener);
    }

    // from AVRGameJoinListener
    override public function requestFailed (cause :String) :void
    {
        // GameLiaison conveniently already handles this
        super.requestFailed(cause);

        shutdown();
    }

    // from AVRGameJoinListener
    public function avrgJoined (placeOid :int, config :AVRGameConfig) :void
    {
        // since we hijacked the movement process server-side, let the client catch up
        _gctx.getLocationDirector().didMoveTo(placeOid, config);

        // now that the controller is created, tell it about the world context as well
        getAVRGameController().initializeWorldContext(_wctx);

        // handle deactivations to offer the user to share earned trophies
        getAVRGameController().addDeactivateHandler(onUserDeactivate);

        // tell interested parties (ie the party director) that we're now gaming
        _wctx.getGameDirector().dispatchGamingStateChanged();

        // if we're not in a room, stuff a display with our splash media into view
        if (_wctx.getLocationDirector().getPlaceObject() == null) {
            _wctx.setPlaceView(new SplashPlaceView(config.getSplash(), config.getThumbnail()));
        }

        // if we started the game afk/idle, tell the server
        if (_wctx.getWorldController().isIdle()) {
            getAVRGameController().setIdle(true);
        }
    }

    override public function shutdown () :void
    {
        super.shutdown();

        // TODO, If we are in "no place", we should go to the last place visited, or home.
        // The last place is problematic because it could have been a scene that triggered
        // our participation in this AVRG in the first place, so what we really want is
        // "the last place visited before we started playing this game". But even that could
        // be somewhat wrong if you jumped directly from one AVRG to another. Do you really
        // want to go back? So: home. But home itself could have forced you into an AVRG,
        // perhaps even the one you are trying to quit. Perhaps the ultimate solution is
        // to pop up a little dealy: "where do you want to go today?". For now: go home.
        // ...however, don't go anywhere if the client is logged off or about to
        if (_wctx.getMsoyClient().isConnected() && !_wctx.getMsoyClient().isLogoffPending() &&
            0 == _wctx.getWorldController().getCurrentSceneId()) {
            _wctx.getWorldController().handleGoScene(_wctx.getMemberObject().homeSceneId);
        }
    }

    public function leaveAVRGame () :void
    {
        // remove our trophy feed display stuff
        if (getAVRGameController() == null) {
            log.warning("Controller null on leaveAVRGame?");
        } else {
            getAVRGameController().removeDeactivateHandler(onUserDeactivate);
        }

        // NOTE: Ray 2009-07-24: the callback to this service does not seem to be getting called.
        // Perhaps deactivating the game causes the connection to drop, and so we never get
        // the response?
        var svc :AVRService = (_gctx.getClient().requireService(AVRService) as AVRService);
        svc.deactivateGame(_gameId,
            _gctx.getWorldContext().confirmListener(_gctx.getLocationDirector().leavePlace,
                null, null, null, "gameId", _gameId));
    }

    /**
     * Returns the backend if we're currently in an AVRG, null otherwise.
     */
    public function getAVRGameBackend () :AVRGameBackend
    {
        var ctrl :AVRGameController = getAVRGameController();
        return (ctrl != null) ? ctrl.backend : null;
    }

    /**
     * Returns the game object if we're currently in an AVRG, null otherwise.
     */
    public function getAVRGameController () :AVRGameController
    {
        var ctrl :PlaceController = _gctx.getLocationDirector().getPlaceController();
        return (ctrl != null) ? (ctrl as AVRGameController) : null;
    }

    // from interface MessageListener
    override public function messageReceived (event :MessageEvent) :void
    {
        super.messageReceived(event);

        if (event.getName() == AVRGameObject.TASK_COMPLETED_MESSAGE) {
            var coins :int = int(event.getArgs()[1]);
            const forReal :Boolean = Boolean(event.getArgs()[2]);
            const hasCookie :Boolean = true; // we always assume AVRGs have saved state
            if (forReal && _gctx.getPlayerObject().isPermaguest()) {
                // if a guest earns flow, we want to show them the "please register" dialog
                displayGuestFlowEarnage(coins, hasCookie);
            }
        }
    }

    protected function onUserDeactivate () :Boolean
    {
        var tryAgain :Function = getAVRGameController().deactivateGame;
        if (!_wctx.getSocialDirector().mayDeactivateAVRGame(tryAgain)) {
            return false;
        }
        return maybeShowFeedPanel(tryAgain);
    }

    override protected function maybeShowFeedPanel (onClose :Function) :Boolean
    {
        // remove the handler, we don't want to show this twice
        if (getAVRGameController() == null) {
            log.warning("Null controller in showFeedPanel?");
        } else {
            getAVRGameController().removeDeactivateHandler(onUserDeactivate);
        }

        return super.maybeShowFeedPanel(onClose);
    }
}
}
