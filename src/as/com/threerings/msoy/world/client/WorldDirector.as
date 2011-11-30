//
// $Id$

package com.threerings.msoy.world.client {

import com.threerings.io.TypedArray;

import com.threerings.util.Log;
import com.threerings.util.Util;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.orth.room.data.PetMarshaller;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.item.data.all.Avatar;
import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.RoomConfig;
import com.threerings.msoy.room.data.RoomObject;
import com.threerings.msoy.tutorial.client.TutorialDirector;
import com.threerings.msoy.tutorial.client.TutorialSequenceBuilder;
import com.threerings.msoy.world.client.WorldClient;

/**
 * Handles moving around in the virtual world.
 */
public class WorldDirector extends BasicDirector
{
    public const log :Log = Log.getLog(this);

    // statically reference classes we require
    PetMarshaller;
    RoomConfig;

    public function WorldDirector (ctx :WorldContext)
    {
        super(ctx);
        _wctx = ctx;
        _wctx.getLocationDirector().addLocationObserver(
            new LocationAdapter(null, locationDidChange, null));

        _memberNotifier = new MemberNotifier(_wctx);
    }

    /**
     * Request to move to the specified member's home.
     */
    public function goToMemberHome (memberId :int) :void
    {
        goToHome(MsoySceneModel.OWNER_TYPE_MEMBER, memberId);
    }

    /**
     * Request to move to the specified group's home.
     */
    public function goToGroupHome (groupId :int) :void
    {
        goToHome(MsoySceneModel.OWNER_TYPE_GROUP, groupId);
    }

    /**
     * Request to move to the specified member's current location (game or scene).
     *
     * Note: presently the member must be a friend.
     */
    public function goToMemberLocation (memberId :int) :void
    {
        _msvc.getCurrentMemberLocation(
            memberId, _wctx.resultListener(finishGoToMemberLocation));
    }

    /**
     * Request a change to our avatar.
     *
     * @param newScale a new scale to use, or 0 to retain the avatar's last scale.
     */
    public function setAvatar (avatarId :int) :void
    {
        _wsvc.setAvatar(avatarId, _wctx.confirmListener());
    }

    /**
     * Fire up the selection UI with the given array of avatars and call the callback
     * when the user makes their choice, passing the chosen avatar.
     */
    public function selectAvatar (avatars :Array, groupName :String, finish :Function) :void
    {
        // TODO: pass in the theme name?
        var tip :String = groupName == null ? Msgs.WORLD.get("m.pick_avatar") :
            Msgs.WORLD.get("m.pick_avatar_theme", groupName);

        // TODO: get rid of the "Connecting..." that stays behind the picker
        // TODO: we need to cancel the popup if a new room is selected
        AvatarPickerPanel.show(_wctx, avatars, tip, function giftSelected (avatar :Avatar) :void {
            log.info("Avatar selected, accepting gift", "name", avatar.name);
            _wsvc.acceptAndProceed(avatar.catalogId, _wctx.confirmListener(finish));
        });
    }

    /**
     * Notify the server that we've finished the tutorial.
     */
    public function completeDjTutorial () :void
    {
        _wsvc.completeDjTutorial(_wctx.listener());
    }

    /**
     * Request to go to the home of the specified entity.
     */
    protected function goToHome (ownerType :int, ownerId :int) :void
    {
        if (!_wctx.getClient().isLoggedOn()) {
            log.info("Delaying goToHome, not online [type=" + ownerType + ", id=" + ownerId + "].");
            var waiter :ClientAdapter = new ClientAdapter(null, function (event :*) :void {
                _wctx.getClient().removeClientObserver(waiter);
                goToHome(ownerType, ownerId);
            });
            _wctx.getClient().addClientObserver(waiter);
            return;
        }
        function selectGift (avatars :TypedArray, homeSceneId :int) :void {
            selectAvatar(avatars, null, function () :void {
                _wctx.getSceneDirector().moveTo(homeSceneId);
            });
        }
        _wsvc.getHomeId(ownerType, ownerId, new WorldService_HomeResultListenerAdapter(
            _wctx.getSceneDirector().moveTo, selectGift,
            Util.adapt(_wctx.displayFeedback, Msgs.GENERAL)));
    }

    /**
     * Called by {@link #goToMemberLocation}.
     */
    protected function finishGoToMemberLocation (location :MemberLocation) :void
    {
        var goToGame :Function = function () :void {};
        // TODO: Do something more interesting for AVR Games.
        if (!location.avrGame && location.gameId != 0) {
            goToGame = function () :void {
                _wctx.getGameDirector().playNow(location.gameId, location.memberId);
            };
        }

        var sceneId :int = location.sceneId;
        if (sceneId == 0 && _wctx.getSceneDirector().getScene() == null) {
            // if we're not in a scene and they're not in a scene, go home.  If they're in an
            // unwatchable game, we'll get an error in the lobby, and this way we'll at least be in
            // a scene as well
            sceneId = _wctx.getMemberObject().getHomeSceneId();
        }

        if (sceneId == 0) {
            goToGame(); // we're not moving, so take our game action immediately
            return;
        }

        // otherwise we have to do things the hard way
        _goToGame = goToGame;
        _wctx.getWorldController().handleGoScene(location.sceneId);
    }

    /**
     * Adapted as a LocationObserver method.
     */
    protected function locationDidChange (place :PlaceObject) :void
    {
        if (place == null) {
            _wctx.clearPlaceView(null);
        }

        if (_goToGame != null) {
            var fn :Function = _goToGame;
            _goToGame = null;
            fn();

        } else if (place is RoomObject && !_wctx.getGameDirector().isGaming() &&
                   !_wctx.getMsoyClient().isEmbedded()) {
            maybeDisplayRoomTutorial();
        }
    }

    // from BasicDirector
    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);
        WorldClient(client).bodyOf().addListener(_memberNotifier);
    }

    // from BasicDirector
    override protected function registerServices (client :Client) :void
    {
        client.addServiceGroup(MsoyCodes.WORLD_GROUP);
    }

    // from BasicDirector
    override protected function fetchServices (client :Client) :void
    {
        super.fetchServices(client);

        // TODO: move more of the functions we use into a WorldService
        _msvc = (client.requireService(MemberService) as MemberService);
        _wsvc = (client.requireService(WorldService) as WorldService);
    }

    protected function maybeDisplayRoomTutorial () :void
    {
        // skip all of this until tutorial switch is flipped
        if (!DeploymentConfig.enableTutorial) {
            return;
        }

        var homeId :int = _wctx.getMemberObject().homeSceneId;

        // checks if the logged in user is in their home room
        function isHome () :Boolean {
            return homeId != 0 && _wctx.getSceneDirector().getScene() != null &&
                _wctx.getSceneDirector().getScene().getId() == homeId;
        }

        // stash the director in a short variable
        var tut :TutorialDirector = _wctx.getTutorialDirector();

        // translates a tutorial string
        function xlate (msg :String) :String {
            return Msgs.NPC.get(msg);
        }

        // omg, we're not in Kansas anymore... show a suggestion for getting back home
        if (!isHome()) {
            if (homeId != 0) {
                tut.newSuggestion("leftHome", xlate("i.noob_left_home")).newbie()
                    .button(xlate("b.noob_left_home"), Util.adapt(
                        _wctx.getSceneDirector().moveTo, homeId))
                    .queue();
            }
            return;
        }

        // the new user sequence...
        var sequence :TutorialSequenceBuilder = tut.newSequence("newUser").newbie().limit(isHome);

        // welcome
        sequence.newSuggestion(xlate("i.noob_welcome"))
            .button(xlate("b.noob_welcome"), null).buttonCloses(true).queue();

        // decorate
        sequence.newSuggestion(xlate("i.noob_decorate"))
            .button(xlate("b.noob_decorate"), _wctx.getWorldController().handleRoomEdit)
            .menuItemHighlight(_wctx.getWorldControlBar().roomBtn, WorldController.ROOM_EDIT)
            .buttonCloses().queue();

        // activate it
        sequence.activate();
    }

    protected var _wctx :WorldContext;
    protected var _wsvc :WorldService;
    protected var _msvc :MemberService;

    protected var _memberNotifier :MemberNotifier;

    /** If non-null, we should call it when we change places. */
    protected var _goToGame :Function;
}
}

import com.threerings.util.MessageBundle;
import com.threerings.util.Set;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.FriendEntry;
import com.threerings.msoy.data.all.MemberName;
import com.threerings.msoy.item.data.all.Item;
import com.threerings.msoy.item.data.all.Item_UsedAs;
import com.threerings.msoy.room.client.RoomView;
import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.Track;
import com.threerings.msoy.world.client.WorldContext;

class MemberNotifier
    implements AttributeChangeListener, SetListener
{
    public function MemberNotifier (wctx :WorldContext)
    {
        _wctx = wctx;
    }

    public function attributeChanged (event :AttributeChangedEvent) :void
    {
        switch (event.getName()) {
        case MemberObject.FOLLOWING:
            var leader :MemberName = event.getValue() as MemberName;
            if (leader != null) {
                _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS,
                    MessageBundle.tcompose("m.following", leader));
            } else if (event.getOldValue() != null) {
                _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS,
                    MessageBundle.tcompose("m.not_following", event.getOldValue()));
            }
            break;

        case MemberObject.FOLLOWERS:
            var followers :DSet = event.getValue() as DSet;
            if (followers.size() == 0) {
                _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS, "m.follows_cleared");
            }
            break;

        case MemberObject.TRACKS:
            // Assume that this update cleared the set
            var tracks :DSet = event.getOldValue() as DSet;
            for each (var track :Track in tracks.toArray()) {
                _wctx.getMsoyClient().itemUsageChangedToGWT(
                    Item.AUDIO, track.audio.itemId, Item_UsedAs.NOTHING, 0);
            }
            break;

        case MemberObject.FRIENDS:
            var roomView :RoomView = _wctx.getPlaceView() as RoomView;
            if (roomView != null) {
                // Unsquelch the new friends, don't bother squelching the old friends DSet value
                for each (var friend :FriendEntry in DSet(event.getValue()).toArray()) {
                    roomView.getRoomController().squelchPlayer(friend.name, false);
                }
            }
            break;
        }
    }

    public function entryAdded (event :EntryAddedEvent) :void
    {
        switch (event.getName()) {
        case MemberObject.FOLLOWERS:
            _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS,
                MessageBundle.tcompose("m.new_follower", event.getEntry() as MemberName));
            break;

        case MemberObject.TRACKS:
            var scene :MsoyScene = _wctx.getSceneDirector().getScene() as MsoyScene;
            if (scene != null) {
                var track :Track = event.getEntry() as Track;
                _wctx.getMsoyClient().itemUsageChangedToGWT(
                    Item.AUDIO, track.audio.itemId, Item_UsedAs.BACKGROUND, scene.getId());
            }
            break;

        case MemberObject.FRIENDS:
            var roomView :RoomView = _wctx.getPlaceView() as RoomView;
            if (roomView != null) {
                roomView.getRoomController().squelchPlayer(
                    FriendEntry(event.getEntry()).name, false);
            }
            break;
        }
    }

    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        // everybody noops
    }

    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        switch (event.getName()) {
        case MemberObject.FOLLOWERS:
            _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS,
                MessageBundle.tcompose("m.follower_ditched", event.getOldEntry() as MemberName));
            break;

        case MemberObject.TRACKS:
            var track :Track = event.getOldEntry() as Track;
            _wctx.getMsoyClient().itemUsageChangedToGWT(
                Item.AUDIO, track.audio.itemId, Item_UsedAs.NOTHING, 0);
            break;

        case MemberObject.FRIENDS:
            var roomView :RoomView = _wctx.getPlaceView() as RoomView;
            if (roomView != null) {
                roomView.getRoomController().squelchPlayer(
                    FriendEntry(event.getOldEntry()).name, true);
            }
            break;
        }
    }

    protected var _wctx :WorldContext;
}
