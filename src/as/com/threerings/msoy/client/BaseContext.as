//
// $Id$

package com.threerings.msoy.client {

import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.Stage;

import com.threerings.util.MessageBundle;
import com.threerings.util.MessageManager;

import com.threerings.presents.client.Client;
import com.threerings.presents.dobj.DObjectManager;

import com.threerings.crowd.client.LocationDirector;
import com.threerings.crowd.client.OccupantDirector;
import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.crowd.chat.client.ChatDirector;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.chat.client.CurseFilter;

/**
 * Provides services shared by all clients.
 */
public /*abstract*/ class BaseContext
    implements CrowdContext
{
    public function BaseContext (client :Client)
    {
        _client = client;

        // initialize the message manager
        _msgMgr = new MessageManager();
        // and our convenience holder
        Msgs.init(_msgMgr);

        _helper = new ContextHelper();

        _locDir = new LocationDirector(this);
        _occDir = new OccupantDirector(this);
        _chatDir = createChatDirector();
        _chatDir.setChatterValidator(_helper);
        _chatDir.addChatFilter(new CurseFilter(this));
        _memberDir = new MemberDirector(this);
    }

    public function getStage () :Stage
    {
        return _client.getStage();
    }

    /**
     * Returns the Whirled cobrand partner in effect or null if we're running in the standard
     * Whirled webapp or emebedded mode.
     */
    public function getPartner () :String
    {
        return getStage().loaderInfo.parameters["partner"];
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

    // from PresentsContext
    public function getClient () :Client
    {
        return _client;
    }

    /**
     * Convenience method.
     */
    public function getMemberObject () :MemberObject
    {
        return (_client.getClientObject() as MemberObject);
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
     * Get the MemberDirector.
     */
    public function getMemberDirector () :MemberDirector
    {
        return _memberDir;
    }

    /**
     * Get the message manager.
     */
    public function getMessageManager () :MessageManager
    {
        return _msgMgr;
    }

//     /**
//      * Get the top-level msoy controller.
//      */
//     public function getMsoyController () :MsoyController
//     {
//         return _controller;
//     }

    // documentation inherited from superinterface CrowdContext
    public function setPlaceView (view :PlaceView) :void
    {
        // TODO: unimplemented
    }

    // documentation inherited from superinterface CrowdContext
    public function clearPlaceView (view :PlaceView) :void
    {
        // TODO: unimplemented
    }

//     public function getTopPanel () :TopPanel
//     {
//         return _topPanel;
//     }

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

    protected function createChatDirector () :ChatDirector
    {
        return new ChatDirector(this, _msgMgr, MsoyCodes.CHAT_MSGS);
    }

    protected var _client :Client;

    protected var _helper :ContextHelper;

//     protected var _topPanel :TopPanel;

//     protected var _controller :MsoyController;

    protected var _msgMgr :MessageManager;

    protected var _locDir :LocationDirector;

    protected var _occDir :OccupantDirector;

    protected var _chatDir :ChatDirector;

    protected var _memberDir :MemberDirector;
}
}

import com.threerings.util.Name;

import com.threerings.crowd.chat.client.ChatterValidator;

import com.threerings.msoy.data.all.MemberName;

/**
 * A helper class that implements common helper interfaces that we would not like to see exposed on
 * the BaseContext class.  In Java, this would be handled by having a number of anonymous inner
 * classes.
 */
class ContextHelper
    implements ChatterValidator
{
    // from ChatterValidator
    public function isChatterValid (username :Name) :Boolean
    {
        return (username is MemberName) &&
            (MemberName.GUEST_ID != (username as MemberName).getMemberId());
    }
}
