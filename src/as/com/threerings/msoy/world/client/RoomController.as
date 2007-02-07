package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.Event;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.ui.ContextMenuItem;
import flash.ui.Keyboard;
import flash.utils.ByteArray;

import mx.core.IToolTip;
import mx.core.UIComponent;
import mx.managers.ToolTipManager;

import com.threerings.util.MenuUtil;
import com.threerings.util.NetUtil;

import com.threerings.io.TypedArray;

import com.threerings.flex.CommandMenu;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.ezgame.util.EZObjectMarshaller;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
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

import com.threerings.msoy.chat.client.ReportingListener;

public class RoomController extends SceneController
{
    private const log :Log = Log.getLog(RoomController);

    public static const EDIT_SCENE :String = "edit_scene";

    public static const FURNI_CLICKED :String = "FurniClicked";
    public static const AVATAR_CLICKED :String = "AvatarClicked";

    public static const TEMP_CLEAR_SCENE_CACHE :String = "clrScenes";

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
     * Handles a request by an item in our room to trigger an event.
     */
    public function triggerEvent (ident :ItemIdent, event :String, arg :Object) :void
    {
        if (!checkCanRequest(ident, "triggerEvent")) {
            return;
        }

        // send the request off to the server
        var data :ByteArray = (EZObjectMarshaller.encode(arg, false) as ByteArray);
        _roomObj.roomService.triggerEvent(_mctx.getClient(), ident, event, data);
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

        _mctx = (ctx as MsoyContext);

        // initialize the instanceId for all sprites
        MsoySprite.instanceId = _mctx.getClientObject().getOid();
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _roomView = new RoomView(ctx as MsoyContext, this);
        return _roomView;
    }

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomObj = (plobj as RoomObject);

        // get a copy of the scene
        _scene = (_mctx.getSceneDirector().getScene() as MsoyScene);

        _walkTarget.visible = false;
        _roomView.addChild(_walkTarget);

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.addEventListener(MouseEvent.MOUSE_OVER, mouseEntered);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);
    }

    // documentation inherited
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.removeEventListener(MouseEvent.MOUSE_OVER, mouseEntered);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.removeChild(_walkTarget);
        setHoverSprite(null);

        _scene = null;
        _roomObj = null;

        super.didLeavePlace(plobj);
    }

    /**
     * Exit editing mode.
     */
    public function endEditing (edits :TypedArray) :void
    {
        _editor = null;

        // turn editing off
        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.addEventListener(MouseEvent.MOUSE_OVER, mouseEntered);

        // possibly save the edits
        if (edits != null) {
            _roomObj.roomService.updateRoom(_mctx.getClient(), edits,
                new ReportingListener(_mctx));
        }
    }

    override public function handleAction (cmd :String, arg :Object) :Boolean
    {
        if (cmd == EDIT_SCENE) {
            if (_editor != null) {
                return true; // handled: we're editing
            }
            _roomObj.roomService.editRoom(_mctx.getClient(),
                new ResultWrapper(
                    function (cause :String) :void {
                        _mctx.displayFeedback("general", cause);
                    },
                    function (result :Object) :void {
                        startEditing(result as Array);
                    }));

        } else if (cmd == TEMP_CLEAR_SCENE_CACHE) {
            _mctx.TEMPClearSceneCache();

        } else {
            return super.handleAction(cmd, arg);
        }

        return true; // for handled commands
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
        var us :MemberObject = _mctx.getClientObject();
        var menuItems :Array = [];
        if (occInfo.bodyOid == us.getOid()) {
            // create a menu for clicking on ourselves
            var actions :Array = avatar.getAvatarActions();
            if (actions.length > 0) {
                var worldActions :Array = [];
                for each (var act :String in actions) {
                    worldActions.push({ label: act,
                        callback: doWorldAvatarAction, arg: act });
                }

                menuItems.push({ label: Msgs.GENERAL.get("l.avAction"),
                    children: worldActions });
            }

        } else {
            // create a menu for clicking on someone else
            var memId :int = occInfo.getMemberId();
            var isGuest :Boolean = (memId == MemberName.GUEST_ID);
            var isFriend :Boolean = us.friends.containsKey(memId);
            menuItems.push({ label: Msgs.GENERAL.get("b.tell"),
                command: MsoyController.TELL, arg: memId });

            if (!isGuest) {
                menuItems.push(
                    { label: Msgs.GENERAL.get("b.visit_home"),
                      command: MsoyController.GO_MEMBER_HOME,
                      arg: memId },
                    { label: Msgs.GENERAL.get(isFriend ? "b.removeAsFriend"
                                                       : "b.addAsFriend"),
                      command: MsoyController.ALTER_FRIEND,
                      arg: [memId, !isFriend] });
            }
        }

        // pop up the menu where the mouse is
        CommandMenu.createMenu(menuItems).show();
    }

    /**
     * Called by the RoomView prior to a context menu popping up.
     */
    public function populateContextMenu (menuItems :Array) :void
    {
        if (_scene == null) {
            return;
        }

        if (_editor == null && _scene.canEdit(_mctx.getClientObject())) {
            // TODO: I used to put a separator before this menu item,
            // but it caused it to break in linux. STRANGE!
            menuItems.push(createMenuItem(EDIT_SCENE, null, false /*true*/));
        }

        menuItems.push(MenuUtil.createControllerMenuItem(
            "temp: clear scene cache", TEMP_CLEAR_SCENE_CACHE, null, true));
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
        _roomView.removeEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.removeEventListener(MouseEvent.MOUSE_OVER, mouseEntered);
        _roomView.removeEventListener(Event.ENTER_FRAME, checkMouse);
        _walkTarget.visible = false;

        _editor = new EditorController(_mctx, this, _roomView, _scene, items);
    }

    protected function mouseLeft (event :MouseEvent) :void
    {
        _walkTarget.visible = false;
        setHoverSprite(null);
        _roomView.chatOverlay.setClickableGlyphs(false);

        _roomView.removeEventListener(Event.ENTER_FRAME, checkMouse);
    }

    protected function mouseEntered (event :MouseEvent) :void
    {
        _roomView.addEventListener(Event.ENTER_FRAME, checkMouse);
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
            if (cloc.click == ClickLocation.FLOOR) {
                _walkTarget.x = _roomView.mouseX;
                _walkTarget.y = _roomView.mouseY;
                _walkTarget.scaleX = 1 / _roomView.scaleX;
                _walkTarget.scaleY = 1 / _roomView.scaleY;
                showWalkTarget = true;
            }
        }

        _walkTarget.visible = showWalkTarget;

        setHoverSprite(hitter, sx, sy);
    }

    /**
     * Get the top-most sprite with both an action and a non-transparent
     * pixel at the specified location.
     */
    protected function getHitSprite (
        stageX :Number, stageY :Number) :MsoySprite
    {
        // we search from last-drawn to first drawn to get the topmost...
        for (var dex :int = _roomView.numChildren - 1; dex >= 0; dex--) {
            var spr :MsoySprite = (_roomView.getChildAt(dex) as MsoySprite);
            if ((spr != null) && spr.hasAction() &&
                    spr.hitTestPoint(stageX, stageY, true)) {
                return spr;
            }
        }

        return null;
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
                    (_hoverTip as UIComponent).setStyle("color", sprite.getHoverColor());
                }
            }
        }
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        var hitter :MsoySprite = getHitSprite(event.stageX, event.stageY);

        if (hitter != null) {
            hitter.mouseClick(event);

        } else {
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

            if (keyDown) {
                switch (event.keyCode) {
                case Keyboard.F1:
                    handleAction(EDIT_SCENE, null);
                    break;

                case Keyboard.F7:
                    _roomView.chatOverlay.setHistoryEnabled(
                        !_roomView.chatOverlay.isHistoryMode());
                    break;
                }
            }

        } finally {
            event.updateAfterEvent();
        }
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
        return (ctrl.controllerOid == _mctx.getClientObject().getOid());
    }

    override protected function sceneUpdated (update :SceneUpdate) :void
    {
        super.sceneUpdated(update);
        _roomView.processUpdate(update);
    }

    /**
     * Called to enact the avatar action globally: all users will see it.
     */
    protected function doWorldAvatarAction (action :String) :void
    {
        _roomObj.postMessage("avAction",
            [ _mctx.getClient().getClientOid(), action ]);
    }

    /** The life-force of the client. */
    protected var _mctx :MsoyContext;

    /** The room view that we're controlling. */
    protected var _roomView :RoomView;

    protected var _roomObj :RoomObject;

    protected var _hoverSprite :MsoySprite;

    protected var _hoverTip :IToolTip;

    /** The current scene we're viewing. */
    protected var _scene :MsoyScene;

    /** The "cursor" used to display that a location is walkable. */
    protected var _walkTarget :DisplayObject = new TargetCursor();

    /** Are we editing the current scene? */
    protected var _editor :EditorController;

    /** The number of pixels we scroll the room on a keypress. */
    protected static const ROOM_SCROLL_INCREMENT :int = 20;
}
}
