//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.Stage;
import flash.display.StageQuality;
import flash.events.Event;
import flash.events.IEventDispatcher;
import flash.external.ExternalInterface;
import flash.geom.Point;
import flash.net.LocalConnection;
import flash.net.URLLoader;
import flash.net.URLRequest;
import flash.net.URLVariables;
import flash.utils.Dictionary;

import com.adobe.crypto.MD5;

import com.threerings.util.Log;
import com.threerings.util.Name;

import com.threerings.presents.client.ClientAdapter;
import com.threerings.presents.client.ClientEvent;
import com.threerings.presents.dobj.DObjectManager;
import com.threerings.presents.net.BootstrapData;
import com.threerings.presents.net.Credentials;

import com.threerings.whirled.data.Scene;

import com.threerings.msoy.chat.data.MsoyChatChannel;
import com.threerings.msoy.client.ContextMenuProvider;
import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.GuestSessionCapture;
import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyParameters;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.client.Prefs;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyAuthResponseData;
import com.threerings.msoy.data.UberClientModes;
import com.threerings.msoy.data.WorldCredentials;
import com.threerings.msoy.data.all.ChannelName;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.data.all.VisitorInfo;
import com.threerings.msoy.room.client.RoomObjectView;
import com.threerings.msoy.world.data.WorldMarshaller;

/**
 * Handles the main services for the world and game clients.
 */
public class WorldClient extends MsoyClient
{
    WorldMarshaller; // static reference for deserialization

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
        if (isEmbedded() && !_featuredPlaceView) {
            _wctx.getUIState().setEmbedded(true);
        }

        _minimized = params["minimized"] != null;

        // if we are going right into a game, do that now and once we get into the game, then we'll
        // be able to logon to a world with our assigned credentials
        if (params["gameId"] || params["gameLobby"] /* legacy */) {
            log.info("Doing pre-logon go to join game", "gameId", params["gameId"]);
            _wctx.getWorldController().preLogonGo(params);

        } else if (getHostname() == null) {
            var loader :URLLoader = new URLLoader();
            var worldClient :WorldClient = this;
            loader.addEventListener(Event.COMPLETE, function () :void {
                loader.removeEventListener(Event.COMPLETE, arguments.callee);
                var bits :Array = (loader.data as String).split(":");
                setServer(bits[0], [ int(bits[1]) ]);
                GuestSessionCapture.capture(worldClient);
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
            GuestSessionCapture.capture(this);
            logon();
        }

        if (_featuredPlaceView) {
            var overlay :FeaturedPlaceOverlay = new FeaturedPlaceOverlay(_ctx);
            _ctx.getTopPanel().getPlaceContainer().addOverlay(
                overlay, PlaceBox.LAYER_FEATURED_PLACE);
        }
    }

    override public function logon () :Boolean
    {
        var lock :LocalConnection = claimLock();
        if (lock == null) {
            // A client is already connected in another tab
            var params :Object = MsoyParameters.get();
            if ("true" == params["auto"]) {
                // Yield to it if this client was automatically opened
                closeClient();
                return false;
            }
        } else {
            lock.close();
        }

        // Otherwise proceed with logging on
        return super.logon();
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

        // store the session token
        _ctx.saveSessionToken(getAuthResponseData());

        log.info("Client logged on",
            "build", DeploymentConfig.buildTime, "mediaURL", DeploymentConfig.mediaURL,
            "staticMediaURL", DeploymentConfig.staticMediaURL);
    }

    override protected function clientDidLogon (event :ClientEvent) :void
    {
        super.clientDidLogon(event);

        var member :MemberObject = _ctx.getMemberObject();
        if (_featuredPlaceView || member == null) {
            return;
        }

        if (_embedding.hasGWT()) {
            member.addListener(new AvatarUpdateNotifier(_wctx));
        }

        // listen for coins and bars updates
        var statusUpdater :StatusUpdater = new StatusUpdater(this);
        member.addListener(statusUpdater);
        // configure our levels to start
        statusUpdater.initStatus(member.level, member.bars, member.coins, member.newMailCount);

        // listen for theme changes
        var themeUpdater :ThemeUpdater = new ThemeUpdater(this);
        member.addListener(themeUpdater);

        // turn on tutorials for the main web site
        if (getEmbedding().hasGWT() && DeploymentConfig.enableTutorial) {
            new MePageTutorial(_wctx);
            new GeneralTips(_wctx);
        }

        _clientLock = claimLock();
    }

    override protected function clientDidLogoff (event :ClientEvent) :void
    {
        if (_clientLock != null) {
            _clientLock.close();
        }
    }

    protected function claimLock () :LocalConnection
    {
        var lock :LocalConnection = new LocalConnection();
        try {
            lock.connect("com.threerings.msoy.ClientLock");
        } catch (e :*) {
            return null;
        }
        return lock;
    }

    /**
     * Exposed to javascript so that it may notify us to logon.
     */
    protected function externalClientLogon (memberId :int, token :String) :void
    {
        if (token == null) {
            return;
        }

        // if we're logged into the world server or game server already with this id, ignore
        if (memberId == _wctx.getMyId()) {
            return;
        }
        if ((_wctx.getGameDirector().getGameContext() != null) &&
                (_wctx.getGameDirector().getGameContext().getMyId() == memberId)) {
            return;
        }

        log.info("Logging on via external request", "id", memberId, "token", token);
        _wctx.getMsoyController().handleLogon(createStartupCreds(token));
    }

    /**
     * Exposed to javascript so that it may notify us to move to a new location.
     */
    protected function externalClientGo (where :String) :Boolean
    {
        if (_wctx.getClient().isLoggedOn()) {
            log.info("Changing scenes per external request", "where", where);
            _wctx.getWorldController().goToPlace(new URLVariables(where));
            return true;
        } else {
            log.info("Not ready to change scenes (we're not logged on)", "where", where);
            return false;
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
    override protected function configureBridgeFunctions (dispatcher :IEventDispatcher) :void
    {
        super.configureBridgeFunctions(dispatcher);
        dispatcher.addEventListener(UberClientModes.GOT_EXTERNAL_NAME, bridgeGotExternalName);
    }

    /**
     * Called when the embedstub obtains a display name from the external site on which we're
     * embedded. We use this to replace "Guest XXXX" for permaguests with their external site name.
     */
    protected function bridgeGotExternalName (event :Event) :void
    {
        var name :String = event["info"]; // EmbedStub.BridgeEvent.info via security boundary
        log.info("Got external name", "name", name);
        function maybeConfigureGuest () :void {
            if (_wctx.getMemberObject().isPermaguest()) {
                log.info("Using external name", "name", name);
                _wctx.getMemberDirector().setDisplayName(name);
            }
        }
        if (_wctx.getClient().isLoggedOn()) {
            maybeConfigureGuest();
        } else {
            var adapter :ClientAdapter = new ClientAdapter(null, function (event :*) :void {
                _wctx.getClient().removeClientObserver(adapter);
                maybeConfigureGuest();
            });
            _wctx.getClient().addClientObserver(adapter);
        }
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
            log.warning("Error populating context menu", e);
        }
    }

    // from MsoyClient
    override protected function createStartupCreds (token :String) :Credentials
    {
        var params :Object = MsoyParameters.get();
        var creds :WorldCredentials;
        var anonymous :Boolean;

        if ((params["pass"] != null) && (params["user"] != null)) {
            creds = new WorldCredentials(
                new Name(String(params["user"])), MD5.hash(String(params["pass"])));
            anonymous = false;

        } else if (Prefs.getPermaguestUsername() != null) {
            creds = new WorldCredentials(new Name(Prefs.getPermaguestUsername()), "");
            anonymous = false;

        } else {
            creds = new WorldCredentials(null, null);
            anonymous = true;
        }

        creds.sessionToken = (token == null) ? params["token"] : token;
        creds.themeId = params["themeId"];
        creds.ident = Prefs.getMachineIdent();
        creds.featuredPlaceView = _featuredPlaceView;
        creds.visitorId = getVisitorId();
        creds.affiliateId = getAffiliateId();
        creds.vector = getEntryVector();

        // if we're anonymous and in an embed and have no visitor id we need to generate one
        if (creds.sessionToken == null && anonymous && creds.visitorId == null) {
            creds.visitorId = VisitorInfo.createLocalId();
            log.info("Created local visitorId", "visitorId", creds.visitorId);
        }

        return creds;
    }

    protected var _wctx :WorldContext;

    /**
     * A LocalConnection owned by exactly one client across all tabs. We use this to prevent
     * clients opened in new tabs from disconnecting the original client.
     */
    protected var _clientLock :LocalConnection;

    private static const log :Log = Log.getLog(WorldClient);
}
}

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.client.MsoyClient;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.GroupName;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Item_UsedAs;
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
                    Item.AVATAR, (value as Avatar).itemId, Item_UsedAs.AVATAR,
                    _wctx.getMyId());
            }
            value = event.getOldValue();
            if (value is Avatar) {
                _wctx.getMsoyClient().itemUsageChangedToGWT(
                    Item.AVATAR, (value as Avatar).itemId, Item_UsedAs.NOTHING, 0);
            }
        }
    }

    protected var _wctx :WorldContext;
}

class ThemeUpdater implements AttributeChangeListener
{
    public function ThemeUpdater (client :MsoyClient)
    {
        _client = client;
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        if (MemberObject.THEME == event.getName()) {
            updateTheme(event.getValue() as GroupName);
        }
    }

    public function updateTheme (theme :GroupName) :void
    {
        _client.dispatchEventToGWT(THEME_CHANGE_EVENT,
            (theme != null) ? [ theme.getGroupId() ] : [ 0 ]);
    }

    protected var _client :MsoyClient;

    /** Event dispatched to GWT when our theme changes. */
    protected static const THEME_CHANGE_EVENT :String = "themeChange";
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
            updateStatus(STATUS_CHANGE_COINS, event);
            break;

        case MemberObject.BARS:
            updateStatus(STATUS_CHANGE_BARS, event);
            break;

        case MemberObject.LEVEL:
            updateStatus(STATUS_CHANGE_LEVEL, event);
            break;

        case MemberObject.NEW_MAIL_COUNT:
            updateStatus(STATUS_CHANGE_MAIL, event);
            break;
        }
    }

    public function entryAdded (event :EntryAddedEvent) :void
    {
        switch (event.getName()) {
        case MemberObject.FRIENDS:
            var entry :FriendEntry = (event.getEntry() as FriendEntry);
            _client.dispatchEventToGWT(
                FRIEND_EVENT, [FRIEND_ADDED, entry.name.toString(), entry.name.getId()]);
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

    public function initStatus (level :int, bars: int, coins :int, mail :int) :void
    {
        _client.dispatchEventToGWT(STATUS_CHANGE_EVENT, [STATUS_CHANGE_LEVEL, level, 0, true]);
        _client.dispatchEventToGWT(STATUS_CHANGE_EVENT, [STATUS_CHANGE_COINS, coins, 0, true]);
        //_client.dispatchEventToGWT(STATUS_CHANGE_EVENT, [STATUS_CHANGE_BARS, bars, 0, true]);
        _client.dispatchEventToGWT(STATUS_CHANGE_EVENT, [STATUS_CHANGE_MAIL, mail, 0, true]);
    }

    public function updateStatus (field :int, event :AttributeChangedEvent) :void
    {
        _client.dispatchEventToGWT(STATUS_CHANGE_EVENT, [
            field, event.getValue() as int, event.getOldValue() as int, false]);
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
