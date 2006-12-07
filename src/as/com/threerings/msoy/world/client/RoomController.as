package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.ui.ContextMenuItem;
import flash.ui.Keyboard;

import mx.managers.ToolTipManager;

import com.threerings.util.MenuUtil;
import com.threerings.util.NetUtil;

import com.threerings.io.TypedArray;

import com.threerings.mx.controls.CommandMenu;

import com.threerings.presents.client.ResultWrapper;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;
import com.threerings.whirled.data.SceneUpdate;

import com.threerings.msoy.client.MemberService;
import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.client.MsoyController;
import com.threerings.msoy.data.MemberInfo;
import com.threerings.msoy.data.MemberObject;

import com.threerings.msoy.web.data.MemberName;

import com.threerings.msoy.world.client.editor.EditorController;

import com.threerings.msoy.world.data.FurniData;
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

    // documentation inherited
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        _mctx = (ctx as MsoyContext);
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
        _roomView.rawChildren.addChild(_walkTarget);

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.addEventListener(MouseEvent.MOUSE_MOVE, mouseMoved);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);
    }

    // documentation inherited
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.removeEventListener(MouseEvent.MOUSE_MOVE, mouseMoved);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.rawChildren.removeChild(_walkTarget);

        _scene = null;
        _roomObj = null;

        // pop down any showing tip (come ON!)
        // TODO: remove this, because I bet it won't be needed soon
        if (ToolTipManager.currentToolTip != null) {
            ToolTipManager.destroyToolTip(ToolTipManager.currentToolTip);
        }

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
        _roomView.addEventListener(MouseEvent.MOUSE_MOVE, mouseMoved);

        // possibly save the edits
        if (edits != null) {
            _roomObj.roomService.updateRoom(_mctx.getClient(), edits,
                new ReportingListener(_mctx));
        }
    }

    override public function handleAction (cmd :String, arg :Object) :Boolean
    {
        if (cmd == EDIT_SCENE) {
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

        case FurniData.ACTION_GAME:
            var actionData :Array = furni.splitActionData();
            var gameId :int = int(actionData[0]);
            postAction(MsoyController.GO_GAME_LOBBY, gameId);
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
        var occInfo :MemberInfo = avatar.getOccupantInfo();
        var us :MemberObject = _mctx.getClientObject();
        var menuItems :Array = [];
        if (occInfo.bodyOid == us.getOid()) {
            // create a menu for clicking on ourselves
            var actions :Array = avatar.getAvatarActions();
            if (actions.length > 0) {
                var localActions :Array = [];
                var worldActions :Array = [];
                var fn :Function = avatar.performAvatarAction;
                for each (var act :String in actions) {
                    localActions.push({ label: act, callback: fn, arg: act });
                    worldActions.push({ label: act,
                        callback: doWorldAvatarAction, arg: act });
                }

                menuItems.push({ label: Msgs.GENERAL.get("l.avAction_world"),
                    children: worldActions });
                menuItems.push({ label: Msgs.GENERAL.get("l.avAction_local"),
                    children: localActions });
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

        var menu :CommandMenu = CommandMenu.createMenu(avatar, menuItems);
        var p :Point = avatar.localToGlobal(new Point());
        menu.show(p.x, p.y);
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
            menuItems.push(createMenuItem(EDIT_SCENE, null, true));
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
        _roomView.removeEventListener(MouseEvent.MOUSE_MOVE, mouseMoved);
        _walkTarget.visible = false;

        _editor = new EditorController(_mctx, this, _roomView, _scene, items);
    }

    protected function mouseLeft (event :MouseEvent) :void
    {
        _walkTarget.visible = false;
        setHoverSprite(null);
    }

    protected function mouseMoved (event :MouseEvent) :void
    {
        var sx :Number = event.stageX;
        var sy :Number = event.stageY;
        var hitter :DisplayObject = getHitObject(sx, sy);

        if (_roomView.isLocationTarget(hitter)) {
            var cloc :ClickLocation = _roomView.pointToLocation(sx, sy);
            if (cloc.click == ClickLocation.FLOOR) {
                var p :Point = _roomView.globalToLocal(new Point(sx, sy));
                _walkTarget.x = p.x;
                _walkTarget.y = p.y;
                hitter = null;
            }
        }

        _walkTarget.visible = (hitter == null);

        setHoverSprite(hitter as MsoySprite); // will pass null if hitter
        // is not a MsoySprite.
    }

    /**
     * Get the top-most object with a non-transparent pixel at the specified
     * location.
     */
    protected function getHitObject (
        stageX :Number, stageY :Number) :DisplayObject
    {
        for (var dex :int = _roomView.numChildren - 1; dex >= 0; dex--) {
            var disp :DisplayObject = _roomView.getChildAt(dex);
            if (disp.hitTestPoint(stageX, stageY, true)) {
                return disp;
            }
        }

        return _roomView;
    }

    protected function setHoverSprite (sprite :MsoySprite) :void
    {
        if (_hoverSprite != sprite) {
            if (_hoverSprite != null) {
                _hoverSprite.setGlow(false);
                _hoverSprite = null;
            }
            if (sprite != null && sprite.hasAction()) {
                sprite.setGlow(true);
                _hoverSprite = sprite;
            }
        }
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        var hitter :DisplayObject = getHitObject(event.stageX, event.stageY);

        if (_roomView.isLocationTarget(hitter)) {
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

        } else if (hitter is MsoySprite) {
            var sprite :MsoySprite = (hitter as MsoySprite);
            if (sprite.hasAction()) {
                sprite.mouseClick(event);
            }
        }
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        var keyDown :Boolean = event.type == KeyboardEvent.KEY_DOWN;
        switch (event.keyCode) {
        case Keyboard.F4:
            _roomView.dimAvatars(keyDown);
            return;

        case Keyboard.F5:
            _roomView.dimFurni(keyDown);
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
            var frob :String = null;
            switch (event.keyCode) {
            case Keyboard.F2:
                frob = MemberObject.CHAT_STYLE;
                break;

            case Keyboard.F3:
                frob = MemberObject.CHAT_POP_STYLE;
                break;
            }

            if (frob != null) {
                _mctx.getClient().getClientObject().postMessage(
                    "alterTEMP", [ frob ]);
            }
        }
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
