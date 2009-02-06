//
// $Id$

package com.threerings.msoy.client {

import flash.display.Stage;

import flash.geom.Rectangle;

import mx.core.UIComponent;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.client.ConfirmAdapter;
import com.threerings.presents.client.InvocationAdapter;
import com.threerings.presents.client.InvocationService_ConfirmListener;
import com.threerings.presents.client.InvocationService_InvocationListener;
import com.threerings.presents.client.InvocationService_ResultListener;
import com.threerings.presents.client.ResultAdapter;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.BodyObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.ChatDirector;
import com.threerings.crowd.chat.client.MuteDirector;

import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.MsoyTokenRing;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.client.CurseFilter;
import com.threerings.msoy.chat.client.MsoyChatDirector;

import com.threerings.msoy.notify.client.NotificationDirector;

/**
 * Provides services shared by all clients.
 */
public /*abstract*/ class MsoyContext
    implements CrowdContext
{
    public function MsoyContext (client :MsoyClient)
    {
        _client = client;

        // initialize the message manager
        _msgMgr = new MessageManager();
        // and our convenience holder
        Msgs.init(_msgMgr);

        _locDir = new LocationDirector(this);
        _occDir = new OccupantDirector(this);
        _chatDir = createChatDirector();
        _chatDir.addChatFilter(new CurseFilter(this));

        // the top panel's constructor will add it to the app's UI hierarchy
        _topPanel = new TopPanel(this, createControlBar());

        // we create some of our directors in a method that can be overridden
        createAdditionalDirectors();
    }

    /**
     * Get the width of the client.
     * By default this is just the stage width, but that should not be assumed!
     * Certain subclasses override this method and in the future it could become
     * more complicated due to embedding.
     */
    public function getWidth () :Number
    {
        return _client.getStage().stageWidth;
    }

    /**
     * Get the height of the client. Please review the the notes in getWidth().
     */
    public function getHeight () :Number
    {
        return _client.getStage().stageHeight;
    }

    public function getStage () :Stage
    {
        return _client.getStage();
    }

    /**
     * Returns our client as its MsoyClient self.
     */
    public function getMsoyClient () :MsoyClient
    {
        return _client;
    }

    /**
     * Returns a reference to the top-level UI container.
     */
    public function getTopPanel () :TopPanel
    {
        return _topPanel;
    }

    /**
     * Return the control bar
     */
    public function getControlBar () :ControlBar
    {
        return _topPanel.getControlBar();
    }

    /**
     * Return's this client's member name.
     */
    public function getMyName () :MemberName
    {
        var body :BodyObject = _client.getClientObject() as BodyObject;
        return (body == null) ? null : body.getVisibleName() as MemberName;
    }

    /**
     * Returns this client's access control tokens.
     */
    public function getTokens () :MsoyTokenRing
    {
        throw new Error("abstract");
    }

    /**
     * Returns the Whirled cobrand partner in effect or null if we're running in the standard
     * Whirled webapp or emebedded mode.
     */
    public function getPartner () :String
    {
        return MsoyParameters.get()["partner"];
    }

    /**
     * Create an InvocationListener that will automatically log and report errors to chat.
     *
     * @param bundle the MessgeBundle to use to translate the error message.
     * @param errWrap if not null, a translation key used to report the error, with the
     *        'cause' String from the server as it's argument.
     * @param logArgs arguments to use when logging the error. An even number of arguments
     *        may be specified in the "description", value, "description", value format.
     *        Specifying an odd number of arguments uses the first arg as the primary log message,
     *        instead of something generic like "An error occurred".
     */
    public function listener (
        bundle :String = MsoyCodes.GENERAL_MSGS, errWrap :String = null, ... logArgs)
        :InvocationService_InvocationListener
    {
        return new InvocationAdapter(chatErrHandler(bundle, errWrap, null, logArgs));
    }

    /**
     * Create a ConfirmListener that will automatically log and report errors to chat.
     *
     * @param confirm if a String, a message that will be reported on success. If a function,
     *        it will be run on success.
     * @param component if non-null, a component that will be disabled, and re-enabled when
     *        the response arrives from the server (success or failure).
     * @see listener() for a description of the rest of the arguments.
     */
    public function confirmListener (
        confirm :* = null, bundle :String = MsoyCodes.GENERAL_MSGS, errWrap :String = null,
        component :UIComponent = null, ... logArgs)
        :InvocationService_ConfirmListener
    {
        var success :Function = function () :void {
            if (component != null) {
                component.enabled = true;
            }
            if (confirm is Function) {
                (confirm as Function)();
            } else if (confirm is String) {
                displayFeedback(bundle, String(confirm));
            }
        };
        if (component != null) {
            component.enabled = false;
        }
        return new ConfirmAdapter(success, chatErrHandler(bundle, errWrap, component, logArgs));
    }

    /**
     * Create a ResultListener that will automatically log and report errors to chat.
     *
     * @param gotResult a function that will be passed a single result argument from the server.
     * @param component if non-null, a component that will be disabled, and re-enabled when
     *        the response arrives from the server (success or failure).
     * @see listener() for a description of the rest of the arguments.
     */
    public function resultListener (
        gotResult :Function, bundle :String = MsoyCodes.GENERAL_MSGS, errWrap :String = null,
        component :UIComponent = null, ... logArgs)
        :InvocationService_ResultListener
    {
        var success :Function;
        if (component == null) {
            success = gotResult;
        } else {
            component.enabled = false;
            success = function (result :Object) :void {
                component.enabled = true;
                gotResult(result);
            };
        }
        return new ResultAdapter(success, chatErrHandler(bundle, errWrap, component, logArgs));
    }

    /**
     * Convenience method.
     */
    public function displayFeedback (bundle :String, message :String) :void
    {
        _chatDir.displayFeedback(bundle, message);
    }

    /**
     * Convenience method.
     */
    public function displayInfo (bundle :String, message :String, localType :String = null) :void
    {
        _chatDir.displayInfo(bundle, message, localType);
    }

    /**
     * Returns the chat director casted to an MsoyChatDirector.
     */
    public function getMsoyChatDirector () :MsoyChatDirector
    {
        return _chatDir;
    }

    // from PresentsContext
    public function getClient () :Client
    {
        return _client;
    }

    // from PresentsContext
    public function getDObjectManager () :DObjectManager
    {
        return _client.getDObjectManager();
    }

    // from CrowdContext
    public function getLocationDirector () :LocationDirector
    {
        return _locDir;
    }

    // from CrowdContext
    public function getOccupantDirector () :OccupantDirector
    {
        return _occDir;
    }

    // from CrowdContext
    public function getChatDirector () :ChatDirector
    {
        return _chatDir;
    }

    /**
     * Get the mute director.
     */
    public function getMuteDirector () :MuteDirector
    {
        return _muteDir;
    }

    /**
     * Get the notification director.
     */
    public function getNotificationDirector () :NotificationDirector
    {
        return _notifyDir;
    }

    /**
     * Get the message manager.
     */
    public function getMessageManager () :MessageManager
    {
        return _msgMgr;
    }

    /**
     * Returns the top-level client controller.
     */
    public function getMsoyController () :MsoyController
    {
        throw new Error("abstract");
    }

    /**
     * Get the current boundaries of the PlaceView, in global coordinates.
     */
    public function getPlaceViewBounds () :Rectangle
    {
        return _topPanel.getPlaceViewBounds();
    }

    /**
     * Return the current PlaceView.
     */
    public function getPlaceView () :PlaceView
    {
        return _topPanel.getPlaceView();
    }

    // documentation inherited from superinterface CrowdContext
    public function setPlaceView (view :PlaceView) :void
    {
        _topPanel.setPlaceView(view);
    }

    // documentation inherited from superinterface CrowdContext
    public function clearPlaceView (view :PlaceView) :void
    {
        _topPanel.clearPlaceView(view);
    }

    public function getUIState () :UIState
    {
        return _uiState;
    }

    /**
     * Convenience translation method. If the first arg imethod to translate a key using the
     * general bundle.
     */
    public function xlate (bundle :String, key :String, ... args) :String
    {
        args.unshift(key);
        if (bundle == null) {
            bundle = MsoyCodes.GENERAL_MSGS;
        }
        var mb :MessageBundle = _msgMgr.getBundle(bundle);
        return mb.get.apply(mb, args);
    }

    /**
     * Create an error handling function for use with InvocationService listener adapters.
     */
    protected function chatErrHandler (
        bundle :String, errWrap :String, component :UIComponent, logArgs :Array) :Function
    {
        return function (cause :String) :void {
            if (component != null) {
                component.enabled = true;
            }
            var args :Array = logArgs.concat("cause", cause); // make a copy, we're reentrant
            if (args.length % 2 == 0) {
                args.unshift("Reporting failure");
            }
            Log.getLog(MsoyContext).info.apply(null, args);

            if (errWrap != null) {
                cause = MessageBundle.compose(errWrap, cause);
            }
            displayFeedback(bundle, cause);
        };
    }

    /**
     * Create extra directors that we may want to suppress in certain subclasses.
     */
    protected function createAdditionalDirectors () :void
    {
        _notifyDir = new NotificationDirector(this);
        _muteDir = new MuteDirector(this);
        _muteDir.setChatDirector(_chatDir);
    }

    /**
     * Creates a potentially custom version of our control bar.
     */
    protected function createControlBar () :ControlBar
    {
        return new ControlBar(this);
    }

    /**
     * Creates a potentially custom chat director.
     */
    protected function createChatDirector () :MsoyChatDirector
    {
        return new MsoyChatDirector(this);
    }

    protected var _client :MsoyClient;
    protected var _topPanel :TopPanel;
    protected var _uiState :UIState = new UIState();

    protected var _msgMgr :MessageManager;
    protected var _locDir :LocationDirector;
    protected var _occDir :OccupantDirector;
    protected var _chatDir :MsoyChatDirector;
    protected var _muteDir :MuteDirector;
    protected var _notifyDir :NotificationDirector;
}
}
