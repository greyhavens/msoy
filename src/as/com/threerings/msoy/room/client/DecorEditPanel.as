//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;

import flash.events.Event;

import flash.external.ExternalInterface;

import mx.binding.utils.BindingUtils;

import mx.controls.ComboBox;
import mx.controls.TextInput;
import mx.controls.VSlider;

import mx.containers.Grid;

import mx.events.CloseEvent;

import com.threerings.util.Log;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.ui.FloatingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.data.all.Decor;

import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * Widgets used for editing a backdrop definition.
 */
public class DecorEditPanel extends FloatingPanel
{
    public function DecorEditPanel (ctx :MsoyContext, studioView :RoomStudioView)
    {
        super(ctx, Msgs.WORLD.get("t.edit_backdrop"));
        _studioView = studioView;
        _decor = studioView.getScene().getDecor();
        showCloseButton = true;
    }

    override public function open (
        modal :Boolean = false, parent :DisplayObject = null, center :Boolean = false) :void
    {
        super.open(modal, parent, center);

        // position ourselves
        this.x = stage.stageWidth - width - 10;
        this.y = 10;

        callLater(doHookup);
    }

    protected function doHookup () :void
    {
        // register everything. We are never actually closed
        try {
            ExternalInterface.addCallback("updateMedia", updateMedia);
            ExternalInterface.addCallback("updateParameters", updateParameters);

            ExternalInterface.call("updateDecorInit");

        } catch (e :Error) {
            log.warning("Unable to configure js bridge", e);
        }
    }

    override protected function handleClose (event :CloseEvent) :void
    {
        // don't close, just hide
        visible = false;
    }

    protected function saveChanges (... ignored) :void
    {
        if (_suppressSaves) {
            return;
        }

        _decor.type = int(_types[_roomType.selectedIndex]);
        _decor.width = int(_width.text);
        _decor.height = int(_height.text);
        _decor.depth = _depth.value;
        _decor.horizon = _horizon.value;
        // TODO: offx/offy?
        _decor.hideWalls = _hideWalls.selected;

        updateDecorInViewer(false);
        updateDecorOnPage();
    }

    protected function updateMedia (path :String) :void
    {
        _decor.furniMedia = new StudioMediaDesc(path);

        updateDecorInViewer(true);
    }

    protected function updateParameters (
        width :int, height :int, depth :int, horizon :Number, type :int,
        offsetX :Number, offsetY :Number, hideWalls :Boolean) :void
    {
        _decor.width = width;
        _decor.height = height;
        _decor.depth = depth;
        _decor.type = type;
        _decor.horizon = horizon;
        _decor.offsetX = offsetX;
        _decor.offsetY = offsetY;
        _decor.hideWalls = hideWalls;

        _suppressSaves = true;
        try {
            _width.text = String(width);
            _height.text = String(height);
            _depth.value = depth;
            _roomType.selectedIndex = _types.indexOf(type);
            _horizon.value = horizon;
            // TODO: offx/offy?
            _hideWalls.selected = hideWalls;
        } finally {
            _suppressSaves = false;
        }

        updateDecorInViewer(false);
    }

    protected function updateDecorInViewer (mediaUpdated :Boolean) :void
    {
        var newScene :MsoyScene = _studioView.getScene().clone() as MsoyScene;
        var newModel :MsoySceneModel = newScene.getSceneModel() as MsoySceneModel;
        newModel.decor = _decor;

        _studioView.setScene(newScene);
        _studioView.updateBackground();
    }

    protected function updateDecorOnPage () :void
    {
        try {
            if (!ExternalInterface.available) {
                log.warning("External interface not available, can't save decor.");
                return;
            }

            ExternalInterface.call("updateDecor",
                _decor.width, _decor.height, _decor.depth, _decor.horizon,
                _decor.type, _decor.offsetX, _decor.offsetY, _decor.hideWalls);

        } catch (e :Error) {
            log.warning("Unable to send decor to hosting page.", e);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        var grid :Grid = new Grid();

        _horizon = new VSlider();
        _horizon.liveDragging = true;
        _horizon.maximum = 1;
        _horizon.minimum = 0;

        _depth = new VSlider();
        _depth.liveDragging = true;
        _depth.maximum = 2000;
        _depth.minimum = 0;

        _hideWalls = new CommandCheckBox(Msgs.EDITING.get("l.hide_walls"), saveChanges);

        _roomType = new ComboBox();
        var types :Array = [];
        for (var ii :int = 0; ii < _types.length; ii++) {
            types[ii] = { label: Msgs.EDITING.get("m.scene_type_" + _types[ii]) };
        }
        _roomType.dataProvider = types;
        _roomType.addEventListener(Event.CHANGE, saveChanges);

        _width = new TextInput();
        _height = new TextInput();

        // TODO: offx/offy?

        GridUtil.addRow(grid, Msgs.EDITING.get("l.scene_type"), _roomType, [ 2, 1 ]);
        GridUtil.addRow(grid, _hideWalls, [ 3, 1 ]);
        GridUtil.addRow(grid, Msgs.EDITING.get("l.scene_dimensions"), _width, _height);
        addChild(grid);

        grid = new Grid();
        GridUtil.addRow(grid, Msgs.EDITING.get("l.horizon"), _horizon,
            Msgs.EDITING.get("l.scene_depth"), _depth);
        addChild(grid);

        // now bind everything up so that these widgets change things
        _suppressSaves = true;
        try {
            BindingUtils.bindSetter(saveChanges, _horizon, "value");
            BindingUtils.bindSetter(saveChanges, _depth, "value");
            BindingUtils.bindSetter(saveChanges, _width, "text");
            BindingUtils.bindSetter(saveChanges, _height, "text");
        } finally {
            _suppressSaves = false;
        }
    }

    protected var log :Log = Log.getLog(this);

    protected var _suppressSaves :Boolean;

    protected var _decor :Decor;

    protected var _studioView :RoomStudioView;

    protected var _types :Array = [ 1, 3]; // this starts out with valid roomtypes

    protected var _horizon :VSlider;
    protected var _depth :VSlider;
    protected var _hideWalls :CommandCheckBox;
    protected var _roomType :ComboBox;
    protected var _width :TextInput;
    protected var _height :TextInput;
}
}
