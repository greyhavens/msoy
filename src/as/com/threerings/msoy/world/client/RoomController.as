package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.geom.Rectangle;
import flash.ui.ContextMenuItem;
import flash.ui.Keyboard;
import flash.utils.ByteArray;

import mx.core.IToolTip;
import mx.core.UIComponent;
import mx.managers.ToolTipManager;

import com.threerings.util.Integer;
import com.threerings.util.NetUtil;

import com.threerings.io.TypedArray;

import com.threerings.flash.MenuUtil;

import com.threerings.flex.CommandMenu;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.presents.dobj.ChangeListener;
import com.threerings.presents.dobj.MessageAdapter;
import com.threerings.presents.dobj.MessageEvent;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.ezgame.util.EZObjectMarshaller;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.WorldContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.item.web.ItemIdent;
import com.threerings.msoy.web.data.MemberName;

import com.threerings.msoy.world.client.editor.EditorController;

import com.threerings.msoy.world.data.EntityControl;
import com.threerings.msoy.world.data.FurniData;
import com.threerings.msoy.world.data.MemoryEntry;
import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomObject;
import com.threerings.msoy.world.data.WorldOccupantInfo;

import com.threerings.msoy.chat.client.ReportingListener;

public class RoomController extends SceneController
{
    private const log :Log = Log.getLog(RoomController);

    public static const EDIT_SCENE :String = "EditScene";

    public static const FURNI_CLICKED :String = "FurniClicked";
    public static const AVATAR_CLICKED :String = "AvatarClicked";

    /**
     * Get the instanceId of all the entity instances in the room.
     * This is used so that two instances of a pet can negotiate which
     * client will control it, for example.
     */
    public function getEntityInstanceId () :int
    {
        // every sprite uses our own OID as the instanceid.
        return _mctx.getMemberObject().getOid();
    }

    /**
     * Requests that this client be given control of the specified item.
     */
    public function requestControl (ident :ItemIdent) :void
    {
        if (_roomObj == null) {
            log.warning("Cannot request entity control, no room object [ident=" + ident + "].");
        } else {
            _roomObj.roomService.requestControl(_mctx.getClient(), ident);
        }
    }

    /**
     * Handles a request by an item in our room to send an "action" (requires control) or
     * a "message" (doesn't require control).
     */
    public function sendSpriteMessage (
        ident :ItemIdent, name :String, arg :Object, isAction :Boolean) :void
    {
        if (isAction && !checkCanRequest(ident, "triggerAction")) {
            return;
        }

        // send the request off to the server
        var data :ByteArray = (EZObjectMarshaller.encode(arg, false) as ByteArray);
        _roomObj.roomService.sendSpriteMessage(_mctx.getClient(), ident, name, data, isAction);
    }

    /**
     * Handles a request by an actor item to change its persistent state.
     * Requires control.
     */
    public function setActorState (ident :ItemIdent, actorOid :int, state :String) :void
    {
        if (!checkCanRequest(ident, "setState")) {
            return;
        }

        _roomObj.roomService.setActorState(_mctx.getClient(), ident, actorOid, state);
    }

    /**
     * Handles a request by an item in our room to update its memory.
     */
    public function updateMemory (ident :ItemIdent, key :String, value: Object) :Boolean
    {
        if (!checkCanRequest(ident, "updateMemory")) {
            return false;
        }

        // serialize datum (TODO: move this to somewhere more general purpose)
        var data :ByteArray = (EZObjectMarshaller.encode(value, false) as ByteArray);

        // TODO: total up item's used memory, ensure it doesn't exceed the allowed limit

        // ship the update request off to the server
        _roomObj.roomService.updateMemory(_mctx.getClient(), new MemoryEntry(ident, key, data));
        return true;
    }

    /**
     * Handles a request by an actor to change its location. Returns true if the request was
     * dispatched, false if funny business prevented it.
     */
    public function requestMove (ident :ItemIdent, newloc :MsoyLocation) :Boolean
    {
        if (!checkCanRequest(ident, "requestMove")) {
            return false;
        }
        _roomObj.roomService.changeLocation(_mctx.getClient(), ident, newloc);
        return true;
    }

    // documentation inherited
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        _mctx = (ctx as WorldContext);
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _roomView = new RoomView(ctx as WorldContext, this);
        return _roomView;
    }

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomObj = (plobj as RoomObject);
        _roomListener = new MessageAdapter(msgReceivedOnRoomObj);
        _roomObj.addListener(_roomListener);

        // get a copy of the scene
        _scene = (_mctx.getSceneDirector().getScene() as MsoyScene);

        _walkTarget.visible = false;
        _roomView.addChild(_walkTarget);

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(Event.ENTER_FRAME, checkMouse);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);
    }

    // documentation inherited
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(Event.ENTER_FRAME, checkMouse);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.removeChild(_walkTarget);
        setHoverSprite(null);

        _roomObj.removeListener(_roomListener);

        _scene = null;
        _roomObj = null;

        closeAllMusic();

        super.didLeavePlace(plobj);
    }

    /**
     * Close and reset all music.
     */
    protected function closeAllMusic () :void
    {
        if (_music != null) {
            _music.close();
            _music = null;
            _musicIsBackground = true;
        }
        if (_loadingMusic != null) {
            _loadingMusic.close();
            _loadingMusic = null;
        }
    }

    /**
     * Returns true if this scene is currently being edited.
     */
    public function get isCurrentlyEditing () :Boolean
    {
        return (_editor != null);
    }

    /**
     * Exit editing mode.
     */
    public function endEditing (edits :TypedArray) :void
    {
        _editor = null;

        // turn editing off
        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(Event.ENTER_FRAME, checkMouse);

        // possibly save the edits
        if (edits != null) {
            _roomObj.roomService.updateRoom(_mctx.getClient(), edits,
                new ReportingListener(_mctx));
        }

        // re-start any music
        if (_music != null) {
            _music.play();
        }
    }

    /**
     * Handles EDIT_SCENE.
     */
    public function handleEditScene () :void
    {
        if (isCurrentlyEditing) {
            return; // handled: we're editing
        }
        _roomObj.roomService.editRoom(_mctx.getClient(), new ResultWrapper(
            function (cause :String) :void {
                _mctx.displayFeedback("general", cause);
            },
            function (result :Object) :void {
                startEditing(result as Array);
            }));
    }

    /**
     * Handles FURNI_CLICKED.
     */
    public function handleFurniClicked (furni :FurniData) :void
    {
        switch (furni.actionType) {
        case FurniData.ACTION_URL:
            NetUtil.navigateToURL(furni.actionData);
            return;

        case FurniData.ACTION_LOBBY_GAME:
        case FurniData.ACTION_WORLD_GAME:
            var actionData :Array = furni.splitActionData();
            var gameId :int = int(actionData[0]);
            postAction(furni.actionType == FurniData.ACTION_LOBBY_GAME ?
                MsoyController.GO_GAME_LOBBY : MsoyController.JOIN_WORLD_GAME, gameId);
            return;
            
        case FurniData.ACTION_PORTAL:
            _mctx.getSpotSceneDirector().traversePortal(furni.id);
            return;

        default:
            log.warning("Clicked on unhandled furni action type " +
                "[actionType=" + furni.actionType +
                ", actionData=" + furni.actionData + "].");
            return;
        }
    }

    /**
     * Handles AVATAR_CLICKED.
     */
    public function handleAvatarClicked (avatar :AvatarSprite) :void
    {
        var occInfo :MemberInfo = (avatar.getActorInfo() as MemberInfo);
        if (occInfo == null) {
            log.info("Clicked on non-MemberInfo sprite " +
                "[info=" + avatar.getActorInfo() + "].");
            return;
        }

        var us :MemberObject = _mctx.getMemberObject();
        var menuItems :Array = [];
        if (occInfo.bodyOid == us.getOid()) {
            if (_mctx.worldProps.userControlsAvatar) {
                // create a menu for clicking on ourselves
                var actions :Array = avatar.getAvatarActions();
                if (actions.length > 0) {
                    var worldActions :Array = [];
                    for each (var act :String in actions) {
                        worldActions.push({ label: act,
                            callback: doAvatarAction, arg: act });
                    }

                    menuItems.push({ label: Msgs.GENERAL.get("l.avAction"),
                        children: worldActions });
                }

                var states :Array = avatar.getAvatarStates();
                if (states.length > 0) {
                    var worldStates :Array = [];
                    for each (var state :String in states) {
                        worldStates.push({ label: state,
                            callback: doAvatarState, arg :state });
                    }

                    menuItems.push({ label: Msgs.GENERAL.get("l.avStates"),
                        children: worldStates });
                }
            }

        } else {
            // create a menu for clicking on someone else
            var memId :int = occInfo.getMemberId();
            var isGuest :Boolean = (memId == MemberName.GUEST_ID);
            var isFriend :Boolean = us.friends.containsKey(memId);
//            menuItems.push({ label: Msgs.GENERAL.get("b.tell"),
//                command: MsoyController.TELL, arg: memId });

            if (!isGuest) {
                menuItems.push(
                    { label: Msgs.GENERAL.get("b.visit_home"),
                      command: MsoyController.GO_MEMBER_HOME,
                      arg: memId },
                    { label: Msgs.GENERAL.get("b.view_member"),
                      command: MsoyController.VIEW_MEMBER,
                      arg: memId },
                    { label: Msgs.GENERAL.get(isFriend ? "b.removeAsFriend"
                                                       : "b.addAsFriend"),
                      command: MsoyController.ALTER_FRIEND,
                      arg: [memId, !isFriend] });
            }
        }

        // pop up the menu where the mouse is
        if (menuItems.length > 0) {
            CommandMenu.createMenu(menuItems).show();
        }
    }

    /**
     * Get the top-most sprite mouse-capturing sprite with a non-transparent
     * pixel at the specified location.
     */
    public function getHitSprite (
        stageX :Number, stageY :Number, all :Boolean = false) :MsoySprite
    {
        if (!_roomView.getGlobalBounds().contains(stageX, stageY)) {
            // no hits possible, mouse is out-of-bounds
            return null;
        }

        // we search from last-drawn to first drawn to get the topmost...
        for (var dex :int = _roomView.numChildren - 1; dex >= 0; dex--) {
            var spr :MsoySprite = (_roomView.getChildAt(dex) as MsoySprite);
            if ((spr != null) && (all || (spr.isActive() && spr.capturesMouse())) &&
                    spr.hitTestPoint(stageX, stageY, true)) {
                return spr;
            }
        }

        return null;
    }

    /**
     * Add the specified command to the context menu for the current scene.
     */
    protected function createMenuItem (
        cmd :String, arg :Object = null, separatorBefore :Boolean = false,
        enabled :Boolean = true, visible :Boolean = true) :ContextMenuItem
    {
        var menuText :String = Msgs.GENERAL.get("b." + cmd);
        return MenuUtil.createControllerMenuItem(menuText, cmd, arg,
                separatorBefore, enabled, visible);
    }

    /**
     * Begin editing the scene.
     */
    protected function startEditing (items :Array) :void
    {
        // set up editing
        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(Event.ENTER_FRAME, checkMouse);
        _walkTarget.visible = false;

        if (_music != null) {
            if (_musicIsBackground) {
                _music.stop();

            } else {
                _music.close();
                _music = null;
                _musicIsBackground = true;
            }
        }

        _editor = new EditorController(_mctx, this, _roomView, _scene, items);
    }

    /**
     * Handle ENTER_FRAME and see if the mouse is now over anything.
     * Normally the flash player will dispatch mouseOver/mouseLeft
     * for an object even if the mouse isn't moving: the sprite could move.
     * Since we're hacking in our own mouseOver handling, we emulate that.
     * Gah.
     */
    protected function checkMouse (event :Event) :void
    {
        var sx :Number = _roomView.stage.mouseX;
        var sy :Number = _roomView.stage.mouseY;
        var showWalkTarget :Boolean = false;

        var hitter :MsoySprite = getHitSprite(sx, sy);
        if (hitter == null) {
            var cloc :ClickLocation = _roomView.pointToLocation(sx, sy);
            if (cloc.click == ClickLocation.FLOOR && _mctx.worldProps.userControlsAvatar) {
                _walkTarget.x = _roomView.mouseX - _walkTarget.width/2;
                _walkTarget.y = _roomView.mouseY - _walkTarget.height/2;
                _walkTarget.scaleX = 1 / _roomView.scaleX;
                _walkTarget.scaleY = 1 / _roomView.scaleY;
                showWalkTarget = true;
            }

        } else if (!hitter.hasAction()) {
            // it may have captured the mouse, but it doesn't actually
            // have any action, so we don't hover it.
            hitter = null;
        }

        _walkTarget.visible = showWalkTarget;

        setHoverSprite(hitter, sx, sy);
    }

    /**
     * Set the sprite that the mouse is hovering over.
     */
    protected function setHoverSprite (
        sprite :MsoySprite, stageX :Number = 0, stageY :Number = 0) :void
    {
        if (_hoverSprite != sprite) {
            if (_hoverSprite != null) {
                _hoverSprite.setGlow(false);
                _hoverSprite = null;
                if (_hoverTip != null) {
                    ToolTipManager.destroyToolTip(_hoverTip);
                    _hoverTip = null;
                }
            }

            if (sprite != null) {
                sprite.setGlow(true);
                _hoverSprite = sprite;
                var tipText :String = sprite.getToolTipText();
                if (tipText != null) {
                    _hoverTip = ToolTipManager.createToolTip(tipText,
                        stageX, stageY);
                    var tipComp :UIComponent = UIComponent(_hoverTip);
                    tipComp.styleName = "roomToolTip";
                    var hoverColor :uint = sprite.getHoverColor();
                    tipComp.setStyle("color", hoverColor);
                    if (hoverColor == 0) {
                        tipComp.setStyle("backgroundColor", 0xFFFFFF);
                    }
                }
            }
        }
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        // if the shift key is down, we're not interested in what the sprite
        // represents, we're intrested in the sprite itself.
        // TODO: Oh fer chrissakes. Shift/Ctrl don't seem to be detected
        // on windows!!!
        var isItemContext :Boolean = event.shiftKey;

        var hitter :MsoySprite = getHitSprite(event.stageX, event.stageY, isItemContext);

        if (hitter != null) {
            if (isItemContext) {
                showItemMenu(hitter);

            } else if (hitter.hasAction()) {
                hitter.mouseClick(event);
            }
            // otherwise: the sprite simply captures and discards the event

        } else if (!isItemContext && _mctx.worldProps.userControlsAvatar) {
            var curLoc :MsoyLocation = _roomView.getMyCurrentLocation();
            if (curLoc == null) {
                return; // we've already left, ignore the click
            }

            // calculate where the location is
            var cloc :ClickLocation = _roomView.pointToLocation(
                event.stageX, event.stageY);
            if (cloc.click == ClickLocation.FLOOR) {
                // orient the location as appropriate
                var newLoc :MsoyLocation = cloc.loc;
                var degrees :Number = 180 / Math.PI *
                    Math.atan2(newLoc.z - curLoc.z, newLoc.x - curLoc.x);
                // we rotate so that 0 faces forward
                newLoc.orient = (degrees + 90 + 360) % 360;
                _mctx.getSpotSceneDirector().changeLocation(newLoc, null);
            }
        }
    }

    /**
     * Show the menu containing item-specific options.
     */
    protected function showItemMenu (sprite :MsoySprite) :void
    {
        var menuItems :Array = [];

        var ident :ItemIdent = sprite.getItemIdent();
        if (ident != null) {
            menuItems.push(
                { label: Msgs.GENERAL.get("b.view_item"),
                  command: MsoyController.VIEW_ITEM,
                  arg: ident });
        }
        // TODO: more..?

        // pop up the menu where the mouse is
        if (menuItems.length > 0) {
            CommandMenu.createMenu(menuItems).show();
        }
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        try {
            var keyDown :Boolean = event.type == KeyboardEvent.KEY_DOWN;
            switch (event.keyCode) {
            case Keyboard.F4:
                _roomView.dimAvatars(keyDown);
                return;

            case Keyboard.F5:
                _roomView.dimFurni(keyDown);
                return;

            case Keyboard.F6:
                _roomView.chatOverlay.setClickableGlyphs(keyDown);
                return;
            }

            if (keyDown) {
                switch (event.charCode) {
                case 91: // '['
                    _roomView.scrollViewBy(-ROOM_SCROLL_INCREMENT);
                    break;

                case 93: // ']'
                    _roomView.scrollViewBy(ROOM_SCROLL_INCREMENT);
                    break;
                }
            }

        } finally {
            event.updateAfterEvent();
        }
    }

    /**
     * Called when a message is received on the room object.
     */
    protected function msgReceivedOnRoomObj (event :MessageEvent) :void
    {
        var args :Array = event.getArgs();
        switch (event.getName()) {
        case RoomObject.LOAD_MUSIC:
            if (_loadingMusic != null) {
                _loadingMusic.close();
            }
            _loadingMusic = new SoundPlayer(String(args[0]));
            // TODO: dispatched MUSIC_LOADED back...
            break;

        case RoomObject.PLAY_MUSIC:
            if (args == null || args.length == 0) {
                closeAllMusic();
                break;
            }
            var url :String = (args[0] as String);
            if (_loadingMusic != null) {
                if (_loadingMusic.getURL() == url) {
                    // awesome
                    if (_music != null) {
                        _music.close();
                    }
                    _music = _loadingMusic;
                    _loadingMusic = null;
                    _musicIsBackground = false;
                    _music.play();

                } else {
                    log.warning("Asked to play music different from loaded? " +
                        "[loaded=" + _loadingMusic.getURL() +
                        ", toPlay=" + url + "].");
                }
            }
            break;
        }
    }

    public function setBackgroundMusic (music :FurniData) :void
    {
        if (!_musicIsBackground) {
            if (_music.isPlaying()) {
                // don't disrupt the other music..
                return;

            } else {
                // oh, this other music is done. Sure, let's go for
                // the background music again
                _music.close();
                _music = null;
                _musicIsBackground = true;
            }
        }

        var path :String = music.media.getMediaPath();
        // maybe shutdown old music
        // if _music is playing the right thing, let it keep on playing
        if (_music != null && _music.getURL() != path) {
            _music.close();
            _music = null;
        }
        // set up new music, if needed
        if (_music == null) {
            _music = new SoundPlayer(path);
            // TODO: we probably need to wait for COMPLETE
            _music.play();
            //var pos :Number = Prefs.getMediaPosition(music.getMediaId());
            //_music.loop(pos);
            // NOTE: the position argument has been disabled because
            // it causes the flash player to crash, and also seems to booch
            // proper looping.
        }
        // set the volume, even if we're just re-setting it on
        // already-playing music
        _music.setVolume(Number(music.actionData));
    }

    /**
     * Ensures that we can issue a request to update the distributed state of the specified item,
     * returning true if so, false if we don't yet have a room object or are not in control of that
     * item.
     */
    protected function checkCanRequest (ident :ItemIdent, from :String) :Boolean
    {
        if (_roomObj == null) {
            log.warning("Cannot issue request for lack of room object [from=" + from +
                        ", ident=" + ident + "].");
            return false;
        }

        // make sure we are in control of this entity (or that no one has control)
        if (hasEntityControl(ident, true)) {
            return true;
        }

        log.info("Dropping request as we are not controller [from=" + from +
                 ", item=" + ident + "].");
        return false;
    }

    /**
     * Does this client have control over the specified entity?
     *
     * @param orNobody if true, claim that we have entity control when nobody
     * yet has control.
     */
    protected function hasEntityControl (
        ident :ItemIdent, orNobody :Boolean = false) :Boolean
    {
        var ctrl :EntityControl = (_roomObj.controllers.get(ident) as EntityControl);
        if (ctrl == null) {
            return orNobody;
        }
        return (ctrl.controllerOid == _mctx.getMemberObject().getOid());
    }

    override protected function sceneUpdated (update :SceneUpdate) :void
    {
        super.sceneUpdated(update);
        _roomView.processUpdate(update);
    }

    /**
     * Called to enact the avatar action globally: all users will see it.
     */
    protected function doAvatarAction (action :String) :void
    {
        sendSpriteMessage(_roomView.getMyAvatar().getItemIdent(),
            action, null, true);
    }

    /**
     * Called to enact an avatar state change.
     */
    protected function doAvatarState (state :String) :void
    {
        var avatar :AvatarSprite = _roomView.getMyAvatar();
        setActorState(avatar.getItemIdent(), avatar.getOid(), state);
    }

    /** The life-force of the client. */
    protected var _mctx :WorldContext;

    /** The room view that we're controlling. */
    protected var _roomView :RoomView;

    protected var _roomObj :RoomObject;

    /** Our general-purpose room listener. */
    protected var _roomListener :ChangeListener;

    protected var _hoverSprite :MsoySprite;

    protected var _hoverTip :IToolTip;

    /** The music currently playing in the scene, which may or may not be
     * background music. */
    protected var _music :SoundPlayer;

    /** True if _music is the room's background music. Otherwise
     * The music playing is from some other source. */
    protected var _musicIsBackground :Boolean = true;

    /** Holds loading alternate music. Once triggered to play,
     * it's shifted to _music. */
    protected var _loadingMusic :SoundPlayer;

    /** The current scene we're viewing. */
    protected var _scene :MsoyScene;

    [Embed(source="../../../../../../../rsrc/media/walkable.swf")]
    protected static const WALKTARGET :Class;

    /** The "cursor" used to display that a location is walkable. */
    protected var _walkTarget :DisplayObject = (new WALKTARGET() as DisplayObject);

    /** Are we editing the current scene? */
    protected var _editor :EditorController;

    /** The number of pixels we scroll the room on a keypress. */
    protected static const ROOM_SCROLL_INCREMENT :int = 20;
}
}
