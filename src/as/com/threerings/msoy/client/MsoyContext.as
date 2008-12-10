//
// $Id$

package com.threerings.msoy.client {

import flash.display.Stage;

import flash.geom.Rectangle;

import com.threerings.util.Log;
import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
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
     * For use with Invocation service listener adapters. A function for handling any error
     * and reporting via a chat feedback message. If you want to just use something like this
     * as your complete listener, see ReportingListener.
     *
     * @param bundle the MessageBundle identifier to use for translating error causes.
     * @param errWrap a translation key in which to wrap the 'cause' string from the server.
     * @poram logArgs any arguments you wish to pass to be logged. If the number of args is
     *        even then the message logged is [ "Reporting failure", <your args>, "cause", cause ];
     *        if the number of args is odd then your first arg is used in place of
     *        "Reporting failure".
     */
    public function chatErrHandler (
        bundle :String = null, errWrap :String = null, ... logArgs) :Function
    {
        return function (cause :String) :void {
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
     * Convenience method.
     */
    public function displayFeedback (bundle :String, message :String) :void
    {
        _chatDir.displayFeedback(bundle, message);
    }

    /**
     * Convenience method.
     */
    public function displayInfo (bundle :String, message :String) :void
    {
        _chatDir.displayInfo(bundle, message);
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

    protected var _msgMgr :MessageManager;
    protected var _locDir :LocationDirector;
    protected var _occDir :OccupantDirector;
    protected var _chatDir :MsoyChatDirector;
    protected var _muteDir :MuteDirector;
    protected var _notifyDir :NotificationDirector;
}
}
