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

import com.threerings.crowd.client.PlaceView;
import com.threerings.crowd.data.PlaceConfig;
import com.threerings.crowd.data.PlaceObject;
import com.threerings.crowd.util.CrowdContext;

import com.threerings.whirled.client.SceneController;
import com.threerings.whirled.spot.data.Portal;

import com.threerings.msoy.client.MsoyContext;
import com.threerings.msoy.data.MsoyUserObject;

import com.threerings.msoy.world.data.MsoyLocation;
import com.threerings.msoy.world.data.MsoyScene;

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
        _roomView.contextMenu = _menu;
        return _roomView;
    }

    // documentation inherited
    override public function willEnterPlace (plobj :PlaceObject) :void
    {
        super.willEnterPlace(plobj);

        // get a copy of the scene
        _scene = (_mctx.getSceneDirector().getScene() as MsoyScene);
        configureContextMenu();

        _roomView.addEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.stage.addEventListener(MouseEvent.MOUSE_WHEEL, mouseWheeled);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.addEventListener(KeyboardEvent.KEY_UP, keyEvent);
    }

    // documentation inherited
    override public function didLeavePlace (plobj :PlaceObject) :void
    {
        _roomView.removeEventListener(MouseEvent.CLICK, mouseClicked);
        _roomView.stage.removeEventListener(MouseEvent.MOUSE_WHEEL,
            mouseWheeled);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_DOWN, keyEvent);
        _roomView.stage.removeEventListener(KeyboardEvent.KEY_UP, keyEvent);

        _scene = null;
        configureContextMenu();

        super.didLeavePlace(plobj);
    }

    override public function handleAction (cmd :String, arg :Object) :Boolean
    {
        if (cmd == "portalClicked") {
            var portal :Portal = (arg as Portal);
            _mctx.getSpotSceneDirector().traversePortal(portal.portalId);

        } else if (cmd == EDIT_SCENE) {
            _editing = true;
            configureContextMenu();
            // TODO

        } else if ((cmd == SAVE_EDITS) || (cmd == DISCARD_EDITS)) {
            _editing = false;
            configureContextMenu();
            if (cmd == SAVE_EDITS) {
                // TODO
            }

        } else {
            return super.handleAction(cmd, arg);
        }

        return true;
    }

    protected function mouseClicked (event :MouseEvent) :void
    {
        if (_roomView.isLocationTarget(event.target as DisplayObject)) {
            var p :Point = _roomView.globalToLocal(
                new Point(event.stageX, event.stageY));

            // mouse events are propogated upwards to parent components
            var curLoc :MsoyLocation = _roomView.getMyCurrentLocation();

            // calculate where the location is
            var newLoc :MsoyLocation = _roomView.pointToLocation(p.x, p.y);
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
            }

            if (frob != null) {
                _mctx.getClient().getClientObject().postMessage(
                    "alterTEMP", [ frob ]);
            }
        }
    }

    protected function configureContextMenu () :void
    {
        // first remove any custom actions that were already in there
        _menu.customItems.length = 0; // clear

        if (_scene == null) {
            return;
        }

        if (_editing) {
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

    /** The current scene we're viewing. */
    protected var _scene :MsoyScene;

    /** Are we editing the current scene? */
    protected var _editing :Boolean;

    /** The context menu. */
    protected var _menu :ContextMenu;
}
}
