package com.threerings.msoy.world.client {

import flash.display.DisplayObject;
import flash.display.InteractiveObject;
import flash.events.KeyboardEvent;
import flash.events.MouseEvent;
import flash.geom.Point;
import flash.ui.ContextMenu;
import flash.ui.ContextMenuItem;
import flash.ui.Keyboard;

import com.threerings.util.MenuUtil;

import com.threerings.io.TypedArray;

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;
import com.threerings.whirled.data.SceneUpdate;
import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyUserObject;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;
import com.threerings.msoy.world.data.RoomObject;

import com.threerings.msoy.world.chat.client.ReportingListener;

public class RoomController extends SceneController
{
    public static const EDIT_SCENE :String = "edit_scene";
    public static const SAVE_EDITS :String = "save_edits";
    public static const DISCARD_EDITS :String = "discard_edits";

    // documentation inherited
    override public function init (ctx :CrowdContext, config :PlaceConfig) :void
    {
        super.init(ctx, config);

        _mctx = (ctx as MsoyContext);
    }

    // documentation inherited
    override protected function createPlaceView (ctx :CrowdContext) :PlaceView
    {
        _roomView = new RoomView(ctx as MsoyContext);

        _menu = new ContextMenu();
        // disallow all builtins, but allow printing
        _menu.hideBuiltInItems();
        _roomView.contextMenu = _menu;
        return _roomView;
    }

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        _roomObj = (plobj as RoomObject);

        // get a copy of the scene
        _scene = (_mctx.getSceneDirector().getScene() as MsoyScene);
        configureContextMenu();

        _walkTarget.visible = false;
        _roomView.rawChildren.addChild(_walkTarget);

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.addEventListener(MouseEvent.MOUSE_MOVE, mouseMoved);
        _roomView.stage.addEventListener(MouseEvent.MOUSE_WHEEL, mouseWheeled);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);
    }

    // documentation inherited
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.removeEventListener(MouseEvent.MOUSE_MOVE, mouseMoved);
        _roomView.stage.removeEventListener(MouseEvent.MOUSE_WHEEL,
            mouseWheeled);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _roomView.rawChildren.removeChild(_walkTarget);

        _scene = null;
        _roomObj = null;
        configureContextMenu();

        super.didLeavePlace(plobj);
    }

    override public function handleAction (cmd :String, arg :Object) :Boolean
    {
        if (cmd == "portalClicked") {
            var portal :Portal = (arg as Portal);
            _mctx.getSpotSceneDirector().traversePortal(portal.portalId);

        } else if (cmd == EDIT_SCENE) {
            startEditing();

        } else if ((cmd == SAVE_EDITS) || (cmd == DISCARD_EDITS)) {
            endEditing(cmd == SAVE_EDITS);

        } else {
            return super.handleAction(cmd, arg);
        }

        return true;
    }

    protected function configureContextMenu () :void
    {
        // first remove any custom actions that were already in there
        _menu.customItems.length = 0; // clear

        if (_scene == null) {
            return;
        }

        if (_editor != null) {
            addMenuItem(SAVE_EDITS);
            addMenuItem(DISCARD_EDITS);

        } else if (true) { // TODO: if canEditScene...
            addMenuItem(EDIT_SCENE);
        }
    }

    /**
     * Add the specified command to the context menu for the current scene.
     */
    protected function addMenuItem (cmd :String) :void
    {
        _menu.customItems.push(
            MenuUtil.createControllerMenuItem("m." + cmd, cmd));
    }

    /**
     * Begin editing the scene.
     */
    protected function startEditing () :void
    {
        // set up editing
        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.removeEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.removeEventListener(MouseEvent.MOUSE_MOVE, mouseMoved);
        _walkTarget.visible = false;

        _editor = new EditRoomHelper(_mctx, _roomView);
        configureContextMenu();
    }

    /**
     * Exit editing mode.
     */
    protected function endEditing (saveEdits :Boolean) :void
    {
        var edits :TypedArray = _editor.endEditing(saveEdits);
        _editor = null;

        // turn editing off
        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.addEventListener(MouseEvent.MOUSE_OUT, mouseLeft);
        _roomView.addEventListener(MouseEvent.MOUSE_MOVE, mouseMoved);
        configureContextMenu();

        // possibly save the edits
        if (edits != null) {
            _roomObj.roomService.updateRoom(_mctx.getClient(), edits,
                new ReportingListener(_mctx));
        }
    }

    protected function mouseLeft (event :MouseEvent) :void
    {
        _walkTarget.visible = false;
    }

    protected function mouseMoved (event :MouseEvent) :void
    {
        if (_roomView.isLocationTarget(event.target as DisplayObject)) {
            var newLoc :MsoyLocation =
                _roomView.pointToLocation(event.stageX, event.stageY);
            if (newLoc != null) {
                _walkTarget.x = event.localX;
                _walkTarget.y = event.localY;
                _walkTarget.visible = true;
                return;
            }
        }

        _walkTarget.visible = false;
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        if (_roomView.isLocationTarget(event.target as DisplayObject)) {
            var curLoc :MsoyLocation = _roomView.getMyCurrentLocation();
            if (curLoc == null) {
                return; // we've already left, ignore the click
            }

            // calculate where the location is
            var newLoc :MsoyLocation = _roomView.pointToLocation(
                event.stageX, event.stageY);
            if (newLoc != null) {
                // orient the location as appropriate
                newLoc.orient = (curLoc.x > newLoc.x ? 180 : 0);
                _mctx.getSpotSceneDirector().changeLocation(newLoc, null);
            }
        }
    }

    protected function mouseWheeled (event :MouseEvent) :void
    {
        _roomView.scrollViewBy(20 * event.delta); // TODO
    }

    protected function keyEvent (event :KeyboardEvent) :void
    {
        var keyDown :Boolean = event.type == KeyboardEvent.KEY_DOWN;
        switch (event.keyCode) {
        case Keyboard.SHIFT:
            _roomView.dimAvatars(keyDown);
            return;

        case Keyboard.CONTROL:
            _roomView.dimFurni(keyDown);
            return;
        }

        if (keyDown) {
            var frob :String = null;
            switch (event.keyCode) {
            case Keyboard.F1:
                frob = MsoyUserObject.AVATAR;
                break;

            case Keyboard.F2:
                frob = MsoyUserObject.CHAT_STYLE;
                break;

            case Keyboard.F3:
                frob = MsoyUserObject.CHAT_POP_STYLE;
                break;

            case Keyboard.F4:
                frob = MsoyUserObject.AVATAR + "r";
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

        // for now, this should take care of updating things
        _roomView.updateAllFurniAndPortals();
    }

    /**
     * Callback when an item is selected from the context menu.
     */
/*
    protected function menuItemSelected (event :ContextMenuEvent) :void
    {
        var item :ContextMenuItem = (event.target as ContextMenuItem);
        var cmd :String = item.caption
    }
*/

    /** The life-force of the client. */
    protected var _mctx :MsoyContext;

    /** The room view that we're controlling. */
    protected var _roomView :RoomView;

    protected var _roomObj :RoomObject;

    /** The current scene we're viewing. */
    protected var _scene :MsoyScene;

    /** The "cursor" used to display that a location is walkable. */
    protected var _walkTarget :DisplayObject = new TargetCursor();

    /** Are we editing the current scene? */
    protected var _editor :EditRoomHelper;

    /** The context menu. */
    protected var _menu :ContextMenu;
}
}
