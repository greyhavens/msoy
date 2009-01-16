//
// $Id$

package com.threerings.msoy.world.client {

import flash.display.BlendMode;
import flash.display.DisplayObject;
import flash.display.DisplayObjectContainer;
import flash.display.SimpleButton;
import flash.display.Sprite;

import flash.events.MouseEvent;

import flash.text.TextField;

import caurina.transitions.Tweener;

import com.threerings.util.Log;
import com.threerings.util.MultiLoader;

import com.threerings.presents.client.BasicDirector;
import com.threerings.presents.client.Client;
import com.threerings.presents.client.ClientAdapter;

import com.threerings.crowd.client.LocationAdapter;
import com.threerings.crowd.data.PlaceObject;

import com.threerings.msoy.client.DeploymentConfig;
import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.PermaguestUtil;
import com.threerings.msoy.client.PlaceBox;
import com.threerings.msoy.data.MemberLocation;
import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;

import com.threerings.msoy.item.data.all.Item;

import com.threerings.msoy.room.data.MsoySceneModel;
import com.threerings.msoy.room.data.PetMarshaller;
import com.threerings.msoy.room.data.RoomConfig;
import com.threerings.msoy.room.data.RoomObject;

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

        _followingNotifier = new FollowingNotifier(_wctx);
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
     * Request to move to the specified member's current scene.
     *
     * Note: presently the member must be a friend.
     */
    public function goToMemberLocation (memberId :int, location :MemberLocation = null) :void
    {
        if (location != null) {
            finishGoToMemberLocation(location);
            return;
        }

        _msvc.getCurrentMemberLocation(_wctx.getClient(), memberId,
            _wctx.resultListener(finishGoToMemberLocation));
    }

    /**
     * Request a change to our avatar.
     *
     * @param newScale a new scale to use, or 0 to retain the avatar's last scale.
     */
    public function setAvatar (avatarId :int, newScale :Number = 0) :void
    {
        _msvc.setAvatar(_wctx.getClient(), avatarId, newScale, _wctx.confirmListener());
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
        _msvc.getHomeId(_wctx.getClient(), ownerType, ownerId,
            _wctx.resultListener(_wctx.getSceneDirector().moveTo));
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
                _wctx.getGameDirector().joinPlayer(location.gameId, location.memberId);
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

        } else if (place is RoomObject && !_wctx.getGameDirector().isGaming()) {
            var member :MemberObject = _wctx.getMemberObject();
            if (PermaguestUtil.isPermaguestEmail(member.username.toString()) &&
                member.memberName.toString() == PermaguestUtil.DISPLAY_NAME) {
                new GuestNameDialog(_wctx).open();

            } else {
                maybeDisplayAvatarIntro();
            }
        }
    }

    // from BasicDirector
    override protected function clientObjectUpdated (client :Client) :void
    {
        super.clientObjectUpdated(client);
        client.getClientObject().addListener(_followingNotifier);
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

        // TODO: move the functions we use into a WorldService
        _msvc = (client.requireService(MemberService) as MemberService);
    }

    /**
     * This has nowhere else good to live.
     */
    protected function maybeDisplayAvatarIntro () :void
    {
        // if we have already shown the intro, they are a guest, are not wearing the tofu avatar,
        // or have ever worn any non-tofu avatar, don't show the avatar intro
        var mobj :MemberObject = _wctx.getMemberObject();
        if (_avatarIntro != null || mobj.isGuest() || mobj.avatar != null ||
                mobj.avatarCache.size() > 0) {
            return;
        }

        MultiLoader.getContents(DeploymentConfig.serverURL + "rsrc/avatar_intro.swf",
            function (result :DisplayObjectContainer) :void {
            _avatarIntro = result;
            _avatarIntro.x = 15;

            var title :TextField = (_avatarIntro.getChildByName("txt_welcome") as TextField);
            title.text = Msgs.GENERAL.get("t.avatar_intro");

            var info :TextField = (_avatarIntro.getChildByName("txt_description") as TextField);
            info.text = Msgs.GENERAL.get("m.avatar_intro");

            var fadeOut :Function = function (event :MouseEvent) :void {
                Tweener.addTween(_avatarIntro, { alpha: 0, time: .75, transition: "linear",
                    onComplete: function () :void {
                        _wctx.getTopPanel().getPlaceContainer().removeOverlay(_avatarIntro);
                        // gc the intro, but suppress further popups this session
                        _avatarIntro = new Sprite();
                    } });
            };

            var close :SimpleButton = (_avatarIntro.getChildByName("btn_nothanks") as SimpleButton);
            close.addEventListener(MouseEvent.CLICK, fadeOut);

            var go :SimpleButton = (_avatarIntro.getChildByName("btn_gotoshop") as SimpleButton);
            go.addEventListener(MouseEvent.CLICK, function (event :MouseEvent) :void {
                _wctx.getWorldController().handleViewShop(Item.AVATAR);
            });
            go.addEventListener(MouseEvent.CLICK, fadeOut);

            _avatarIntro.alpha = 0;
            // make the text get the alpha setting too (contrary to flash documentation)
            _avatarIntro.blendMode = BlendMode.LAYER;
            _wctx.getTopPanel().getPlaceContainer().addOverlay(
                _avatarIntro, PlaceBox.LAYER_TRANSIENT);
            Tweener.addTween(_avatarIntro, { alpha: 1, time: .75, transition: "linear" });
        });
    }

    protected var _wctx :WorldContext;
    protected var _msvc :MemberService;

    protected var _followingNotifier :FollowingNotifier;

    /** If non-null, we should call it when we change places. */
    protected var _goToGame :Function;

    /** An introduction to avatars shown to brand new players. */
    protected var _avatarIntro :DisplayObjectContainer;
}
}

import com.threerings.util.MessageBundle;

import com.threerings.presents.dobj.AttributeChangeListener;
import com.threerings.presents.dobj.AttributeChangedEvent;
import com.threerings.presents.dobj.DSet;
import com.threerings.presents.dobj.EntryAddedEvent;
import com.threerings.presents.dobj.EntryRemovedEvent;
import com.threerings.presents.dobj.EntryUpdatedEvent;
import com.threerings.presents.dobj.SetListener;

import com.threerings.msoy.data.MemberObject;
import com.threerings.msoy.data.MsoyCodes;
import com.threerings.msoy.data.all.MemberName;

import com.threerings.msoy.world.client.WorldContext;

class FollowingNotifier
    implements AttributeChangeListener, SetListener
{
    public function FollowingNotifier (wctx :WorldContext)
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
        }
    }

    public function entryAdded (event :EntryAddedEvent) :void
    {
        if (MemberObject.FOLLOWERS == event.getName()) {
            _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS,
                MessageBundle.tcompose("m.new_follower", event.getEntry() as MemberName));
        }
    }

    public function entryUpdated (event :EntryUpdatedEvent) :void
    {
        // everybody noops
    }

    public function entryRemoved (event :EntryRemovedEvent) :void
    {
        if (MemberObject.FOLLOWERS == event.getName()) {
            _wctx.displayFeedback(MsoyCodes.GENERAL_MSGS,
                MessageBundle.tcompose("m.follower_ditched", event.getOldEntry() as MemberName));
        }
    }

    protected var _wctx :WorldContext;
}
