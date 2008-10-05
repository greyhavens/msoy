//
// $Id$

package com.threerings.msoy.room.client {

import flash.display.DisplayObject;

import flash.events.Event;

import flash.external.ExternalInterface;

import mx.binding.utils.BindingUtils;

import mx.controls.Button;
import mx.controls.ComboBox;
import mx.controls.TextInput;
import mx.controls.VSlider;
import mx.containers.Grid;
import mx.containers.VBox;
import mx.core.UIComponent;

import mx.events.CloseEvent;

import com.threerings.util.Log;

import com.threerings.flex.CommandButton;
import com.threerings.flex.CommandCheckBox;
import com.threerings.flex.FlexUtil;
import com.threerings.flex.GridUtil;

import com.threerings.msoy.ui.FlyingPanel;

import com.threerings.msoy.client.Msgs;
import com.threerings.msoy.client.MsoyContext;

import com.threerings.msoy.item.data.all.Decor;

import com.threerings.msoy.room.data.MsoyScene;
import com.threerings.msoy.room.data.MsoySceneModel;

/**
 * Widgets used for editing a backdrop definition.
 */
public class DecorEditPanel extends FlyingPanel
{
    public function DecorEditPanel (ctx :MsoyContext, studioView :RoomStudioView)
    {
        super(ctx, Msgs.STUDIO.get("t.backdrop_props"));
        styleName = "sexyWindow";
        _studioView = studioView;
        _decor = studioView.getScene().getDecor();
        showCloseButton = true;
        open();
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
        var btn :Button = getCloseButton();
        //btn.selected = !btn.selected; // doesn't work
        FlexUtil.setVisible(_grid, !_grid.visible);
    }

    protected function saveChanges (... ignored) :void
    {
        if (_suppressSaves) {
            return;
        }

        _decor.type = int(ROOM_TYPES[_roomType.selectedIndex]);
        _decor.hideWalls = _hideWalls.selected;
        _decor.width = int(_width.text);
        _decor.height = int(_height.text);
        _decor.depth = _depth.value;
        _decor.horizon = _horizon.value;
        _decor.scale = _scale.value;
        _decor.offsetX = Number(_xoff.text);
        _decor.offsetY = Number(_yoff.text);

        updateDecorInViewer();
        updateDecorOnPage();
    }

    protected function updateMedia (path :String) :void
    {
        _decor.furniMedia = new StudioMediaDesc(path);

        updateDecorInViewer();
    }

    protected function updateParameters (
        type :int, hideWalls :Boolean, width :int, height :int, depth :int,
        horizon :Number, scale :Number, offsetX :Number, offsetY :Number) :void
    {
        _decor.type = type;
        _decor.hideWalls = hideWalls;
        _decor.width = width;
        _decor.height = height;
        _decor.depth = depth;
        _decor.horizon = horizon;
        _decor.scale = scale;
        _decor.offsetX = offsetX;
        _decor.offsetY = offsetY;

        _suppressSaves = true;
        try {
            _roomType.selectedIndex = ROOM_TYPES.indexOf(type);
            _hideWalls.selected = hideWalls;
            _width.text = String(width);
            _height.text = String(height);
            _depth.value = depth;
            _depth.tickValues = [ height ];
            _horizon.value = horizon;
            _scale.value = scale;
            _xoff.text = String(offsetX);
            _yoff.text = String(offsetY);
            _checkRoomTypes()
        } finally {
            _suppressSaves = false;
        }

        updateDecorInViewer();
    }

    protected function updateDecorInViewer () :void
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
                _decor.type, _decor.hideWalls, _decor.width, _decor.height, _decor.depth,
                _decor.horizon, _decor.scale, _decor.offsetX, _decor.offsetY);

        } catch (e :Error) {
            log.warning("Unable to send decor to hosting page.", e);
        }
    }

    override protected function createChildren () :void
    {
        super.createChildren();

        _roomType = new ComboBox();
        var types :Array = [];
        for (var ii :int = 0; ii < ROOM_TYPES.length; ii++) {
            types[ii] = { label: Msgs.STUDIO.get("l.room_type_" + ROOM_TYPES[ii]) };
        }
        _roomType.dataProvider = types;
        _roomType.addEventListener(Event.CHANGE, saveChanges);

        _hideWalls = new CommandCheckBox(Msgs.STUDIO.get("l.hide_walls"), saveChanges);

        _scale = new VSlider();
        _scale.liveDragging = true;
        _scale.maximum = 4;
        _scale.minimum = .25;
        _scale.tickValues = [ 1 ];

        _horizon = new VSlider();
        _horizon.liveDragging = true;
        _horizon.maximum = 1;
        _horizon.minimum = 0;
        _horizon.tickValues = [ .5 ];

        _depth = new VSlider();
        _depth.liveDragging = true;
        _depth.maximum = 2000; // TODO: higher?
        _depth.minimum = 0;

        var expertBtn :CommandCheckBox = new CommandCheckBox(Msgs.STUDIO.get("l.dimensions"));

        _width = new TextInput();
        _width.maxChars = 4;
        _height = new TextInput();
        _height.maxChars = 4;
        _xoff = new TextInput();
        _xoff.maxChars = 4;
        _yoff = new TextInput();
        _yoff.maxChars = 4;

        var typeP :Grid = new Grid();
        GridUtil.addRow(typeP, Msgs.STUDIO.get("l.room_type"), _roomType);

        var detailP :Grid = new Grid();
        GridUtil.addRow(detailP, _hideWalls, [2, 1]);
        GridUtil.addRow(detailP, expertBtn, [2, 1]);
        //var dimL :UIComponent = GridUtil.addRow(detailP, Msgs.STUDIO.get("l.dimensions"), [2, 1]);
        GridUtil.addRow(detailP, _width, _height);
        var offsetL :UIComponent = GridUtil.addRow(detailP, Msgs.STUDIO.get("l.offsets"), [2, 1]);
        GridUtil.addRow(detailP, _xoff, _yoff);

        var slideP :Grid = new Grid();
        var scaleP :Grid = new Grid();
        var horzP :Grid = new Grid();
        var depthP :Grid = new Grid();
        GridUtil.addRow(scaleP, Msgs.STUDIO.get("l.scale"));
        GridUtil.addRow(scaleP, _scale);
        GridUtil.addRow(horzP, Msgs.STUDIO.get("l.horizon"));
        GridUtil.addRow(horzP, _horizon);
        GridUtil.addRow(depthP, Msgs.STUDIO.get("l.depth"));
        GridUtil.addRow(depthP, _depth);
        GridUtil.addRow(slideP, scaleP, horzP, depthP, detailP);

        var showExpert :Function = function (toggled :Boolean) :void {
            for each (var comp :UIComponent in [ _width, _height, _xoff, _yoff, offsetL ]) {
                FlexUtil.setVisible(comp, toggled);
            }
        };
        expertBtn.setCallback(showExpert);
        showExpert(false);
        _checkRoomTypes = function ( ... ignored) :void {
            const on :Boolean = ROOM_TYPES[_roomType.selectedIndex] != Decor.FLAT_LAYOUT;
            for each (var comp :UIComponent in [ horzP, depthP, _hideWalls ]){
                FlexUtil.setVisible(comp, on);
            }
        };
        _roomType.addEventListener(Event.CHANGE, _checkRoomTypes);
        _roomType.selectedIndex = ROOM_TYPES.indexOf(Decor.FLAT_LAYOUT); // smallest layout by def.
        _checkRoomTypes();

        GridUtil.addRow(_grid, typeP);
        GridUtil.addRow(_grid, slideP);
        addChild(_grid);

        // now bind everything up so that these widgets change things
        _suppressSaves = true;
        try {
            BindingUtils.bindSetter(saveChanges, _scale, "value");
            BindingUtils.bindSetter(saveChanges, _horizon, "value");
            BindingUtils.bindSetter(saveChanges, _depth, "value");
            BindingUtils.bindSetter(saveChanges, _width, "text");
            BindingUtils.bindSetter(saveChanges, _height, "text");
            BindingUtils.bindSetter(saveChanges, _xoff, "text");
            BindingUtils.bindSetter(saveChanges, _yoff, "text");
        } finally {
            _suppressSaves = false;
        }
    }

    protected var log :Log = Log.getLog(this);

    protected var _suppressSaves :Boolean;

    protected var _decor :Decor;

    protected var _studioView :RoomStudioView;

    /** The room types we support. */
    // TODO: FIXED_IMAGE is not active because it doesn't seem to play well with room zooming
    protected const ROOM_TYPES :Array =
        [ Decor.IMAGE_OVERLAY, /* Decor.FIXED_IMAGE,*/ Decor.FLAT_LAYOUT ];

    protected var _roomType :ComboBox;
    protected var _scale :VSlider;
    protected var _horizon :VSlider;
    protected var _depth :VSlider;
    protected var _hideWalls :CommandCheckBox;
    protected var _width :TextInput;
    protected var _height :TextInput;
    protected var _xoff :TextInput;
    protected var _yoff :TextInput;

    protected var _grid :Grid = new Grid();

    protected var _checkRoomTypes :Function;
}
}
