//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Stage;
import flash.display.StageQuality;
import flash.events.Event;
import flash.external.ExternalInterface;
import flash.geom.Point;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLVariables;

import flash.utils.Dictionary;

import com.adobe.crypto.MD5;

import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.data.ClientObject;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.net.Credentials;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.EmbedHeader;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyParameters;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.WorldCredentials;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.chat.data.MsoyChatChannel;
import com.threerings.msoy.room.client.RoomObjectView;

/**
 * Handles the main services for the world and game clients.
 */
public class WorldClient extends MsoyClient
{
    public function WorldClient (stage :Stage)
    {
        super(stage);

        // TODO: allow users to choose? I think it's a decision that we should make for them.
        // Jon speculates that maybe we can monitor the frame rate and automatically shift it,
        // but noticable jiggles occur when it's switched and I wouldn't want the entire
        // world to jiggle when someone starts walking, then jiggle again when they stop.
        // So: for now we just peg it to MEDIUM.
        stage.quality = StageQuality.MEDIUM;

        // if we are embedded, we won't have a server host in our parameters, so we need to obtain
        // that via an HTTP request, otherwise just logon directly
        var params :Object = MsoyParameters.get();

        // if we're an embedded client, turn on the embed header
        if (getHostname() == null && !_featuredPlaceView) {
            _wctx.getTopPanel().setTopPanel(new EmbedHeader(_wctx));
        }

        // if we are going right into a game lobby, do that now and once we get into the game, then
        // we'll be able to logon to a world with our assigned credentials
        if (params["gameLobby"]) {
            log.info("Doing pre-logon go to join game lobby.");
            _wctx.getWorldController().preLogonGo(params);

        } else if (getHostname() == null) {
            var loader :URLLoader = new URLLoader();
            loader.addEventListener(Event.COMPLETE, function () :void {
                loader.removeEventListener(Event.COMPLETE, arguments.callee);
                var bits :Array = (loader.data as String).split(":");
                setServer(bits[0], [ int(bits[1]) ]);
                logon();
            });
            // TODO: add listeners for failure events? give feedback on failure?

            // embedded clients should link to a particular scene (or game in which case we'll just
            // connect to any old world server)
            var sceneId :int = int(params["sceneId"]);
            var url :String = DeploymentConfig.serverURL + "embed/" +
                (sceneId == 0 ? "" : ("s"+sceneId));
            loader.load(new URLRequest(url));
            log.info("Loading server info from " + url + ".");

        } else {
            logon();
        }

        if (_featuredPlaceView) {
            var overlay :FeaturedPlaceOverlay = new FeaturedPlaceOverlay(_ctx);
            _ctx.getTopPanel().getPlaceContainer().addOverlay(
                overlay, PlaceBox.LAYER_FEATURED_PLACE);
        }
    }

    // from Client
    override public function gotBootstrap (data :BootstrapData, omgr :DObjectManager) :void
    {
        super.gotBootstrap(data, omgr);

        // save any machineIdent or sessionToken from the server.
        var rdata :MsoyAuthResponseData = (getAuthResponseData() as MsoyAuthResponseData);
        if (rdata.ident != null) {
            Prefs.setMachineIdent(rdata.ident);
        }
        if (rdata.sessionToken != null) {
            Prefs.setSessionToken(rdata.sessionToken);

            // record whether or not we used a token to login
            _usedToken = WorldCredentials(getCredentials()).sessionToken != null;

            // fill our session token into our credentials so that we can log in more efficiently
            // on a reconnect, so that we can log into game servers and so that guests can preserve
            // some sense of identity during the course of their session
            WorldCredentials(getCredentials()).sessionToken = rdata.sessionToken;
        }

        log.info("Client logged on [built=" + DeploymentConfig.buildTime +
                 ", mediaURL=" + DeploymentConfig.mediaURL +
                 ", staticMediaURL=" + DeploymentConfig.staticMediaURL + "].");
    }

    // from Client
    override public function gotClientObject (clobj :ClientObject) :void
    {
        super.gotClientObject(clobj);

        if (clobj is MemberObject && !_embedded && !_featuredPlaceView) {
            var member :MemberObject = clobj as MemberObject;
            member.addListener(new AvatarUpdateNotifier(_wctx));
        }

        if (!_featuredPlaceView) {
            // listen for coins and bars updates
            _user = (clobj as MemberObject);
            var updater :StatusUpdater = new StatusUpdater(this);
            _user.addListener(updater);

            // configure our levels to start
            updater.newLevel(_user.level);
            // updater.newGold(_user.gold);
            updater.newCoins(_user.coins);
            updater.newMail(_user.newMailCount);
        }
    }

    /**
     * Exposed to javascript so that it may notify us to logon.
     */
    protected function externalClientLogon (memberId :int, token :String) :void
    {
        if (token == null) {
            return;
        }

        log.info("Logging on via external request [id=" + memberId + ", token=" + token + "].");
        var co :MemberObject = _wctx.getMemberObject();
        if (co == null || co.getMemberId() != memberId) {
            _wctx.getMsoyController().handleLogon(createStartupCreds(token));
        }
    }

    /** @inheritDoc */
    // from MsoyClient
    override protected function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);

        var member :MemberObject = _clobj as MemberObject;
        if (member == null || _embedded) {
            return;
        }

        // set or reset our permaguest stuff
        var username :String = member.username.toString();
        var memname :String = member.memberName.toString();
        if (MemberName.isPermaguest(username)) {
            log.info("You are a permaguest", "name", username, "mname", memname);
            Prefs.setPermaguestUsername(username);

            var serverToken :String =
                _authData != null ? MsoyAuthResponseData(_authData).sessionToken : null;

            if (!_usedToken && serverToken != null) {
                // the server has created an account for us, yippee! let gwt know
                if (ExternalInterface.available) {
                    log.info("Setting permaguest token to GWT", "token", serverToken);
                    ExternalInterface.call("setPermaguestInfo", username, serverToken);
                }
            }
        }
    }

    /**
     * Exposed to javascript so that it may notify us to move to a new location.
     */
    protected function externalClientGo (where :String) :Boolean
    {
        if (_wctx.getClient().isLoggedOn()) {
            log.info("Changing scenes per external request [where=" + where + "].");
            _wctx.getWorldController().goToPlace(new URLVariables(where));
            return true;
        } else {
            log.info("Not ready to change scenes (we're not logged on) [where=" + where + "].");
            return false;
        }
    }

    /**
     * Exposed to javascript so that it may notify us to logoff.
     */
    protected function externalClientLogoff (backAsGuest :Boolean = true) :void
    {
        log.info("Logging off via external request [backAsGuest=" + backAsGuest + "].");

        if (backAsGuest) {
            var creds :WorldCredentials = new WorldCredentials(null, null);
            creds.ident = "";
            _wctx.getMsoyController().handleLogon(creds);
        } else {
            logoff(false);
        }
    }

    /**
     * Exposed to javascript so that the it may determine if the current scene is a room.
     */
    protected function externalInRoom () :Boolean
    {
        return _wctx.getPlaceView() is RoomObjectView;
    }

    /**
     * Exposed to javascript so that the it may determine if the scene id.
     */
    protected function externalGetSceneId () :int
    {
        var scene :Scene = _wctx.getSceneDirector().getScene();
        return (scene == null) ? 0 : scene.getId();
    }

    /**
     * Exposed to javascript so that it may tell us to use this avatar.  If the avatarId of 0 is
     * passed in, the current avatar is simply cleared away, leaving them with the default.
     */
    protected function externalUseAvatar (avatarId :int) :void
    {
        _wctx.getWorldDirector().setAvatar(avatarId);
    }

    /**
     * Exposed to javascript so that the avatarviewer may update the scale of an avatar
     * in real-time.
     */
    protected function externalUpdateAvatarScale (avatarId :int, newScale :Number) :void
    {
        var view :RoomObjectView = _wctx.getPlaceView() as RoomObjectView;
        if (view != null) {
            view.updateAvatarScale(avatarId, newScale);
        }
    }

    /**
     * Exposed to javascript so that it may tell us to use items in the current room, either as
     * background items, or as furni as apporpriate.
     */
    protected function externalUseItem (itemType :int, itemId :int) :void
    {
        var view :RoomObjectView = _wctx.getPlaceView() as RoomObjectView;
        if (view != null) {
            view.getRoomObjectController().useItem(itemType, itemId);
        }
    }

    /**
     * Exposed to javascript so that it may tell us to remove an item from use.
     */
    protected function externalClearItem (itemType :int, itemId :int) :void
    {
        var view :RoomObjectView = _wctx.getPlaceView() as RoomObjectView;
        if (view != null) {
            view.getRoomObjectController().clearItem(itemType, itemId);
        }
    }

    /**
     * Exposed to JavaScript so that it may order us to open chat channels.
     */
    protected function externalOpenChannel (type :int, name :String, id :int) :void
    {
        var nameObj :Name;
        if (type == MsoyChatChannel.MEMBER_CHANNEL) {
            nameObj = new MemberName(name, id);
        } else if (type == MsoyChatChannel.GROUP_CHANNEL) {
            nameObj = new GroupName(name, id);
        } else if (type == MsoyChatChannel.PRIVATE_CHANNEL) {
            nameObj = new ChannelName(name, id);
        } else {
            throw new Error("Unknown channel type: " + type);
        }
        _wctx.getMsoyChatDirector().openChannel(nameObj);
    }

    protected function externalStartTour () :void
    {
        _wctx.getTourDirector().startTour();
    }

    // from MsoyClient
    override protected function createContext () :MsoyContext
    {
        return (_wctx = new WorldContext(this));
    }

    // from MsoyClient
    override protected function configureExternalFunctions () :void
    {
        super.configureExternalFunctions();

        ExternalInterface.addCallback("clientLogon", externalClientLogon);
        ExternalInterface.addCallback("clientGo", externalClientGo);
        ExternalInterface.addCallback("clientLogoff", externalClientLogoff);
        ExternalInterface.addCallback("inRoom", externalInRoom);
        ExternalInterface.addCallback("getSceneId", externalGetSceneId);
        ExternalInterface.addCallback("useAvatar", externalUseAvatar);
        ExternalInterface.addCallback("updateAvatarScale", externalUpdateAvatarScale);
        ExternalInterface.addCallback("useItem", externalUseItem);
        ExternalInterface.addCallback("clearItem", externalClearItem);
        ExternalInterface.addCallback("openChannel", externalOpenChannel);
        ExternalInterface.addCallback("startTour", externalStartTour);
    }

    // from MsoyClient
    override protected function populateContextMenu (custom :Array) :void
    {
        try {
            var allObjects :Array = _stage.getObjectsUnderPoint(
                new Point(_stage.mouseX, _stage.mouseY));
            var seen :Dictionary = new Dictionary();
            for each (var disp :DisplayObject in allObjects) {
                try {
                    while (disp != null && !(disp in seen)) {
                        seen[disp] = true;
                        if (disp is ContextMenuProvider) {
                            (disp as ContextMenuProvider).populateContextMenu(_wctx, custom);
                        }
                        disp = disp.parent;
                    }
                } catch (serr :SecurityError) {
                    // that's ok, let's move on
                }
            }
        } catch (e :Error) {
            log.logStackTrace(e);
        }
    }

    // from MsoyClient
    override protected function createStartupCreds (token :String) :Credentials
    {
        var params :Object = MsoyParameters.get();
        var creds :WorldCredentials;
        if ((params["pass"] != null) && (params["user"] != null)) {
            creds = new WorldCredentials(
                new Name(String(params["user"])), MD5.hash(String(params["pass"])));
        } else {
            creds = new WorldCredentials(null, null);
        }

        creds.sessionToken = (token == null) ? params["token"] : token;
        creds.ident = Prefs.getMachineIdent();
        creds.featuredPlaceView = _featuredPlaceView;
        creds.visitorId = getVisitorId();

        return creds;
    }

    protected var _wctx :WorldContext;
    protected var _user :MemberObject;
    protected var _usedToken :Boolean;

    private static const log :Log = Log.getLog(WorldClient);
}
}

import flash.external.ExternalInterface;

import com.threerings.util.Log;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.FriendEntry;

import com.threerings.msoy.world.client.WorldContext;

class AvatarUpdateNotifier implements AttributeChangeListener
{
    public function AvatarUpdateNotifier (wctx :WorldContext)
    {
        _wctx = wctx;
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (MemberObject.AVATAR == event.getName()) {
            var value :Object = event.getValue();
            if (value is Avatar) {
                _wctx.getMsoyClient().itemUsageChangedToGWT(
                    Item.AVATAR, (value as Avatar).itemId, Item.USED_AS_AVATAR,
                    _wctx.getMyName().getMemberId());
            }
            value = event.getOldValue();
            if (value is Avatar) {
                _wctx.getMsoyClient().itemUsageChangedToGWT(
                    Item.AVATAR, (value as Avatar).itemId, Item.UNUSED, 0);
            }
        }
    }

    protected var _wctx :WorldContext;
}

class StatusUpdater implements AttributeChangeListener, SetListener
{
    public function StatusUpdater (client :MsoyClient)
    {
        _client = client;
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
        case MemberObject.COINS:
            newCoins(event.getValue() as int, event.getOldValue() as int);
            break;

        case MemberObject.BARS:
            newBars(event.getValue() as int, event.getOldValue() as int);
            break;

        case MemberObject.LEVEL:
            newLevel(event.getValue() as int, event.getOldValue() as int);
            break;

        case MemberObject.NEW_MAIL_COUNT:
            newMail(event.getValue() as int, event.getOldValue() as int);
            break;
        }
    }

    public function entryAdded (event :EntryAddedEvent) :void
    {
        switch (event.getName()) {
        case MemberObject.FRIENDS:
            var entry :FriendEntry = (event.getEntry() as FriendEntry);
            _client.dispatchEventToGWT(
                FRIEND_EVENT, [FRIEND_ADDED, entry.name.toString(), entry.name.getMemberId()]);
            break;
        }
    }

    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        // nada
    }

    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        switch (event.getName()) {
        case MemberObject.FRIENDS:
            var memberId :int = int(event.getKey());
            _client.dispatchEventToGWT(FRIEND_EVENT, [FRIEND_REMOVED, "", memberId]);
            break;
        }
    }

    public function newLevel (level :int, oldLevel :int = 0) :void {
        sendNotification([STATUS_CHANGE_LEVEL, level, oldLevel]);
    }

    public function newCoins (coins :int, oldCoins :int = 0) :void {
        sendNotification([STATUS_CHANGE_COINS, coins, oldCoins]);
    }

    public function newBars (bars :int, oldBars :int = 0) :void {
        sendNotification([STATUS_CHANGE_BARS, bars, oldBars]);
    }

    public function newMail (mail :int, oldMail :int = -1) :void {
        sendNotification([STATUS_CHANGE_MAIL, mail, oldMail]);
    }

    protected function sendNotification (args :Array) :void {
        _client.dispatchEventToGWT(STATUS_CHANGE_EVENT, args);
    }

    /** Event dispatched to GWT when we've leveled up */
    protected static const STATUS_CHANGE_EVENT :String = "statusChange";
    protected static const STATUS_CHANGE_LEVEL :int = 1;
    protected static const STATUS_CHANGE_COINS :int = 2;
    protected static const STATUS_CHANGE_BARS :int = 3;
    protected static const STATUS_CHANGE_MAIL :int = 4;

    /** Event dispatched to GWT when we add or remove a friend. */
    protected static const FRIEND_EVENT :String = "friend";
    protected static const FRIEND_ADDED :int = 1;
    protected static const FRIEND_REMOVED :int = 2;

    protected var _client :MsoyClient;
}
